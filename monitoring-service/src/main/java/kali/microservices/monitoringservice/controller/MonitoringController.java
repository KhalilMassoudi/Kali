package kali.microservices.monitoringservice.controller;

import kali.microservices.monitoringservice.entities.AlertEvent;
import kali.microservices.monitoringservice.entities.ServiceMetric;
import kali.microservices.monitoringservice.security.AuthContext;
import kali.microservices.monitoringservice.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final AuthContext authContext;

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

    @GetMapping("/alerts/active")
    public ResponseEntity<List<AlertEvent>> getActiveAlerts(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(monitoringService.getActiveAlerts());
    }

    @GetMapping("/services")
    public ResponseEntity<List<String>> getKnownServices(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(monitoringService.getKnownServiceNames());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Monitoring Service is running!");
    }
}