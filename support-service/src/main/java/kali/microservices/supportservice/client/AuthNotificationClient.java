package kali.microservices.supportservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Fire-and-forget notification shipper — calls auth-service's admin notify endpoint, which
 * resolves the target userId to an email and sends it (a no-op if SMTP isn't configured).
 * Runs async and swallows failures: a slow/down auth-service must never fail a ticket action.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthNotificationClient {

    private static final String NOTIFY_URL = "http://localhost:8081/api/auth/admin/notify";

    private final RestTemplate restTemplate;

    @Async
    public void notifyUser(String authHeader, Long userId, String subject, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of("userId", userId, "subject", subject, "body", body);
            restTemplate.postForEntity(NOTIFY_URL, new HttpEntity<>(payload, headers), Void.class);
        } catch (Exception e) {
            log.debug("Failed to notify userId={} via auth-service: {}", userId, e.getMessage());
        }
    }
}
