package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateNetworkRequest;
import kali.microservices.infrastructureservice.entities.VpsNetwork;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.CreatedNetwork;
import kali.microservices.infrastructureservice.repository.VpsNetworkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkService {

    private final VpsNetworkRepository vpsNetworkRepository;
    private final CloudProviderFactory cloudProviderFactory;
    private final QuotaService quotaService;
    private final ClientProjectService clientProjectService;

    public VpsNetwork assignProject(Long id, Long projectId) {
        VpsNetwork network = getNetworkById(id);
        clientProjectService.validateAssignable(projectId, network.getUserId());
        network.setProjectId(projectId);
        return vpsNetworkRepository.save(network);
    }

    public VpsNetwork createNetwork(CreateNetworkRequest request) {
        quotaService.checkNetworkCreation(request.getUserId());

        VpsNetwork network = new VpsNetwork();
        network.setUserId(request.getUserId());
        network.setName(request.getName());
        network.setCidr(request.getCidr());
        network.setStatus(VpsNetwork.NetworkStatus.CREATING);
        network = vpsNetworkRepository.save(network);

        try {
            CreatedNetwork created = cloudProviderFactory.getProvider().createNetwork(request.getName(), request.getCidr());
            network.setExternalId(created.networkId());
            network.setSubnetId(created.subnetId());
            network.setStatus(VpsNetwork.NetworkStatus.ACTIVE);
        } catch (Exception e) {
            log.error("OpenStack network creation failed for network {}: {}", network.getId(), e.getMessage());
            network.setStatus(VpsNetwork.NetworkStatus.ERROR);
        }
        return vpsNetworkRepository.save(network);
    }

    public List<VpsNetwork> getNetworksByUser(Long userId) {
        return vpsNetworkRepository.findByUserId(userId).stream()
                .filter(n -> n.getStatus() != VpsNetwork.NetworkStatus.DELETED)
                .toList();
    }

    public List<VpsNetwork> getAllNetworks() {
        return vpsNetworkRepository.findAll().stream()
                .filter(n -> n.getStatus() != VpsNetwork.NetworkStatus.DELETED)
                .toList();
    }

    public VpsNetwork getNetworkById(Long id) {
        return vpsNetworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Network not found: " + id));
    }

    public void deleteNetwork(Long id) {
        VpsNetwork network = getNetworkById(id);
        if (network.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().deleteNetwork(network.getExternalId(), network.getSubnetId());
                network.setStatus(VpsNetwork.NetworkStatus.DELETED);
            } catch (Exception e) {
                log.error("Failed to delete OpenStack network {}: {}", network.getExternalId(), e.getMessage());
                network.setStatus(VpsNetwork.NetworkStatus.ERROR);
                vpsNetworkRepository.save(network);
                throw new RuntimeException("Failed to delete network on OpenStack: " + e.getMessage(), e);
            }
        } else {
            network.setStatus(VpsNetwork.NetworkStatus.DELETED);
        }
        vpsNetworkRepository.save(network);
    }
}
