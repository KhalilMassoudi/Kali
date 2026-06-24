package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateVpsRequest;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.VpsProvisionResponse;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpsService {

    private final VpsServerRepository vpsServerRepository;
    private final CloudProviderFactory cloudProviderFactory;

    public VpsServer createVps(CreateVpsRequest request) {
        VpsServer vps = new VpsServer();
        vps.setUserId(request.getUserId());
        vps.setName(request.getName());
        vps.setOs(request.getOs());
        vps.setRam(request.getRam());
        vps.setCpu(request.getCpu());
        vps.setStorage(request.getStorage());
        vps.setRegion(request.getRegion() != null ? request.getRegion() : "RegionOne");
        vps.setStatus(VpsServer.VpsStatus.PENDING);
        vps = vpsServerRepository.save(vps);

        try {
            VpsProvisionResponse response = cloudProviderFactory.getProvider()
                    .createVPS(request.getOs(), String.valueOf(request.getRam()), vps.getRegion());

            vps.setExternalId(response.getExternalId());
            vps.setIpAddress(response.getIpAddress());
            vps.setStatus(VpsServer.VpsStatus.RUNNING);
            log.info("VPS provisioned on OpenStack: id={}, externalId={}, ip={}",
                    vps.getId(), vps.getExternalId(), vps.getIpAddress());
        } catch (Exception e) {
            log.error("OpenStack provisioning failed for VPS {}: {}", vps.getId(), e.getMessage());
            vps.setStatus(VpsServer.VpsStatus.ERROR);
        }

        return vpsServerRepository.save(vps);
    }

    public List<VpsServer> getVpsByUser(Long userId) {
        return vpsServerRepository.findByUserId(userId);
    }

    public VpsServer getVpsById(Long id) {
        return vpsServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VPS not found: " + id));
    }

    public VpsServer updateVpsStatus(Long id, VpsServer.VpsStatus status) {
        VpsServer vps = getVpsById(id);
        vps.setStatus(status);
        return vpsServerRepository.save(vps);
    }

    public VpsServer stopVps(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().stopVPS(vps.getExternalId());
            } catch (Exception e) {
                log.error("Failed to stop OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
            }
        }
        vps.setStatus(VpsServer.VpsStatus.STOPPED);
        return vpsServerRepository.save(vps);
    }

    public VpsServer restartVps(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().restartVPS(vps.getExternalId());
            } catch (Exception e) {
                log.error("Failed to restart OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
            }
        }
        vps.setStatus(VpsServer.VpsStatus.RUNNING);
        return vpsServerRepository.save(vps);
    }

    public void deleteVps(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().deleteVPS(vps.getExternalId());
            } catch (Exception e) {
                log.error("Failed to delete OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
            }
        }
        vps.setStatus(VpsServer.VpsStatus.DELETED);
        vpsServerRepository.save(vps);
    }
}
