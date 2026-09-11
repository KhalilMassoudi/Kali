package kali.microservices.infrastructureservice.controller;

import kali.microservices.infrastructureservice.dto.MetricsSummary;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.security.AuthenticatedUser;
import kali.microservices.infrastructureservice.service.MetricsAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsAggregationService metricsAggregationService;
    private final AuthContext authContext;

    @GetMapping("/metrics/summary")
    public ResponseEntity<MetricsSummary> getMySummary(@RequestHeader("Authorization") String authHeader) {
        AuthenticatedUser user = authContext.resolve(authHeader);
        return ResponseEntity.ok(metricsAggregationService.getSummaryForUser(user.userId()));
    }

    @GetMapping("/admin/metrics/summary")
    public ResponseEntity<MetricsSummary> getFleetSummary(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(metricsAggregationService.getFleetSummary());
    }
}
