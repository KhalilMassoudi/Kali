package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.entities.ClientFloatingIp;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.openstack.CloudProviderFactory;
import kali.microservices.infrastructureservice.repository.ClientFloatingIpRepository;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FloatingIpManagementService {

    private final ClientFloatingIpRepository repository;
    private final VpsServerRepository vpsServerRepository;
    private final CloudProviderFactory cloudProviderFactory;

    public ClientFloatingIp allocate(Long userId, String pool) {
        var provider = cloudProviderFactory.getProvider();
        String resolvedPool = pool;
        if (resolvedPool == null || resolvedPool.isBlank()) {
            List<String> pools = provider.listFloatingIpPools();
            resolvedPool = pools.isEmpty() ? null : pools.get(0);
        }
        String address = provider.allocateFloatingIp(resolvedPool);

        ClientFloatingIp entity = new ClientFloatingIp();
        entity.setUserId(userId);
        entity.setFloatingIpAddress(address);
        entity.setPool(resolvedPool);
        return repository.save(entity);
    }

    public ClientFloatingIp associate(Long id, Long vpsId) {
        ClientFloatingIp ip = getById(id);
        VpsServer vps = vpsServerRepository.findById(vpsId)
                .orElseThrow(() -> new RuntimeException("VPS introuvable: " + vpsId));
        if (vps.getExternalId() == null) {
            throw new RuntimeException("Cette VM n'a pas encore de serveur OpenStack associé");
        }
        cloudProviderFactory.getProvider().associateFloatingIp(vps.getExternalId(), ip.getFloatingIpAddress());
        ip.setAssociatedVpsId(vpsId);
        return repository.save(ip);
    }

    public ClientFloatingIp disassociate(Long id) {
        ClientFloatingIp ip = getById(id);
        if (ip.getAssociatedVpsId() == null) {
            return ip;
        }
        VpsServer vps = vpsServerRepository.findById(ip.getAssociatedVpsId())
                .orElseThrow(() -> new RuntimeException("VPS introuvable: " + ip.getAssociatedVpsId()));
        if (vps.getExternalId() != null) {
            cloudProviderFactory.getProvider().disassociateFloatingIp(vps.getExternalId(), ip.getFloatingIpAddress());
        }
        ip.setAssociatedVpsId(null);
        return repository.save(ip);
    }

    public void release(Long id) {
        ClientFloatingIp ip = getById(id);
        try {
            cloudProviderFactory.getProvider().releaseFloatingIp(ip.getFloatingIpAddress());
        } catch (Exception e) {
            log.error("Failed to release floating IP {}: {}", ip.getFloatingIpAddress(), e.getMessage());
            throw new RuntimeException("Échec de la libération de l'IP flottante sur OpenStack: " + e.getMessage(), e);
        }
        repository.deleteById(id);
    }

    public List<ClientFloatingIp> getByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public List<ClientFloatingIp> getAll() {
        return repository.findAll();
    }

    public ClientFloatingIp getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("IP flottante introuvable: " + id));
    }
}
