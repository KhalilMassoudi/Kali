package kali.microservices.monitoringservice.service;

import kali.microservices.monitoringservice.entities.AlertRule;
import kali.microservices.monitoringservice.entities.ServiceMetric;
import kali.microservices.monitoringservice.repository.AlertRuleRepository;
import kali.microservices.monitoringservice.repository.ServiceMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final ServiceMetricRepository metricRepository;
    private final AlertRuleRepository alertRuleRepository;

    // Liste des microservices à monitorer
    private static final List<String> SERVICES = List.of(
            "auth-service", "chat-service", "infrastructure-service",
            "billing-service", "support-service", "Gateway-service"
    );

    /**
     * Collecte des métriques simulées toutes les 30 secondes.
     * En production, on appellerait les endpoints /actuator/metrics de chaque service.
     */
    @Scheduled(fixedDelay = 30000)
    public void collectMetrics() {
        for (String serviceName : SERVICES) {
            ServiceMetric metric = generateMockMetric(serviceName);
            metricRepository.save(metric);
        }
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

    // Génère des métriques simulées pour la démo
    private ServiceMetric generateMockMetric(String serviceName) {
        ServiceMetric metric = new ServiceMetric();
        metric.setServiceName(serviceName);
        metric.setStatus(ServiceMetric.ServiceStatus.UP);
        metric.setResponseTimeMs(50 + Math.random() * 200);
        metric.setCpuUsagePercent(10 + Math.random() * 30);
        metric.setMemoryUsagePercent(30 + Math.random() * 40);
        metric.setRequestCount((long)(Math.random() * 1000));
        metric.setErrorCount((long)(Math.random() * 10));
        metric.setTimestamp(LocalDateTime.now());
        return metric;
    }
}