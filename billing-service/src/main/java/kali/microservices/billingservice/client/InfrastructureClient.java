package kali.microservices.billingservice.client;

import kali.microservices.billingservice.security.ServiceTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InfrastructureClient {

    private final RestTemplate restTemplate;
    private final ServiceTokenProvider tokenProvider;

    @Value("${services.infrastructure.url}")
    private String baseUrl;

    private HttpEntity<Void> authenticatedEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.mintServiceAdminToken());
        return new HttpEntity<>(headers);
    }

    public List<VmSnapshot> getAllVms() {
        try {
            var response = restTemplate.exchange(baseUrl + "/api/infrastructure/admin/vps", HttpMethod.GET,
                    authenticatedEntity(), VmSnapshot[].class);
            return response.getBody() != null ? List.of(response.getBody()) : List.of();
        } catch (Exception e) {
            log.error("Metering: failed to fetch VMs from infrastructure-service: {}", e.getMessage());
            return List.of();
        }
    }

    public List<VolumeSnapshot> getAllVolumes() {
        try {
            var response = restTemplate.exchange(baseUrl + "/api/infrastructure/admin/volumes", HttpMethod.GET,
                    authenticatedEntity(), VolumeSnapshot[].class);
            return response.getBody() != null ? List.of(response.getBody()) : List.of();
        } catch (Exception e) {
            log.error("Metering: failed to fetch volumes from infrastructure-service: {}", e.getMessage());
            return List.of();
        }
    }

    public List<FloatingIpSnapshot> getAllFloatingIps() {
        try {
            var response = restTemplate.exchange(baseUrl + "/api/infrastructure/admin/floating-ips", HttpMethod.GET,
                    authenticatedEntity(), FloatingIpSnapshot[].class);
            return response.getBody() != null ? List.of(response.getBody()) : List.of();
        } catch (Exception e) {
            log.error("Metering: failed to fetch floating IPs from infrastructure-service: {}", e.getMessage());
            return List.of();
        }
    }

    /** Zero-balance suspension - same stop endpoint the client's own "Arrêter" button uses. */
    public boolean stopVm(Long vmId) {
        try {
            restTemplate.exchange(baseUrl + "/api/infrastructure/vps/" + vmId + "/stop", HttpMethod.POST,
                    authenticatedEntity(), Void.class);
            return true;
        } catch (Exception e) {
            log.error("Suspension: failed to stop VM {} via infrastructure-service: {}", vmId, e.getMessage());
            return false;
        }
    }
}
