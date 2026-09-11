package kali.microservices.infrastructureservice.controller;

import kali.microservices.infrastructureservice.openstack.telemetry.GnocchiClient;
import kali.microservices.infrastructureservice.security.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Backend proxy for OpenStack Gnocchi telemetry. The browser never sees the telemetry
 * service account's credentials or the Keystone token — everything is fetched here and
 * forwarded as plain JSON.
 *
 * <p>Admin-only, verified independently via {@link AuthContext} (not the Gateway's
 * forwarded headers) so this also holds if infrastructure-service is ever reached directly.
 */
@Slf4j
@RestController
@RequestMapping("/api/infrastructure/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final GnocchiClient gnocchiClient;
    private final AuthContext authContext;

    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> listResources(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "generic") String type) {
        authContext.requireAdmin(authHeader);
        if (!gnocchiClient.isConfigured()) {
            return ResponseEntity.ok(Map.of("configured", false, "resources", List.of()));
        }
        try {
            List<Map<String, Object>> resources = gnocchiClient.listResources(type);
            return ResponseEntity.ok(Map.of("configured", true, "resources", resources));
        } catch (Exception e) {
            log.warn("Failed to list Gnocchi resources: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("configured", true, "resources", List.of(), "error", "Gnocchi injoignable"));
        }
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<Map<String, Object>> getResource(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id,
            @RequestParam(defaultValue = "generic") String type) {
        authContext.requireAdmin(authHeader);
        if (!gnocchiClient.isConfigured()) {
            return ResponseEntity.ok(Map.of("configured", false));
        }
        try {
            Map<String, Object> resource = gnocchiClient.getResource(type, id);
            return ResponseEntity.ok(resource != null ? resource : Map.of());
        } catch (Exception e) {
            log.warn("Failed to fetch Gnocchi resource {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Gnocchi injoignable"));
        }
    }

    @GetMapping("/resources/{id}/metrics/{metricName}/measures")
    public ResponseEntity<List<List<Object>>> getMeasures(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id,
            @PathVariable String metricName,
            @RequestParam(defaultValue = "generic") String type,
            @RequestParam(required = false) Integer granularity,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String stop) {
        authContext.requireAdmin(authHeader);
        if (!gnocchiClient.isConfigured()) {
            return ResponseEntity.ok(List.of());
        }
        try {
            List<List<Object>> measures = gnocchiClient.getMeasures(type, id, metricName, granularity, start, stop);
            return ResponseEntity.ok(measures != null ? measures : List.of());
        } catch (Exception e) {
            log.warn("Failed to fetch measures for resource {} metric {}: {}", id, metricName, e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }
}
