package kali.microservices.infrastructureservice.openstack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.RebootType;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.network.Network;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component("openstack")
@RequiredArgsConstructor
public class OpenStackProvider implements CloudProvider {

    private final OpenStackAuthService authService;

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public VpsProvisionResponse createVPS(String os, String ram, String region) {
        OSClient.OSClientV3 client = authService.getClient();

        String imageId   = getImageIdByName(client, os);
        String flavorId  = getFlavorIdByRam(client, ram);
        String networkId = getDefaultNetworkId(client);
        String serverName = generateServerName();

        log.info("Provisioning OpenStack VM: name={}, image={}, flavor={}, network={}", serverName, imageId, flavorId, networkId);

        ServerCreate sc = Builders.server()
                .name(serverName)
                .image(imageId)
                .flavor(flavorId)
                .network(networkId)
                .build();

        Server server = client.compute().servers().boot(sc);
        log.info("VM created: id={}, initial status={}", server.getId(), server.getStatus());

        // Wait up to 120s for ACTIVE, polling every 5s
        server = waitForServerStatus(client, server.getId(), Server.Status.ACTIVE, 120, 5);

        String ip = extractPublicIp(server);
        log.info("VM is ACTIVE: id={}, ip={}", server.getId(), ip);

        return VpsProvisionResponse.builder()
                .externalId(server.getId())
                .name(server.getName())
                .ipAddress(ip)
                .status(server.getStatus().name())
                .build();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<VpsDetails> listVPS(String userId) {
        OSClient.OSClientV3 client = authService.getClient();
        List<? extends Server> servers = client.compute().servers().list();
        return servers.stream()
                .map(this::mapToDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public VpsDetails getVPS(String externalId) {
        OSClient.OSClientV3 client = authService.getClient();
        Server server = client.compute().servers().get(externalId);
        if (server == null) {
            throw new RuntimeException("OpenStack server not found: " + externalId);
        }
        return mapToDetails(server);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteVPS(String externalId) {
        authService.getClient().compute().servers().delete(externalId);
        log.info("Deleted OpenStack server: {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void restartVPS(String externalId) {
        authService.getClient().compute().servers().reboot(externalId, RebootType.HARD);
        log.info("Rebooted OpenStack server: {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void stopVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.STOP);
        log.info("Stopped OpenStack server: {}", externalId);
    }

    @Override
    public boolean ping() {
        return authService.ping();
    }

    // ──────────────────────────── Helpers ─────────────────────────────────

    private String getImageIdByName(OSClient.OSClientV3 client, String osName) {
        String target = resolveImageName(osName);
        List<? extends org.openstack4j.model.compute.Image> images = client.compute().images().list();
        return images.stream()
                .filter(img -> img.getName() != null && img.getName().toLowerCase().contains(target.toLowerCase()))
                .findFirst()
                .map(org.openstack4j.model.compute.Image::getId)
                .orElseThrow(() -> new RuntimeException("No image found matching: " + target));
    }

    private String resolveImageName(String os) {
        if (os == null) return "ubuntu";
        String lower = os.toLowerCase();
        if (lower.contains("ubuntu")) return "ubuntu-22.04";
        if (lower.contains("cirros")) return "cirros";
        if (lower.contains("acura")) return "Acura_Cntr_4_3_1";
        return os; // use as-is
    }

    private String getFlavorIdByRam(OSClient.OSClientV3 client, String ramStr) {
        int requestedMb = parseRamToMb(ramStr);
        List<? extends Flavor> flavors = client.compute().flavors().list();
        if (flavors.isEmpty()) {
            throw new RuntimeException("No flavors available in OpenStack");
        }
        // Pick the flavor whose RAM is closest to the requested amount
        Flavor best = flavors.stream()
                .min((a, b) -> Math.abs(a.getRam() - requestedMb) - Math.abs(b.getRam() - requestedMb))
                .orElseThrow();
        log.debug("Resolved flavor: {} ({}MB RAM) for requested {}MB", best.getName(), best.getRam(), requestedMb);
        return best.getId();
    }

    private int parseRamToMb(String ram) {
        if (ram == null) return 2048;
        try {
            int val = Integer.parseInt(ram.trim());
            // If value < 100, assume it's in GB (e.g. "2" means 2 GB)
            return val < 100 ? val * 1024 : val;
        } catch (NumberFormatException e) {
            return 2048;
        }
    }

    private String getDefaultNetworkId(OSClient.OSClientV3 client) {
        List<? extends Network> networks = client.networking().network().list();
        // Prefer private-net, fall back to first available
        return networks.stream()
                .filter(n -> "private-net".equalsIgnoreCase(n.getName()))
                .findFirst()
                .map(Network::getId)
                .orElseGet(() -> networks.isEmpty() ? null : networks.get(0).getId());
    }

    private String generateServerName() {
        return "safozi-vm-" + System.currentTimeMillis();
    }

    private Server waitForServerStatus(OSClient.OSClientV3 client, String serverId,
                                        Server.Status target, int timeoutSeconds, int pollIntervalSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            Server server = client.compute().servers().get(serverId);
            if (server == null) {
                throw new RuntimeException("Server " + serverId + " disappeared while waiting");
            }
            Server.Status current = server.getStatus();
            log.debug("Polling server {}: status={}", serverId, current);
            if (current == target) {
                return server;
            }
            if (current == Server.Status.ERROR) {
                throw new RuntimeException("Server " + serverId + " entered ERROR state");
            }
            try {
                Thread.sleep(pollIntervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for server " + serverId);
            }
        }
        throw new RuntimeException("Timeout: server " + serverId + " did not reach " + target + " within " + timeoutSeconds + "s");
    }

    private String extractPublicIp(Server server) {
        if (server.getAddresses() == null) return null;
        Map<String, List<? extends Address>> addressMap = server.getAddresses().getAddresses();
        // First pass: look for floating IP
        for (List<? extends Address> addrs : addressMap.values()) {
            for (Address addr : addrs) {
                if ("floating".equals(addr.getType())) {
                    return addr.getAddr();
                }
            }
        }
        // Second pass: return any fixed IP
        for (List<? extends Address> addrs : addressMap.values()) {
            if (!addrs.isEmpty()) {
                return addrs.get(0).getAddr();
            }
        }
        return null;
    }

    private Integer extractRam(OSClient.OSClientV3 client, Server server) {
        try {
            String flavorId = server.getFlavor().getId();
            Flavor flavor = client.compute().flavors().get(flavorId);
            return flavor != null ? flavor.getRam() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private VpsDetails mapToDetails(Server server) {
        return VpsDetails.builder()
                .externalId(server.getId())
                .name(server.getName())
                .status(server.getStatus() != null ? server.getStatus().name() : "UNKNOWN")
                .ipAddress(extractPublicIp(server))
                .os(server.getImageRef())
                .build();
    }
}
