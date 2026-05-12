package kali.microservices.monitoringservice.controller;

import kali.microservices.monitoringservice.entities.AlertRule;
import kali.microservices.monitoringservice.entities.ServiceMetric;
import kali.microservices.monitoringservice.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        return ResponseEntity.ok(monitoringService.getSystemOverview());
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<ServiceMetric>> getLatestMetrics() {
        return ResponseEntity.ok(monitoringService.getLatestMetrics());
    }

    @GetMapping("/metrics/{serviceName}")
    public ResponseEntity<List<ServiceMetric>> getServiceMetrics(@PathVariable String serviceName) {
        return ResponseEntity.ok(monitoringService.getMetricsForService(serviceName));
    }

    @PostMapping("/metrics")
    public ResponseEntity<ServiceMetric> recordMetric(@RequestBody Map<String, Object> body) {
        String serviceName = body.get("serviceName").toString();
        ServiceMetric.ServiceStatus status = ServiceMetric.ServiceStatus.valueOf(body.get("status").toString());
        Double responseTime = body.containsKey("responseTimeMs") ? Double.valueOf(body.get("responseTimeMs").toString()) : null;
        Double cpu = body.containsKey("cpuUsagePercent") ? Double.valueOf(body.get("cpuUsagePercent").toString()) : null;
        Double memory = body.containsKey("memoryUsagePercent") ? Double.valueOf(body.get("memoryUsagePercent").toString()) : null;
        Long requests = body.containsKey("requestCount") ? Long.valueOf(body.get("requestCount").toString()) : null;
        Long errors = body.containsKey("errorCount") ? Long.valueOf(body.get("errorCount").toString()) : null;

        return ResponseEntity.ok(monitoringService.recordMetric(serviceName, status, responseTime, cpu, memory, requests, errors));
    }

    @PostMapping("/alerts/rules")
    public ResponseEntity<AlertRule> createAlertRule(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String serviceName = body.containsKey("serviceName") ? body.get("serviceName").toString() : null;
        AlertRule.AlertType alertType = AlertRule.AlertType.valueOf(body.get("alertType").toString());
        Double threshold = Double.valueOf(body.get("threshold").toString());

        return ResponseEntity.ok(monitoringService.createAlertRule(userId, serviceName, alertType, threshold));
    }

    @GetMapping("/alerts/rules/user/{userId}")
    public ResponseEntity<List<AlertRule>> getAlertRules(@PathVariable Long userId) {
        return ResponseEntity.ok(monitoringService.getAlertRulesByUser(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Monitoring Service is running!");
    }
}