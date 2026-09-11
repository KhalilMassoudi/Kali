package kali.microservices.gatewayservice.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Fire-and-forget shipper for API access log entries, sent straight to monitoring-service
 * (bypassing the Gateway's own proxy route, so this never generates a log entry for itself).
 * Runs on Spring's async executor and swallows failures — the Gateway must never slow down
 * or fail a proxied request because monitoring-service is unreachable.
 */
@Slf4j
@Service
public class ApiLogClient {

    private static final String INGEST_URL = "http://localhost:8086/api/monitoring/logs/ingest";

    private final RestTemplate restTemplate;

    public ApiLogClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1500);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async
    public void log(String method, String path, String targetService, String userEmail, String userRole,
                     int statusCode, long durationMs) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("method", method);
            body.put("path", path);
            body.put("targetService", targetService);
            body.put("userEmail", userEmail);
            body.put("userRole", userRole);
            body.put("statusCode", statusCode);
            body.put("durationMs", durationMs);
            restTemplate.postForEntity(INGEST_URL, body, Void.class);
        } catch (Exception e) {
            log.debug("Failed to ship API log entry for {} {}: {}", method, path, e.getMessage());
        }
    }
}
