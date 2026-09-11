package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateServerGroupRequest;
import kali.microservices.infrastructureservice.entities.ClientServerGroup;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.ServerGroupDetails;
import kali.microservices.infrastructureservice.repository.ClientServerGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerGroupManagementService {

    private final ClientServerGroupRepository repository;
    private final CloudProviderFactory cloudProviderFactory;

    public ClientServerGroup create(CreateServerGroupRequest request) {
        ServerGroupDetails details = cloudProviderFactory.getProvider()
                .createServerGroup(request.getName(), request.getPolicy().toOpenStackValue());

        ClientServerGroup group = new ClientServerGroup();
        group.setUserId(request.getUserId());
        group.setName(request.getName());
        group.setExternalId(details.id());
        group.setPolicy(request.getPolicy());
        return repository.save(group);
    }

    public List<ClientServerGroup> getByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<ClientServerGroup> getAll() {
        return repository.findAll();
    }

    public ClientServerGroup getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Groupe de serveurs introuvable: " + id));
    }

    public ServerGroupDetails getLiveDetails(Long id) {
        ClientServerGroup group = getById(id);
        return cloudProviderFactory.getProvider().getServerGroup(group.getExternalId());
    }

    public void delete(Long id) {
        ClientServerGroup group = getById(id);
        try {
            cloudProviderFactory.getProvider().deleteServerGroup(group.getExternalId());
        } catch (Exception e) {
            log.error("Failed to delete OpenStack server group {}: {}", group.getExternalId(), e.getMessage());
            throw new RuntimeException("Échec de la suppression du groupe de serveurs sur OpenStack: " + e.getMessage(), e);
        }
        repository.deleteById(id);
    }
}
