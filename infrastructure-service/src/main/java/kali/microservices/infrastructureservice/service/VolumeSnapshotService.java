package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateVolumeSnapshotRequest;
import kali.microservices.infrastructureservice.entities.VpsVolume;
import kali.microservices.infrastructureservice.entities.VpsVolumeSnapshot;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.SnapshotDetails;
import kali.microservices.infrastructureservice.repository.VpsVolumeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cinder block-level volume snapshots — distinct from the VM-backup feature (Glance server
 * image). See VpsVolumeSnapshot's javadoc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolumeSnapshotService {

    private final VpsVolumeSnapshotRepository repository;
    private final VolumeService volumeService;
    private final CloudProviderFactory cloudProviderFactory;

    public VpsVolumeSnapshot createSnapshot(CreateVolumeSnapshotRequest request) {
        VpsVolume volume = volumeService.getVolumeById(request.getVolumeId());
        boolean force = volume.getStatus() == VpsVolume.VolumeStatus.IN_USE;

        VpsVolumeSnapshot snapshot = new VpsVolumeSnapshot();
        snapshot.setUserId(request.getUserId());
        snapshot.setSourceVolumeId(volume.getId());
        snapshot.setName(request.getName());
        snapshot.setDescription(request.getDescription());
        snapshot.setStatus(VpsVolumeSnapshot.SnapshotStatus.CREATING);
        snapshot = repository.save(snapshot);

        try {
            SnapshotDetails details = cloudProviderFactory.getProvider()
                    .createVolumeSnapshot(volume.getExternalId(), request.getName(), request.getDescription(), force);
            snapshot.setExternalId(details.id());
            snapshot.setSizeGb(details.sizeGb());
            snapshot.setStatus(VpsVolumeSnapshot.SnapshotStatus.AVAILABLE);
        } catch (Exception e) {
            log.error("OpenStack volume snapshot creation failed for snapshot {}: {}", snapshot.getId(), e.getMessage());
            snapshot.setStatus(VpsVolumeSnapshot.SnapshotStatus.ERROR);
        }
        return repository.save(snapshot);
    }

    public List<VpsVolumeSnapshot> getByUser(Long userId) {
        return repository.findByUserId(userId).stream()
                .filter(s -> s.getStatus() != VpsVolumeSnapshot.SnapshotStatus.DELETED)
                .toList();
    }

    public List<VpsVolumeSnapshot> getByVolume(Long volumeId) {
        return repository.findBySourceVolumeId(volumeId).stream()
                .filter(s -> s.getStatus() != VpsVolumeSnapshot.SnapshotStatus.DELETED)
                .toList();
    }

    public List<VpsVolumeSnapshot> getAll() {
        return repository.findAll().stream()
                .filter(s -> s.getStatus() != VpsVolumeSnapshot.SnapshotStatus.DELETED)
                .toList();
    }

    public VpsVolumeSnapshot getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Snapshot introuvable: " + id));
    }

    public void deleteSnapshot(Long id) {
        VpsVolumeSnapshot snapshot = getById(id);
        if (snapshot.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().deleteVolumeSnapshot(snapshot.getExternalId());
                snapshot.setStatus(VpsVolumeSnapshot.SnapshotStatus.DELETED);
            } catch (Exception e) {
                log.error("Failed to delete OpenStack snapshot {}: {}", snapshot.getExternalId(), e.getMessage());
                snapshot.setStatus(VpsVolumeSnapshot.SnapshotStatus.ERROR);
                repository.save(snapshot);
                throw new RuntimeException("Échec de la suppression du snapshot sur OpenStack: " + e.getMessage(), e);
            }
        } else {
            snapshot.setStatus(VpsVolumeSnapshot.SnapshotStatus.DELETED);
        }
        repository.save(snapshot);
    }
}
