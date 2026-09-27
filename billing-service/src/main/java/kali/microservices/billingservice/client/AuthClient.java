package kali.microservices.billingservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls into auth-service, which owns user identities and the platform's SMTP config/sending.
 * Background calls (metering job, no user session) use auth-service's /internal endpoints with the
 * shared INTERNAL_API_SECRET - auth-service rejects our minted service JWT on its admin endpoints,
 * since its subject isn't a real user. Emails are resolved and sent by auth-service with the
 * admin-configured SMTP settings, so there's no second SMTP config here. Never throws: callers get
 * an empty/false result instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestTemplate restTemplate;

    @Value("${services.auth.url}")
    private String baseUrl;

    @Value("${internal.api.secret:}")
    private String internalSecret;

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        return headers;
    }

    private HttpHeaders headers(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        return headers;
    }

    /** Background use (metering job) - no user session, so this goes through the internal endpoint. */
    public boolean notifyUser(Long userId, String subject, String body) {
        try {
            HttpHeaders headers = internalHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of("userId", userId, "subject", subject, "body", body);
            restTemplate.postForEntity(baseUrl + "/internal/notify", new HttpEntity<>(payload, headers), Void.class);
            return true;
        } catch (Exception e) {
            log.warn("Failed to notify userId={} via auth-service: {}", userId, e.getMessage());
            return false;
        }
    }

    /** Background use (metering job) - looks a user up through the internal endpoint. */
    public Optional<UserSnapshot> findUser(Long userId) {
        try {
            var response = restTemplate.exchange(baseUrl + "/internal/users/" + userId, HttpMethod.GET,
                    new HttpEntity<>(internalHeaders()), UserSnapshot.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to look up userId={} in auth-service: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /** Looks a user up in the admin user list, on behalf of the given (admin) caller. */
    public Optional<UserSnapshot> findUser(String authHeader, Long userId) {
        try {
            var response = restTemplate.exchange(baseUrl + "/api/auth/admin/users", HttpMethod.GET,
                    new HttpEntity<>(headers(authHeader)), UserSnapshot[].class);
            UserSnapshot[] users = response.getBody();
            return users == null ? Optional.empty()
                    : List.of(users).stream().filter(u -> userId.equals(u.id())).findFirst();
        } catch (Exception e) {
            log.warn("Failed to look up userId={} in auth-service: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /** The caller's own profile - for a client acting on their own data. */
    public Optional<UserSnapshot> currentUser(String authHeader) {
        try {
            var response = restTemplate.exchange(baseUrl + "/api/auth/me", HttpMethod.GET,
                    new HttpEntity<>(headers(authHeader)), UserSnapshot.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to fetch current user from auth-service: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
