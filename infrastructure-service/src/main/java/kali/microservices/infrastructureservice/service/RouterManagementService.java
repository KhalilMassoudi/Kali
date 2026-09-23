package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateRouterRequest;
import kali.microservices.infrastructureservice.entities.ClientRouter;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.RouterDetails;
import kali.microservices.infrastructureservice.openstack.RouterInterfaceDetails;
import kali.microservices.infrastructureservice.repository.ClientRouterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouterManagementService {

    private final ClientRouterRepository repository;
    private final CloudProviderFactory cloudProviderFactory;

    public ClientRouter create(CreateRouterRequest request) {
        RouterDetails details = cloudProviderFactory.getProvider()
                .createRouter(request.getName(), request.getExternalNetworkId());

        ClientRouter router = new ClientRouter();
        router.setUserId(request.getUserId());
        router.setName(request.getName());
        router.setExternalId(details.id());
        router.setExternalGatewayNetworkId(details.externalGatewayNetworkId());
        return repository.save(router);
    }

    public List<ClientRouter> getByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<ClientRouter> getAll() {
        return repository.findAll();
    }

    public ClientRouter getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Routeur introuvable: " + id));
    }

    public List<RouterInterfaceDetails> listInterfaces(Long id) {
        ClientRouter router = getById(id);
        return cloudProviderFactory.getProvider().listRouterInterfaces(router.getExternalId());
    }

    public void attachInterface(Long id, String subnetId) {
        ClientRouter router = getById(id);
        cloudProviderFactory.getProvider().attachRouterInterface(router.getExternalId(), subnetId);
    }

    public void detachInterface(Long id, String subnetId) {
        ClientRouter router = getById(id);
        cloudProviderFactory.getProvider().detachRouterInterface(router.getExternalId(), subnetId);
    }

    public ClientRouter setGateway(Long id, String externalNetworkId) {
        ClientRouter router = getById(id);
        cloudProviderFactory.getProvider().setRouterGateway(router.getExternalId(), externalNetworkId);
        router.setExternalGatewayNetworkId(externalNetworkId);
        return repository.save(router);
    }

    public ClientRouter clearGateway(Long id) {
        ClientRouter router = getById(id);
        cloudProviderFactory.getProvider().clearRouterGateway(router.getExternalId());
        router.setExternalGatewayNetworkId(null);
        return repository.save(router);
    }

    public void delete(Long id) {
        ClientRouter router = getById(id);
        try {
            cloudProviderFactory.getProvider().deleteRouter(router.getExternalId());
        } catch (Exception e) {
            log.error("Failed to delete OpenStack router {}: {}", router.getExternalId(), e.getMessage());
            throw new RuntimeException("Échec de la suppression du routeur sur OpenStack: " + e.getMessage(), e);
        }
        repository.deleteById(id);
    }
}
