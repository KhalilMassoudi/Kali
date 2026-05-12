package kali.microservices.infrastructureservice.service;

import kali.microservices.infrastructureservice.dto.CreateVpsRequest;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.repository.VpsServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VpsService {

    private final VpsServerRepository vpsServerRepository;

    public VpsServer createVps(CreateVpsRequest request) {
        VpsServer vps = new VpsServer();
        vps.setUserId(request.getUserId());
        vps.setName(request.getName());
        vps.setOs(request.getOs());
        vps.setRam(request.getRam());
        vps.setCpu(request.getCpu());
        vps.setStorage(request.getStorage());
        vps.setRegion(request.getRegion() != null ? request.getRegion() : "eu-west-1");
        vps.setIpAddress(generateMockIp());
        vps.setStatus(VpsServer.VpsStatus.RUNNING);

        return vpsServerRepository.save(vps);
    }

    public List<VpsServer> getVpsByUser(Long userId) {
        return vpsServerRepository.findByUserId(userId);
    }

    public VpsServer getVpsById(Long id) {
        return vpsServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VPS non trouvé: " + id));
    }

    public VpsServer updateVpsStatus(Long id, VpsServer.VpsStatus status) {
        VpsServer vps = getVpsById(id);
        vps.setStatus(status);
        return vpsServerRepository.save(vps);
    }

    public void deleteVps(Long id) {
        VpsServer vps = getVpsById(id);
        vps.setStatus(VpsServer.VpsStatus.DELETED);
        vpsServerRepository.save(vps);
    }

    private String generateMockIp() {
        return "10." + (int)(Math.random() * 255) + "." + (int)(Math.random() * 255) + "." + (int)(Math.random() * 255);
    }
}