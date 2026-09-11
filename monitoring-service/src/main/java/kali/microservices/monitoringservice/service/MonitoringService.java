package kali.microservices.monitoringservice.service;

import jakarta.annotation.PostConstruct;
import kali.microservices.monitoringservice.entities.AlertEvent;
import kali.microservices.monitoringservice.entities.AlertRule;
import kali.microservices.monitoringservice.entities.ServiceMetric;
import kali.microservices.monitoringservice.repository.AlertEventRepository;
import kali.microservices.monitoringservice.repository.AlertRuleRepository;
import kali.microservices.monitoringservice.repository.ServiceMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    /** userId sentinel for rules seeded at startup rather than created by a specific admin. */
    private static final long SYSTEM_SEEDED_USER_ID = 0L;

    private final ServiceMetricRepository metricRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    /** Ensures a "service down" alert always exists out of the box — the highest-signal, zero-noise rule. */
    @PostConstruct
    public void seedDefaultAlertRules() {
        boolean hasServiceDownRule = alertRuleRepository.findAll().stream()
                .anyMatch(r -> r.getAlertType() == AlertRule.AlertType.SERVICE_DOWN && r.getServiceName() == null);
        if (!hasServiceDownRule) {
            AlertRule rule = new AlertRule();
            rule.setUserId(SYSTEM_SEEDED_USER_ID);
            rule.setServiceName(null);
            rule.setAlertType(AlertRule.AlertType.SERVICE_DOWN);
            rule.setThreshold(0.0);
            rule.setEnabled(true);
            alertRuleRepository.save(rule);
            log.info("Seeded default alert rule: service-down for all services");
        }
    }

    /**
     * Polls every service registered in Eureka for real health/latency (and, best-effort,
     * CPU/memory/request counts via actuator's metrics endpoint) every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000)
    public void collectMetrics() {
        for (String serviceId : discoveryClient.getServices()) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances.isEmpty()) continue;

            ServiceMetric metric = checkHealth(serviceId, instances.get(0));
            metricRepository.save(metric);
            evaluateAlerts(metric);
        }
    }

    private ServiceMetric checkHealth(String serviceId, ServiceInstance instance) {
        String baseUrl = instance.getUri().toString();
        ServiceMetric metric = new ServiceMetric();
        metric.setServiceName(serviceId);
        metric.setTimestamp(LocalDateTime.now());

        long start = System.currentTimeMillis();
        boolean up;
        try {
            // /health/liveness (not the plain /health aggregate) — a service's own custom
            // indicators (e.g. infrastructure-service's OpenStack reachability check) can
            // legitimately report DOWN without the service itself being unreachable/crashed,
            // and we only want to alert on the latter here.
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/actuator/health/liveness", Map.class);
            long elapsed = System.currentTimeMillis() - start;
            metric.setResponseTimeMs((double) elapsed);
            String status = response.getBody() != null ? String.valueOf(response.getBody().get("status")) : null;
            up = "UP".equalsIgnoreCase(status) || (status == null && response.getStatusCode().is2xxSuccessful());
            metric.setStatus(up ? ServiceMetric.ServiceStatus.UP : ServiceMetric.ServiceStatus.DOWN);
        } catch (Exception e) {
            metric.setResponseTimeMs((double) (System.currentTimeMillis() - start));
            metric.setStatus(ServiceMetric.ServiceStatus.DOWN);
            up = false;
            log.debug("Health check failed for {}: {}", serviceId, e.getMessage());
        }

        if (up) {
            fetchMetricValue(baseUrl, "process.cpu.usage", "VALUE", null)
                    .ifPresent(v -> metric.setCpuUsagePercent(v * 100));
            Optional<Double> memUsed = fetchMetricValue(baseUrl, "jvm.memory.used", "VALUE", null);
            Optional<Double> memMax = fetchMetricValue(baseUrl, "jvm.memory.max", "VALUE", null);
            if (memUsed.isPresent() && memMax.isPresent() && memMax.get() > 0) {
                metric.setMemoryUsagePercent((memUsed.get() / memMax.get()) * 100);
            }
            fetchMetricValue(baseUrl, "http.server.requests", "COUNT", null)
                    .ifPresent(v -> metric.setRequestCount(v.longValue()));
            fetchMetricValue(baseUrl, "http.server.requests", "COUNT", "tag=outcome:SERVER_ERROR")
                    .ifPresent(v -> metric.setErrorCount(v.longValue()));
        }

        return metric;
    }

    /** Reads one statistic from an actuator /actuator/metrics/{name} response. Never throws. */
    @SuppressWarnings("unchecked")
    private Optional<Double> fetchMetricValue(String baseUrl, String metricName, String statistic, String tagQuery) {
        try {
            String url = baseUrl + "/actuator/metrics/" + metricName + (tagQuery != null ? "?" + tagQuery : "");
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() == null) return Optional.empty();
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) response.getBody().get("measurements");
            if (measurements == null) return Optional.empty();
            for (Map<String, Object> m : measurements) {
                if (statistic.equals(m.get("statistic")) && m.get("value") instanceof Number number) {
                    return Optional.of(number.doubleValue());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void evaluateAlerts(ServiceMetric metric) {
        for (AlertRule rule : alertRuleRepository.findByEnabled(true)) {
            if (rule.getServiceName() != null && !rule.getServiceName().equalsIgnoreCase(metric.getServiceName())) {
                continue;
            }
            String breachMessage = breachMessage(rule, metric);
            if (breachMessage == null) continue;

            if (alertEventRepository.existsByRuleIdAndServiceNameAndResolvedFalse(rule.getId(), metric.getServiceName())) {
                continue; // already an open alert for this rule+service — don't spam a new one every poll
            }

            AlertEvent event = new AlertEvent();
            event.setRuleId(rule.getId());
            event.setServiceName(metric.getServiceName());
            event.setAlertType(rule.getAlertType());
            event.setMessage(breachMessage);
            event.setSeverity(rule.getAlertType() == AlertRule.AlertType.SERVICE_DOWN
                    ? AlertEvent.Severity.CRITICAL : AlertEvent.Severity.WARNING);
            alertEventRepository.save(event);
            log.warn("Alert fired: {}", breachMessage);
        }

        // Auto-resolve open alerts for this service once its metrics recover.
        if (metric.getStatus() == ServiceMetric.ServiceStatus.UP) {
            for (AlertEvent open : alertEventRepository.findByResolvedFalseOrderByFiredAtDesc()) {
                if (!open.getServiceName().equalsIgnoreCase(metric.getServiceName())) continue;
                if (open.getAlertType() == AlertRule.AlertType.SERVICE_DOWN) {
                    open.setResolved(true);
                    open.setResolvedAt(LocalDateTime.now());
                    alertEventRepository.save(open);
                }
            }
        }
    }

    private String breachMessage(AlertRule rule, ServiceMetric metric) {
        return switch (rule.getAlertType()) {
            case SERVICE_DOWN -> metric.getStatus() == ServiceMetric.ServiceStatus.DOWN
                    ? metric.getServiceName() + " est injoignable" : null;
            case CPU_HIGH -> metric.getCpuUsagePercent() != null && metric.getCpuUsagePercent() > rule.getThreshold()
                    ? metric.getServiceName() + " CPU à " + Math.round(metric.getCpuUsagePercent()) + "% (seuil " + rule.getThreshold().intValue() + "%)"
                    : null;
            case MEMORY_HIGH -> metric.getMemoryUsagePercent() != null && metric.getMemoryUsagePercent() > rule.getThreshold()
                    ? metric.getServiceName() + " mémoire à " + Math.round(metric.getMemoryUsagePercent()) + "% (seuil " + rule.getThreshold().intValue() + "%)"
                    : null;
            case RESPONSE_TIME_HIGH -> metric.getResponseTimeMs() != null && metric.getResponseTimeMs() > rule.getThreshold()
                    ? metric.getServiceName() + " temps de réponse à " + Math.round(metric.getResponseTimeMs()) + "ms (seuil " + rule.getThreshold().intValue() + "ms)"
                    : null;
            case ERROR_RATE_HIGH -> {
                if (metric.getRequestCount() == null || metric.getRequestCount() == 0 || metric.getErrorCount() == null) {
                    yield null;
                }
                double errorRate = (metric.getErrorCount() * 100.0) / metric.getRequestCount();
                yield errorRate > rule.getThreshold()
                        ? metric.getServiceName() + " taux d'erreur à " + Math.round(errorRate) + "% (seuil " + rule.getThreshold().intValue() + "%)"
                        : null;
            }
        };
    }

    public ServiceMetric recordMetric(String serviceName, ServiceMetric.ServiceStatus status,
                                       Double responseTime, Double cpu, Double memory,
                                       Long requests, Long errors) {
        ServiceMetric metric = new ServiceMetric();
        metric.setServiceName(serviceName);
        metric.setStatus(status);
        metric.setResponseTimeMs(responseTime);
        metric.setCpuUsagePercent(cpu);
        metric.setMemoryUsagePercent(memory);
        metric.setRequestCount(requests);
        metric.setErrorCount(errors);
        metric.setTimestamp(LocalDateTime.now());

        return metricRepository.save(metric);
    }

    public List<ServiceMetric> getLatestMetrics() {
        return metricRepository.findLatestMetricPerService();
    }

    public List<ServiceMetric> getMetricsForService(String serviceName) {
        return metricRepository.findByServiceNameOrderByTimestampDesc(serviceName);
    }

    public List<ServiceMetric> getMetricsForServiceInRange(String serviceName,
                                                             LocalDateTime from,
                                                             LocalDateTime to) {
        return metricRepository.findByServiceNameAndTimestampBetween(serviceName, from, to);
    }

    public AlertRule createAlertRule(Long userId, String serviceName,
                                      AlertRule.AlertType alertType, Double threshold) {
        AlertRule rule = new AlertRule();
        rule.setUserId(userId);
        rule.setServiceName(serviceName);
        rule.setAlertType(alertType);
        rule.setThreshold(threshold);
        rule.setEnabled(true);

        return alertRuleRepository.save(rule);
    }

    public List<AlertRule> getAlertRulesByUser(Long userId) {
        return alertRuleRepository.findByUserIdAndEnabled(userId, true);
    }

    public List<AlertRule> getAllAlertRules() {
        return alertRuleRepository.findAll();
    }

    public AlertRule setAlertRuleEnabled(Long ruleId, boolean enabled) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Règle d'alerte non trouvée: " + ruleId));
        rule.setEnabled(enabled);
        return alertRuleRepository.save(rule);
    }

    public void deleteAlertRule(Long ruleId) {
        alertRuleRepository.deleteById(ruleId);
    }

    public List<String> getKnownServiceNames() {
        return metricRepository.findDistinctServiceNames();
    }

    public List<AlertEvent> getActiveAlerts() {
        return alertEventRepository.findByResolvedFalseOrderByFiredAtDesc();
    }

    public Map<String, Object> getSystemOverview() {
        List<ServiceMetric> latestMetrics = getLatestMetrics();
        long servicesUp = latestMetrics.stream()
                .filter(m -> m.getStatus() == ServiceMetric.ServiceStatus.UP)
                .count();
        long servicesDown = latestMetrics.stream()
                .filter(m -> m.getStatus() == ServiceMetric.ServiceStatus.DOWN)
                .count();

        return Map.of(
                "totalServices", latestMetrics.size(),
                "servicesUp", servicesUp,
                "servicesDown", servicesDown,
                "metrics", latestMetrics
        );
    }
}
