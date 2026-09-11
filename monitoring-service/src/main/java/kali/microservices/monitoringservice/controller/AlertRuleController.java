package kali.microservices.monitoringservice.controller;

import jakarta.validation.Valid;
import kali.microservices.monitoringservice.dto.CreateAlertRuleRequest;
import kali.microservices.monitoringservice.dto.SetEnabledRequest;
import kali.microservices.monitoringservice.entities.AlertRule;
import kali.microservices.monitoringservice.security.AuthContext;
import kali.microservices.monitoringservice.security.AuthenticatedUser;
import kali.microservices.monitoringservice.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only management of alert rules — thresholds that {@code MonitoringService} evaluates every poll. */
@RestController
@RequestMapping("/api/monitoring/alerts/rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final MonitoringService monitoringService;
    private final AuthContext authContext;

    @GetMapping
    public ResponseEntity<List<AlertRule>> list(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(monitoringService.getAllAlertRules());
    }

    @PostMapping
    public ResponseEntity<AlertRule> create(@RequestHeader("Authorization") String authHeader,
                                             @Valid @RequestBody CreateAlertRuleRequest request) {
        AuthenticatedUser admin = authContext.requireAdmin(authHeader);
        AlertRule rule = monitoringService.createAlertRule(
                admin.userId(), request.getServiceName(), request.getAlertType(), request.getThreshold());
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<AlertRule> setEnabled(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody SetEnabledRequest request) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(monitoringService.setAlertRuleEnabled(id, request.getEnabled()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        authContext.requireAdmin(authHeader);
        monitoringService.deleteAlertRule(id);
        return ResponseEntity.noContent().build();
    }
}
