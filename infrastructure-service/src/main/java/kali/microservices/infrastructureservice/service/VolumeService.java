package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateVolumeRequest;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.entities.VpsVolume;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.VolumeDetails;
import kali.microservices.infrastructureservice.repository.VolumeRepository;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolumeService {

    private final VolumeRepository volumeRepository;
    private final VpsServerRepository vpsServerRepository;
    private final CloudProviderFactory cloudProviderFactory;
    private final QuotaService quotaService;
    private final ClientProjectService clientProjectService;

    public VpsVolume assignProject(Long id, Long projectId) {
        VpsVolume volume = getVolumeById(id);
        clientProjectService.validateAssignable(projectId, volume.getUserId());
        volume.setProjectId(projectId);
        return volumeRepository.save(volume);
    }

    public VpsVolume createVolume(CreateVolumeRequest request) {
        quotaService.checkVolumeCreation(request.getUserId(), request.getSizeGb());
        VpsVolume volume = new VpsVolume();
        volume.setUserId(request.getUserId());
        volume.setName(request.getName());
        volume.setSizeGb(request.getSizeGb());
        volume.setRegion(request.getRegion() != null ? request.getRegion() : "RegionOne");
        volume.setStatus(VpsVolume.VolumeStatus.CREATING);
        volume = volumeRepository.save(volume);

        try {
            VolumeDetails details = cloudProviderFactory.getProvider()
                    .createVolume(request.getName(), request.getSizeGb(), volume.getRegion());
            volume.setExternalId(details.getExternalId());
            volume.setStatus(VpsVolume.VolumeStatus.AVAILABLE);
            log.info("Volume provisioned on OpenStack: id={}, externalId={}", volume.getId(), volume.getExternalId());
        } catch (Exception e) {
            log.error("OpenStack volume creation failed for volume {}: {}", volume.getId(), e.getMessage());
            volume.setStatus(VpsVolume.VolumeStatus.ERROR);
        }

        return volumeRepository.save(volume);
    }

    public List<VpsVolume> getVolumesByUser(Long userId) {
        return volumeRepository.findByUserId(userId);
    }

    public List<VpsVolume> getAllVolumes() {
        return volumeRepository.findAll();
    }

    public VpsVolume getVolumeById(Long id) {
        return volumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Volume not found: " + id));
    }

    public VpsVolume attachVolume(Long volumeId, Long vpsId) {
        VpsVolume volume = getVolumeById(volumeId);
        VpsServer vps = vpsServerRepository.findById(vpsId)
                .orElseThrow(() -> new RuntimeException("VPS not found: " + vpsId));

        if (volume.getExternalId() == null || vps.getExternalId() == null) {
            throw new RuntimeException("Volume or VPS has no associated OpenStack resource yet");
        }

        String device = "/dev/vd" + (char) ('b' + volumeRepository.findByAttachedVpsId(vpsId).size());
        try {
            cloudProviderFactory.getProvider().attachVolume(vps.getExternalId(), volume.getExternalId(), device);
            volume.setAttachedVpsId(vpsId);
            volume.setDevice(device);
            volume.setStatus(VpsVolume.VolumeStatus.IN_USE);
        } catch (Exception e) {
            log.error("Failed to attach volume {} to VPS {}: {}", volumeId, vpsId, e.getMessage());
            volume.setStatus(VpsVolume.VolumeStatus.ERROR);
        }
        return volumeRepository.save(volume);
    }

    public VpsVolume detachVolume(Long volumeId) {
        VpsVolume volume = getVolumeById(volumeId);
        if (volume.getAttachedVpsId() == null) {
            return volume;
        }
        VpsServer vps = vpsServerRepository.findById(volume.getAttachedVpsId())
                .orElseThrow(() -> new RuntimeException("VPS not found: " + volume.getAttachedVpsId()));

        try {
            if (vps.getExternalId() != null && volume.getExternalId() != null) {
                cloudProviderFactory.getProvider().detachVolume(vps.getExternalId(), volume.getExternalId());
            }
            volume.setAttachedVpsId(null);
            volume.setDevice(null);
            volume.setStatus(VpsVolume.VolumeStatus.AVAILABLE);
        } catch (Exception e) {
            log.error("Failed to detach volume {}: {}", volumeId, e.getMessage());
            volume.setStatus(VpsVolume.VolumeStatus.ERROR);
        }
        return volumeRepository.save(volume);
    }

    public void deleteVolume(Long id) {
        VpsVolume volume = getVolumeById(id);
        if (volume.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().deleteVolume(volume.getExternalId());
                volume.setStatus(VpsVolume.VolumeStatus.DELETED);
            } catch (Exception e) {
                log.error("Failed to delete OpenStack volume {}: {}", volume.getExternalId(), e.getMessage());
                volume.setStatus(VpsVolume.VolumeStatus.ERROR);
                volumeRepository.save(volume);
                throw new RuntimeException("Failed to delete volume on OpenStack: " + e.getMessage(), e);
            }
        } else {
            volume.setStatus(VpsVolume.VolumeStatus.DELETED);
        }
        volumeRepository.save(volume);
    }
}
