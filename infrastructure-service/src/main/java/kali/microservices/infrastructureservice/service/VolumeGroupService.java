package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateVolumeGroupRequest;
import kali.microservices.infrastructureservice.entities.ClientVolumeGroup;
import kali.microservices.infrastructureservice.openstack.CinderGroupClient;
import kali.microservices.infrastructureservice.repository.ClientVolumeGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Best-effort — see ClientVolumeGroup's javadoc. Any Cinder-side failure here should read as
 * "not supported by this deployment" to callers, not a hard platform error.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolumeGroupService {

    private final ClientVolumeGroupRepository repository;
    private final CinderGroupClient cinderGroupClient;

    /** Probe used by the frontend to feature-detect support before rendering the tab. */
    public boolean isSupported() {
        try {
            cinderGroupClient.listGroupTypes();
            return true;
        } catch (Exception e) {
            log.info("Cinder generic volume groups not available on this deployment: {}", e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> listGroupTypes() {
        return cinderGroupClient.listGroupTypes();
    }

    public ClientVolumeGroup createGroup(CreateVolumeGroupRequest request) {
        Map<String, Object> response = cinderGroupClient.createGroup(
                request.getName(), request.getDescription(), request.getGroupTypeId(), request.getVolumeTypeIds());
        @SuppressWarnings("unchecked")
        Map<String, Object> group = (Map<String, Object>) response.get("group");

        ClientVolumeGroup entity = new ClientVolumeGroup();
        entity.setUserId(request.getUserId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setExternalId(String.valueOf(group.get("id")));
        return repository.save(entity);
    }

    public List<ClientVolumeGroup> getByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<ClientVolumeGroup> getAll() {
        return repository.findAll();
    }

    public ClientVolumeGroup getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Groupe de volumes introuvable: " + id));
    }

    public void deleteGroup(Long id) {
        ClientVolumeGroup group = getById(id);
        try {
            cinderGroupClient.deleteGroup(group.getExternalId());
        } catch (Exception e) {
            log.error("Failed to delete Cinder volume group {}: {}", group.getExternalId(), e.getMessage());
            throw new RuntimeException("Échec de la suppression du groupe de volumes sur OpenStack: " + e.getMessage(), e);
        }
        repository.deleteById(id);
    }

    public List<Map<String, Object>> listGroupSnapshots() {
        return cinderGroupClient.listGroupSnapshots();
    }

    public Map<String, Object> createGroupSnapshot(String groupExternalId, String name, String description) {
        return cinderGroupClient.createGroupSnapshot(groupExternalId, name, description);
    }

    public void deleteGroupSnapshot(String id) {
        cinderGroupClient.deleteGroupSnapshot(id);
    }
}
