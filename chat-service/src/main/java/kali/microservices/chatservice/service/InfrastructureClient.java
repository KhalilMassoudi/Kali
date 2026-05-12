package kali.microservices.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfrastructureClient {

    @Value("${services.infrastructure.url}")
    private String infrastructureUrl;

    private final RestTemplate restTemplate;

    public Object createVps(Map<String, Object> params, Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("name", params.getOrDefault("name", "vps-" + System.currentTimeMillis()));
        body.put("os", params.getOrDefault("os", "ubuntu"));
        body.put("ram", toInt(params.getOrDefault("ram", 2048)));
        body.put("cpu", toInt(params.getOrDefault("cpu", 2)));
        body.put("storage", toInt(params.getOrDefault("storage", 50)));
        body.put("region", params.getOrDefault("region", "eu-west-1"));

        log.info("Création VPS: {}", body);
        return restTemplate.postForObject(infrastructureUrl + "/api/infrastructure/vps", body, Object.class);
    }

    public List<?> listVps(Long userId) {
        log.info("Liste VPS pour userId={}", userId);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                infrastructureUrl + "/api/infrastructure/vps/user/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public void deleteVps(Long vpsId) {
        log.info("Suppression VPS id={}", vpsId);
        restTemplate.delete(infrastructureUrl + "/api/infrastructure/vps/" + vpsId);
    }

    public Object createDomain(Map<String, Object> params, Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("name", params.get("name"));

        log.info("Enregistrement domaine: {}", body);
        return restTemplate.postForObject(infrastructureUrl + "/api/infrastructure/domains", body, Object.class);
    }

    public List<?> listDomains(Long userId) {
        log.info("Liste domaines pour userId={}", userId);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                infrastructureUrl + "/api/infrastructure/domains/user/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    public void deleteDomain(Long domainId) {
        restTemplate.delete(infrastructureUrl + "/api/infrastructure/domains/" + domainId);
    }

    public Object createCluster(Map<String, Object> params, Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("name", params.getOrDefault("name", "cluster-" + System.currentTimeMillis()));
        body.put("nodeCount", toInt(params.getOrDefault("nodeCount", 3)));
        body.put("kubernetesVersion", params.getOrDefault("kubernetesVersion", "1.28"));
        body.put("region", params.getOrDefault("region", "eu-west-1"));

        log.info("Création cluster K8s: {}", body);
        return restTemplate.postForObject(infrastructureUrl + "/api/infrastructure/clusters", body, Object.class);
    }

    public List<?> listClusters(Long userId) {
        log.info("Liste clusters pour userId={}", userId);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                infrastructureUrl + "/api/infrastructure/clusters/user/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return 0; }
    }
}
