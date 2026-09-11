package kali.microservices.infrastructureservice.openstack.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Thin REST client for Gnocchi (OpenStack telemetry/metering). openstack4j has no
 * support for the "metric" service, so this talks to Gnocchi directly over HTTP using
 * the token minted by {@link TelemetryAuthService}.
 */
@Slf4j
@Service
public class GnocchiClient {

    @Value("${openstack.telemetry.gnocchi-url:}")
    private String gnocchiUrl;

    private final TelemetryAuthService authService;
    private final RestTemplate restTemplate;

    public GnocchiClient(TelemetryAuthService authService, RestTemplate restTemplate) {
        this.authService = authService;
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(gnocchiUrl) && authService.isConfigured();
    }

    public List<Map<String, Object>> listResources(String resourceType) {
        return execute(token -> restTemplate.exchange(
                gnocchiUrl + "/resource/" + resourceType,
                HttpMethod.GET,
                entityWithToken(token),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        ).getBody());
    }

    public Map<String, Object> getResource(String resourceType, String resourceId) {
        return execute(token -> restTemplate.exchange(
                gnocchiUrl + "/resource/" + resourceType + "/" + resourceId,
                HttpMethod.GET,
                entityWithToken(token),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody());
    }

    /**
     * Each measure is a [timestamp, granularity, value] triple, per Gnocchi's API.
     * Returns an empty list if the metric has no data yet — that's a normal state, not an error.
     */
    public List<List<Object>> getMeasures(String resourceType, String resourceId, String metricName,
                                           Integer granularity, String start, String stop) {
        return execute(token -> {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(gnocchiUrl + "/resource/" + resourceType + "/" + resourceId
                            + "/metric/" + metricName + "/measures");
            if (granularity != null) builder.queryParam("granularity", granularity);
            if (StringUtils.hasText(start)) builder.queryParam("start", start);
            if (StringUtils.hasText(stop)) builder.queryParam("stop", stop);

            return restTemplate.exchange(
                    builder.build().toUri(),
                    HttpMethod.GET,
                    entityWithToken(token),
                    new ParameterizedTypeReference<List<List<Object>>>() {}
            ).getBody();
        });
    }

    private HttpEntity<Void> entityWithToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", token);
        return new HttpEntity<>(headers);
    }

    private <T> T execute(Function<String, T> call) {
        String token = authService.getToken();
        try {
            return call.apply(token);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("Gnocchi returned 401 (stale token), refreshing and retrying once");
            String freshToken = authService.refreshToken();
            return call.apply(freshToken);
        }
    }
}
