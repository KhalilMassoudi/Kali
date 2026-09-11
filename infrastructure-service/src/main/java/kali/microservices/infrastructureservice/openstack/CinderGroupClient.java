package kali.microservices.infrastructureservice.openstack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Cinder's Generic Volume Groups extension (groups / group_snapshots) has no openstack4j
 * client support (confirmed absent from BlockStorageService in the pinned 3.10 version) — this
 * talks to Cinder directly over HTTP, mirroring GnocchiClient's shape but riding the SAME
 * safozi-app project token via {@link OpenStackAuthService#getRawToken()} rather than a
 * separate service account.
 *
 * <p>Many OpenStack deployments don't enable this Cinder extension regardless of client
 * support — callers should treat any failure here as "not supported by this deployment," not
 * a hard error (see the feature-detect pattern on the frontend).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CinderGroupClient {

    private final OpenStackAuthService authService;
    private final RestTemplate restTemplate;

    public List<Map<String, Object>> listGroups() {
        return execute(token -> restTemplate.exchange(
                base() + "/groups/detail",
                HttpMethod.GET,
                entityWithToken(token, null),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody()).get("groups") instanceof List<?> l ? castList(l) : List.of();
    }

    public Map<String, Object> createGroup(String name, String description, String groupTypeId, List<String> volumeTypeIds) {
        Map<String, Object> body = Map.of("group", Map.of(
                "name", name,
                "description", description == null ? "" : description,
                "group_type", groupTypeId,
                "volume_types", volumeTypeIds
        ));
        return execute(token -> restTemplate.exchange(
                base() + "/groups",
                HttpMethod.POST,
                entityWithToken(token, body),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody());
    }

    public void deleteGroup(String id) {
        execute(token -> restTemplate.exchange(
                base() + "/groups/" + id + "/action",
                HttpMethod.POST,
                entityWithToken(token, Map.of("delete", Map.of("delete-volumes", false))),
                Void.class
        ));
    }

    public List<Map<String, Object>> listGroupSnapshots() {
        return execute(token -> restTemplate.exchange(
                base() + "/group_snapshots/detail",
                HttpMethod.GET,
                entityWithToken(token, null),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody()).get("group_snapshots") instanceof List<?> l ? castList(l) : List.of();
    }

    public Map<String, Object> createGroupSnapshot(String groupId, String name, String description) {
        Map<String, Object> body = Map.of("group_snapshot", Map.of(
                "group_id", groupId,
                "name", name,
                "description", description == null ? "" : description
        ));
        return execute(token -> restTemplate.exchange(
                base() + "/group_snapshots",
                HttpMethod.POST,
                entityWithToken(token, body),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody());
    }

    public void deleteGroupSnapshot(String id) {
        execute(token -> restTemplate.exchange(
                base() + "/group_snapshots/" + id,
                HttpMethod.DELETE,
                entityWithToken(token, null),
                Void.class
        ));
    }

    public List<Map<String, Object>> listGroupTypes() {
        return execute(token -> restTemplate.exchange(
                base() + "/group_types",
                HttpMethod.GET,
                entityWithToken(token, null),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody()).get("group_types") instanceof List<?> l ? castList(l) : List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(List<?> l) {
        return (List<Map<String, Object>>) l;
    }

    private String base() {
        return authService.getCinderEndpoint();
    }

    private <T> HttpEntity<T> entityWithToken(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", token);
        return new HttpEntity<>(body, headers);
    }

    private <T> T execute(Function<String, T> call) {
        String token = authService.getRawToken();
        try {
            return call.apply(token);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("Cinder returned 401 (stale token), refreshing and retrying once");
            authService.authenticate();
            return call.apply(authService.getRawToken());
        }
    }
}
