package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateVpsRequest;
import kali.microservices.infrastructureservice.dto.VmHealthResponse;
import kali.microservices.infrastructureservice.entities.ClientFloatingIp;
import kali.microservices.infrastructureservice.entities.ClientKeypair;
import kali.microservices.infrastructureservice.entities.ClientServerGroup;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.repository.ClientFloatingIpRepository;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.openstack.SecurityGroupOption;
import kali.microservices.infrastructureservice.openstack.VpsDetails;
import kali.microservices.infrastructureservice.openstack.VpsProvisionResponse;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpsService {

    private final VpsServerRepository vpsServerRepository;
    private final CloudProviderFactory cloudProviderFactory;
    private final QuotaService quotaService;
    private final VpsActivityLogService activityLogService;
    private final ClientProjectService clientProjectService;
    private final KeypairManagementService keypairManagementService;
    private final ServerGroupManagementService serverGroupManagementService;
    private final ClientFloatingIpRepository clientFloatingIpRepository;

    public VpsServer assignProject(Long id, Long projectId) {
        VpsServer vps = getVpsById(id);
        clientProjectService.validateAssignable(projectId, vps.getUserId());
        vps.setProjectId(projectId);
        return vpsServerRepository.save(vps);
    }

    public VpsServer createVps(CreateVpsRequest request) {
        quotaService.checkVpsCreation(request.getUserId(), request.getCpu(), request.getRam(), request.getStorage());
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

        String resolvedKeypairName = resolveKeypairName(request.getKeypairId(), request.getUserId());
        String resolvedServerGroupId = resolveServerGroupId(request.getServerGroupId(), request.getUserId());

        try {
            var provider = cloudProviderFactory.getProvider();
            VpsProvisionResponse response = (request.getImageId() != null && !request.getImageId().isBlank())
                    ? provider.createVPSFromImage(request.getImageId(), String.valueOf(request.getRam()), vps.getRegion(),
                            request.getNetworkId(), request.getSecurityGroups(), resolvedKeypairName, resolvedServerGroupId)
                    : provider.createVPS(request.getOs(), String.valueOf(request.getRam()), vps.getRegion(),
                            request.getNetworkId(), request.getSecurityGroups(), resolvedKeypairName, resolvedServerGroupId);

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
        return vpsServerRepository.findByUserId(userId).stream()
                .filter(vps -> vps.getStatus() != VpsServer.VpsStatus.DELETED)
                .toList();
    }

    public List<VpsServer> getAllVps() {
        return vpsServerRepository.findAll().stream()
                .filter(vps -> vps.getStatus() != VpsServer.VpsStatus.DELETED)
                .toList();
    }

    private String resolveKeypairName(Long keypairId, Long requestingUserId) {
        if (keypairId == null) return null;
        ClientKeypair keypair = keypairManagementService.getById(keypairId);
        if (!keypair.getUserId().equals(requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette paire de clés n'appartient pas à ce client");
        }
        return keypair.getOpenstackName();
    }

    private String resolveServerGroupId(Long serverGroupId, Long requestingUserId) {
        if (serverGroupId == null) return null;
        ClientServerGroup group = serverGroupManagementService.getById(serverGroupId);
        if (!group.getUserId().equals(requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce groupe de serveurs n'appartient pas à ce client");
        }
        return group.getExternalId();
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

    public VpsServer stopVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().stopVPS(vps.getExternalId());
                vps.setStatus(VpsServer.VpsStatus.STOPPED);
            } catch (Exception e) {
                log.error("Failed to stop OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
                vps.setStatus(VpsServer.VpsStatus.ERROR);
            }
        } else {
            vps.setStatus(VpsServer.VpsStatus.STOPPED);
        }
        activityLogService.record(id, actingUserId, "STOP", null);
        return vpsServerRepository.save(vps);
    }

    public VpsServer startVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().startVPS(vps.getExternalId());
                vps.setStatus(VpsServer.VpsStatus.RUNNING);
            } catch (Exception e) {
                log.error("Failed to start OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
                vps.setStatus(VpsServer.VpsStatus.ERROR);
            }
        } else {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        activityLogService.record(id, actingUserId, "START", null);
        return vpsServerRepository.save(vps);
    }

    public VpsServer restartVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().restartVPS(vps.getExternalId());
                vps.setStatus(VpsServer.VpsStatus.RUNNING);
            } catch (Exception e) {
                log.error("Failed to restart OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
                vps.setStatus(VpsServer.VpsStatus.ERROR);
            }
        } else {
            vps.setStatus(VpsServer.VpsStatus.RUNNING);
        }
        activityLogService.record(id, actingUserId, "RESTART", null);
        return vpsServerRepository.save(vps);
    }

    public VpsServer pauseVps(Long id, Long actingUserId) {
        return performStatusAction(id, actingUserId, "PAUSE", VpsServer.VpsStatus.PAUSED, cloudProviderFactory.getProvider()::pauseVPS);
    }

    public VpsServer unpauseVps(Long id, Long actingUserId) {
        return performStatusAction(id, actingUserId, "UNPAUSE", VpsServer.VpsStatus.RUNNING, cloudProviderFactory.getProvider()::unpauseVPS);
    }

    public VpsServer suspendVps(Long id, Long actingUserId) {
        return performStatusAction(id, actingUserId, "SUSPEND", VpsServer.VpsStatus.SUSPENDED, cloudProviderFactory.getProvider()::suspendVPS);
    }

    public VpsServer resumeVps(Long id, Long actingUserId) {
        return performStatusAction(id, actingUserId, "RESUME", VpsServer.VpsStatus.RUNNING, cloudProviderFactory.getProvider()::resumeVPS);
    }

    public VpsServer shelveVps(Long id, Long actingUserId) {
        return performStatusAction(id, actingUserId, "SHELVE", VpsServer.VpsStatus.SHELVED, cloudProviderFactory.getProvider()::shelveVPS);
    }

    public VpsServer unshelveVps(Long id, Long actingUserId) {
        return performStatusAction(id, actingUserId, "UNSHELVE", VpsServer.VpsStatus.RUNNING, cloudProviderFactory.getProvider()::unshelveVPS);
    }

    public VpsServer softRebootVps(Long id, Long actingUserId) {
        return performStatusAction(id, actingUserId, "SOFT_REBOOT", VpsServer.VpsStatus.RUNNING, cloudProviderFactory.getProvider()::softRebootVPS);
    }

    private VpsServer performStatusAction(Long id, Long actingUserId, String actionName, VpsServer.VpsStatus newStatus,
                                           java.util.function.Consumer<String> providerCall) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        try {
            providerCall.accept(vps.getExternalId());
            vps.setStatus(newStatus);
        } catch (Exception e) {
            log.error("Failed to {} OpenStack server {}: {}", actionName, vps.getExternalId(), e.getMessage());
            vps.setStatus(VpsServer.VpsStatus.ERROR);
        }
        activityLogService.record(id, actingUserId, actionName, null);
        return vpsServerRepository.save(vps);
    }

    public VpsServer lockVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().lockVPS(vps.getExternalId());
        vps.setLocked(true);
        activityLogService.record(id, actingUserId, "LOCK", null);
        return vpsServerRepository.save(vps);
    }

    public VpsServer unlockVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().unlockVPS(vps.getExternalId());
        vps.setLocked(false);
        activityLogService.record(id, actingUserId, "UNLOCK", null);
        return vpsServerRepository.save(vps);
    }

    public void rebuildVps(Long id, Long actingUserId, String imageId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().rebuildVPS(vps.getExternalId(), imageId);
        activityLogService.record(id, actingUserId, "REBUILD", imageId);
    }

    public String getConsoleUrl(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        return cloudProviderFactory.getProvider().getConsoleUrl(vps.getExternalId());
    }

    public String getConsoleLog(Long id, int numLines) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        return cloudProviderFactory.getProvider().getConsoleLog(vps.getExternalId(), numLines);
    }

    public List<kali.microservices.infrastructureservice.openstack.InterfaceDetails> listInterfaces(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        return cloudProviderFactory.getProvider().listInterfaces(vps.getExternalId());
    }

    public kali.microservices.infrastructureservice.openstack.InterfaceDetails attachInterface(Long id, Long actingUserId, String networkId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        var attachment = cloudProviderFactory.getProvider().attachInterface(vps.getExternalId(), networkId);
        activityLogService.record(id, actingUserId, "ATTACH_INTERFACE", networkId);
        return attachment;
    }

    public void detachInterface(Long id, Long actingUserId, String attachmentId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().detachInterface(vps.getExternalId(), attachmentId);
        activityLogService.record(id, actingUserId, "DETACH_INTERFACE", attachmentId);
    }

    public List<String> getPortSecurityGroups(String portId) {
        return cloudProviderFactory.getProvider().getPortSecurityGroups(portId);
    }

    public void updatePortSecurityGroups(Long id, Long actingUserId, String portId, List<String> groupIds) {
        cloudProviderFactory.getProvider().updatePortSecurityGroups(portId, groupIds);
        activityLogService.record(id, actingUserId, "UPDATE_PORT_SECURITY_GROUPS", portId);
    }

    public void deleteVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            try {
                cloudProviderFactory.getProvider().deleteVPS(vps.getExternalId());
                vps.setStatus(VpsServer.VpsStatus.DELETED);
            } catch (Exception e) {
                log.error("Failed to delete OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
                vps.setStatus(VpsServer.VpsStatus.ERROR);
                vpsServerRepository.save(vps);
                throw new RuntimeException("Failed to delete VPS on OpenStack: " + e.getMessage(), e);
            }
        } else {
            vps.setStatus(VpsServer.VpsStatus.DELETED);
        }
        activityLogService.record(id, actingUserId, "DELETE", null);
        vpsServerRepository.save(vps);
    }

    /**
     * Re-syncs local status/IP from OpenStack. Only called on explicit user request
     * (no background reconciliation) — if OpenStack is unreachable, the exception
     * propagates rather than silently guessing a new state.
     */
    public VpsServer refreshStatus(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            return vps;
        }
        VpsDetails details = cloudProviderFactory.getProvider().getVPS(vps.getExternalId());
        vps.setStatus(mapOpenStackStatus(details.getStatus(), vps.getStatus()));
        if (details.getIpAddress() != null) {
            vps.setIpAddress(details.getIpAddress());
        }
        return vpsServerRepository.save(vps);
    }

    public VmHealthResponse getHealth(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            return new VmHealthResponse(vps.getStatus().name(), vps.getIpAddress(), vps.getFloatingIp(),
                    java.util.Map.of(), LocalDateTime.now());
        }
        VpsDetails details = cloudProviderFactory.getProvider().getVPS(vps.getExternalId());
        var diagnostics = cloudProviderFactory.getProvider().getDiagnostics(vps.getExternalId());
        return new VmHealthResponse(details.getStatus(), details.getIpAddress(), vps.getFloatingIp(),
                diagnostics, LocalDateTime.now());
    }

    public VpsServer allocateAndAssociateFloatingIp(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        var provider = cloudProviderFactory.getProvider();
        List<String> pools = provider.listFloatingIpPools();
        String pool = pools.isEmpty() ? null : pools.get(0);
        String ip = provider.allocateFloatingIp(pool);
        provider.associateFloatingIp(vps.getExternalId(), ip);
        vps.setFloatingIp(ip);
        activityLogService.record(id, actingUserId, "ALLOCATE_FLOATING_IP", ip);

        ClientFloatingIp tracked = new ClientFloatingIp();
        tracked.setUserId(actingUserId);
        tracked.setFloatingIpAddress(ip);
        tracked.setPool(pool);
        tracked.setAssociatedVpsId(id);
        clientFloatingIpRepository.save(tracked);

        return vpsServerRepository.save(vps);
    }

    public VpsServer releaseFloatingIp(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getFloatingIp() == null) {
            return vps;
        }
        cloudProviderFactory.getProvider().releaseFloatingIp(vps.getFloatingIp());
        activityLogService.record(id, actingUserId, "RELEASE_FLOATING_IP", vps.getFloatingIp());
        clientFloatingIpRepository.findByFloatingIpAddress(vps.getFloatingIp())
                .ifPresent(clientFloatingIpRepository::delete);
        vps.setFloatingIp(null);
        return vpsServerRepository.save(vps);
    }

    public void assignSecurityGroup(Long id, String name, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().assignSecurityGroup(vps.getExternalId(), name);
        activityLogService.record(id, actingUserId, "ASSIGN_SECURITY_GROUP", name);
    }

    public void removeSecurityGroup(Long id, String name, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().removeSecurityGroup(vps.getExternalId(), name);
        activityLogService.record(id, actingUserId, "REMOVE_SECURITY_GROUP", name);
    }

    public VpsServer resizeVps(Long id, String flavorId, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        if (vps.getStatus() != VpsServer.VpsStatus.STOPPED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le VPS doit être à l'arrêt avant un redimensionnement");
        }
        vps.setStatus(VpsServer.VpsStatus.RESIZING);
        vpsServerRepository.save(vps);
        try {
            cloudProviderFactory.getProvider().resizeVPS(vps.getExternalId(), flavorId);
            vps.setStatus(VpsServer.VpsStatus.VERIFY_RESIZE);
        } catch (Exception e) {
            log.error("Failed to resize OpenStack server {}: {}", vps.getExternalId(), e.getMessage());
            vps.setStatus(VpsServer.VpsStatus.ERROR);
            vpsServerRepository.save(vps);
            throw new RuntimeException("Échec du redimensionnement: " + e.getMessage(), e);
        }
        activityLogService.record(id, actingUserId, "RESIZE", "flavorId=" + flavorId);
        return vpsServerRepository.save(vps);
    }

    public VpsServer confirmResize(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().confirmResizeVPS(vps.getExternalId());
        vps.setStatus(VpsServer.VpsStatus.STOPPED);
        activityLogService.record(id, actingUserId, "CONFIRM_RESIZE", null);
        return vpsServerRepository.save(vps);
    }

    public VpsServer revertResize(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().revertResizeVPS(vps.getExternalId());
        vps.setStatus(VpsServer.VpsStatus.STOPPED);
        activityLogService.record(id, actingUserId, "REVERT_RESIZE", null);
        return vpsServerRepository.save(vps);
    }

    public String snapshotVps(Long id, String name, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        String imageId = cloudProviderFactory.getProvider().snapshotVPS(vps.getExternalId(), name);
        activityLogService.record(id, actingUserId, "SNAPSHOT", name);
        return imageId;
    }

    public VpsServer renameVps(Long id, String newName, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() != null) {
            cloudProviderFactory.getProvider().renameVPS(vps.getExternalId(), newName);
        }
        activityLogService.record(id, actingUserId, "RENAME", vps.getName() + " -> " + newName);
        vps.setName(newName);
        return vpsServerRepository.save(vps);
    }

    public List<SecurityGroupOption> getCurrentSecurityGroups(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            return List.of();
        }
        return cloudProviderFactory.getProvider().getCurrentSecurityGroups(vps.getExternalId());
    }

    public Map<String, String> getMetadata(Long id) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            return Map.of();
        }
        return cloudProviderFactory.getProvider().getMetadata(vps.getExternalId());
    }

    public Map<String, String> updateMetadata(Long id, Map<String, String> metadata, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        Map<String, String> result = cloudProviderFactory.getProvider().updateMetadata(vps.getExternalId(), metadata);
        activityLogService.record(id, actingUserId, "UPDATE_METADATA", metadata.toString());
        return result;
    }

    public kali.microservices.infrastructureservice.dto.PageResponse<kali.microservices.infrastructureservice.entities.VpsActivityLog> getActivity(Long id, int page, int size) {
        return activityLogService.list(id, page, size);
    }

    public String rescueVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        String password = cloudProviderFactory.getProvider().rescueVPS(vps.getExternalId());
        vps.setStatus(VpsServer.VpsStatus.RESCUE);
        vpsServerRepository.save(vps);
        activityLogService.record(id, actingUserId, "RESCUE", null);
        return password;
    }

    public VpsServer unrescueVps(Long id, Long actingUserId) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().unrescueVPS(vps.getExternalId());
        vps.setStatus(VpsServer.VpsStatus.RUNNING);
        activityLogService.record(id, actingUserId, "UNRESCUE", null);
        return vpsServerRepository.save(vps);
    }

    public void deleteMetadataItem(Long id, String key) {
        VpsServer vps = getVpsById(id);
        if (vps.getExternalId() == null) {
            throw new RuntimeException("VPS " + id + " has no OpenStack server associated with it");
        }
        cloudProviderFactory.getProvider().deleteMetadataItem(vps.getExternalId(), key);
    }

    private VpsServer.VpsStatus mapOpenStackStatus(String openStackStatus, VpsServer.VpsStatus fallback) {
        if (openStackStatus == null) return fallback;
        return switch (openStackStatus.toUpperCase()) {
            case "ACTIVE" -> VpsServer.VpsStatus.RUNNING;
            case "SHUTOFF", "STOPPED", "SUSPENDED", "PAUSED" -> VpsServer.VpsStatus.STOPPED;
            case "ERROR" -> VpsServer.VpsStatus.ERROR;
            case "DELETED" -> VpsServer.VpsStatus.DELETED;
            default -> fallback; // BUILD, REBOOT, RESIZE, MIGRATING, UNKNOWN... leave as-is
        };
    }
}
