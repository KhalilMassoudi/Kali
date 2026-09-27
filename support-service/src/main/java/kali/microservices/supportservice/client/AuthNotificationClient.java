package kali.microservices.supportservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Fire-and-forget notification shipper — calls auth-service's internal notify endpoint (shared
 * INTERNAL_API_SECRET), which resolves the target userId to an email and sends it. Not the admin
 * endpoint with the caller's token: ticket agents aren't necessarily ADMIN, so that was rejected.
 * Runs async and swallows failures: a slow/down auth-service must never fail a ticket action.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthNotificationClient {

    private final RestTemplate restTemplate;

    @Value("${services.auth.url:http://localhost:8081}")
    private String authUrl;

    @Value("${internal.api.secret:}")
    private String internalSecret;

    @Async
    public void notifyUser(Long userId, String subject, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of("userId", userId, "subject", subject, "body", body);
            restTemplate.postForEntity(authUrl + "/internal/notify", new HttpEntity<>(payload, headers), Void.class);
        } catch (Exception e) {
            log.warn("Failed to notify userId={} via auth-service: {}", userId, e.getMessage());
        }
    }
}
