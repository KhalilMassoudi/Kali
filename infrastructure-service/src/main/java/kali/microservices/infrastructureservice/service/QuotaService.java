package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.QuotaUsage;
import kali.microservices.infrastructureservice.entities.UserQuota;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.entities.VpsVolume;
import kali.microservices.infrastructureservice.entities.VpsNetwork;
import kali.microservices.infrastructureservice.entities.VpsSecurityGroup;
import kali.microservices.infrastructureservice.repository.UserQuotaRepository;
import kali.microservices.infrastructureservice.repository.VolumeRepository;
import kali.microservices.infrastructureservice.repository.VpsNetworkRepository;
import kali.microservices.infrastructureservice.repository.VpsSecurityGroupRepository;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Enforces per-user resource limits. Every create-path (VPS, volume, and — once Phases 3/4
 * land — network and security group) calls the matching check*() method before provisioning
 * anything on OpenStack, so a quota breach never leaves a half-created resource behind.
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserQuotaRepository userQuotaRepository;
    private final VpsServerRepository vpsServerRepository;
    private final VolumeRepository volumeRepository;
    private final VpsNetworkRepository vpsNetworkRepository;
    private final VpsSecurityGroupRepository vpsSecurityGroupRepository;

    public UserQuota getOrCreateDefault(Long userId) {
        return userQuotaRepository.findByUserId(userId).orElseGet(() -> {
            UserQuota quota = new UserQuota();
            quota.setUserId(userId);
            return userQuotaRepository.save(quota);
        });
    }

    public UserQuota updateQuota(Long userId, UserQuota patch) {
        UserQuota quota = getOrCreateDefault(userId);
        quota.setMaxVcpu(patch.getMaxVcpu());
        quota.setMaxRamMb(patch.getMaxRamMb());
        quota.setMaxStorageGb(patch.getMaxStorageGb());
        quota.setMaxVms(patch.getMaxVms());
        quota.setMaxVolumes(patch.getMaxVolumes());
        quota.setMaxNetworks(patch.getMaxNetworks());
        quota.setMaxSecurityGroups(patch.getMaxSecurityGroups());
        return userQuotaRepository.save(quota);
    }

    public QuotaUsage getUsage(Long userId) {
        var vms = vpsServerRepository.findByUserId(userId).stream()
                .filter(v -> v.getStatus() != VpsServer.VpsStatus.DELETED)
                .toList();
        var volumes = volumeRepository.findByUserId(userId).stream()
                .filter(v -> v.getStatus() != VpsVolume.VolumeStatus.DELETED)
                .toList();

        int totalVcpu = vms.stream().mapToInt(v -> v.getCpu() != null ? v.getCpu() : 0).sum();
        int totalRamMb = vms.stream().mapToInt(v -> v.getRam() != null ? v.getRam() : 0).sum();
        int vmStorageGb = vms.stream().mapToInt(v -> v.getStorage() != null ? v.getStorage() : 0).sum();
        int volumeStorageGb = volumes.stream().mapToInt(v -> v.getSizeGb() != null ? v.getSizeGb() : 0).sum();

        long networkCount = vpsNetworkRepository.findByUserId(userId).stream()
                .filter(n -> n.getStatus() != VpsNetwork.NetworkStatus.DELETED)
                .count();
        long securityGroupCount = vpsSecurityGroupRepository.findByUserId(userId).stream()
                .filter(g -> g.getStatus() != VpsSecurityGroup.SecurityGroupStatus.DELETED)
                .count();

        return new QuotaUsage(vms.size(), totalVcpu, totalRamMb, volumes.size(),
                vmStorageGb + volumeStorageGb, (int) networkCount, (int) securityGroupCount);
    }

    public void checkNetworkCreation(Long userId) {
        UserQuota quota = getOrCreateDefault(userId);
        QuotaUsage usage = getUsage(userId);
        if (usage.networkCount() + 1 > quota.getMaxNetworks()) {
            throw quotaExceeded("Nombre maximum de réseaux atteint (" + quota.getMaxNetworks() + ")");
        }
    }

    public void checkSecurityGroupCreation(Long userId) {
        UserQuota quota = getOrCreateDefault(userId);
        QuotaUsage usage = getUsage(userId);
        if (usage.securityGroupCount() + 1 > quota.getMaxSecurityGroups()) {
            throw quotaExceeded("Nombre maximum de groupes de sécurité atteint (" + quota.getMaxSecurityGroups() + ")");
        }
    }

    public void checkVpsCreation(Long userId, int vcpu, int ramMb, int storageGb) {
        UserQuota quota = getOrCreateDefault(userId);
        QuotaUsage usage = getUsage(userId);
        if (usage.vmCount() + 1 > quota.getMaxVms()) {
            throw quotaExceeded("Nombre maximum de VMs atteint (" + quota.getMaxVms() + ")");
        }
        if (usage.totalVcpu() + vcpu > quota.getMaxVcpu()) {
            throw quotaExceeded("Quota de vCPU dépassé (max " + quota.getMaxVcpu() + ")");
        }
        if (usage.totalRamMb() + ramMb > quota.getMaxRamMb()) {
            throw quotaExceeded("Quota de RAM dépassé (max " + quota.getMaxRamMb() + " MB)");
        }
        if (usage.totalStorageGb() + storageGb > quota.getMaxStorageGb()) {
            throw quotaExceeded("Quota de stockage dépassé (max " + quota.getMaxStorageGb() + " GB)");
        }
    }

    public void checkVolumeCreation(Long userId, int sizeGb) {
        UserQuota quota = getOrCreateDefault(userId);
        QuotaUsage usage = getUsage(userId);
        if (usage.volumeCount() + 1 > quota.getMaxVolumes()) {
            throw quotaExceeded("Nombre maximum de volumes atteint (" + quota.getMaxVolumes() + ")");
        }
        if (usage.totalStorageGb() + sizeGb > quota.getMaxStorageGb()) {
            throw quotaExceeded("Quota de stockage dépassé (max " + quota.getMaxStorageGb() + " GB)");
        }
    }

    private ResponseStatusException quotaExceeded(String message) {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, message);
    }
}
