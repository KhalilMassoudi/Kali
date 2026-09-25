package kali.microservices.billingservice.client;

import kali.microservices.billingservice.security.ServiceTokenProvider;
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
 * Emails go through auth-service's existing admin notify endpoint (same one support-service uses):
 * it resolves the userId to an email and sends with the admin-configured SMTP settings - so there's
 * no second SMTP config in billing-service. Never throws: callers get an empty/false result instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestTemplate restTemplate;
    private final ServiceTokenProvider tokenProvider;

    @Value("${services.auth.url}")
    private String baseUrl;

    private HttpHeaders headers(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        return headers;
    }

    private String serviceAuthHeader() {
        return "Bearer " + tokenProvider.mintServiceAdminToken();
    }

    /** Background use (metering job) - no user session, so this uses the internal service token. */
    public boolean notifyUser(Long userId, String subject, String body) {
        try {
            HttpHeaders headers = headers(serviceAuthHeader());
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of("userId", userId, "subject", subject, "body", body);
            restTemplate.postForEntity(baseUrl + "/api/auth/admin/notify", new HttpEntity<>(payload, headers), Void.class);
            return true;
        } catch (Exception e) {
            log.warn("Failed to notify userId={} via auth-service: {}", userId, e.getMessage());
            return false;
        }
    }

    /** Background use (metering job) - looks a user up in the admin user list, with the internal service token. */
    public Optional<UserSnapshot> findUser(Long userId) {
        return findUser(serviceAuthHeader(), userId);
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
