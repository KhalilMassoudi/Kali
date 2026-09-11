package kali.microservices.infrastructureservice.openstack.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Authenticates a dedicated OpenStack service account against Keystone to obtain a
 * project-scoped token for Gnocchi telemetry calls. Kept separate from
 * {@link kali.microservices.infrastructureservice.openstack.OpenStackAuthService}
 * because the telemetry service account is scoped to its own project (e.g. "service"),
 * not the per-tenant project used for VM provisioning.
 */
@Slf4j
@Service
public class TelemetryAuthService {

    @Value("${openstack.telemetry.keystone-url:}")
    private String keystoneUrl;

    @Value("${openstack.telemetry.username:}")
    private String username;

    @Value("${openstack.telemetry.password:}")
    private String password;

    @Value("${openstack.telemetry.project-name:service}")
    private String projectName;

    @Value("${openstack.telemetry.user-domain:Default}")
    private String userDomain;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private Instant cachedTokenExpiry;

    public TelemetryAuthService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(keystoneUrl) && StringUtils.hasText(username) && StringUtils.hasText(password);
    }

    public synchronized String getToken() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Telemetry Keystone auth is not configured (OS_KEYSTONE_URL / OS_USERNAME / OS_PASSWORD missing)");
        }
        if (cachedToken == null || cachedTokenExpiry == null || Instant.now().isAfter(cachedTokenExpiry.minusSeconds(60))) {
            authenticate();
        }
        return cachedToken;
    }

    public synchronized String refreshToken() {
        authenticate();
        return cachedToken;
    }

    private void authenticate() {
        log.info("Authenticating telemetry service account '{}' with Keystone at {}", username, keystoneUrl);

        Map<String, Object> body = Map.of(
                "auth", Map.of(
                        "identity", Map.of(
                                "methods", List.of("password"),
                                "password", Map.of(
                                        "user", Map.of(
                                                "name", username,
                                                "domain", Map.of("name", userDomain),
                                                "password", password
                                        )
                                )
                        ),
                        "scope", Map.of(
                                "project", Map.of(
                                        "name", projectName,
                                        "domain", Map.of("name", userDomain)
                                )
                        )
                )
        );

        // Serialize to bytes ourselves and set Content-Length explicitly: RestTemplate's
        // default request factories stream a Map body without a Content-Length header
        // (chunked transfer encoding), and the Apache/mod_wsgi front-end for this
        // deployment's Keystone rejects that outright with "411 Length Required".
        byte[] bodyBytes;
        try {
            bodyBytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Keystone auth request body", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(bodyBytes.length);

        ResponseEntity<String> response = restTemplate.exchange(
                keystoneUrl + "/auth/tokens", HttpMethod.POST, new HttpEntity<>(bodyBytes, headers), String.class);

        String token = response.getHeaders().getFirst("X-Subject-Token");
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("Keystone response did not include an X-Subject-Token header");
        }

        cachedToken = token;
        cachedTokenExpiry = parseExpiry(response.getBody());
        log.info("Telemetry Keystone token acquired, expires at {}", cachedTokenExpiry);
    }

    private Instant parseExpiry(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String expiresAt = root.path("token").path("expires_at").asText();
            return OffsetDateTime.parse(expiresAt).toInstant();
        } catch (Exception e) {
            log.warn("Could not parse Keystone token expiry, defaulting to a 55 minute lifetime: {}", e.getMessage());
            return Instant.now().plus(55, ChronoUnit.MINUTES);
        }
    }
}
