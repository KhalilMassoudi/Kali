package kali.microservices.infrastructureservice.openstack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.RebootType;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.ServerUpdateOptions;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.storage.block.Volume;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@Slf4j
@Component("openstack")
@RequiredArgsConstructor
public class OpenStackProvider implements CloudProvider {

    private final OpenStackAuthService authService;

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public VpsProvisionResponse createVPS(String os, String ram, String region, String networkId, List<String> securityGroups,
                                           String keypairName, String serverGroupId) {
        OSClient.OSClientV3 client = authService.getClient();
        org.openstack4j.model.image.v2.Image image = getImageByName(client, os);
        return provisionServer(client, image, ram, networkId, securityGroups, keypairName, serverGroupId);
    }

    /**
     * Same provisioning path as {@link #createVPS}, but for an explicit Glance image id
     * instead of resolving one by OS name — used by the admin-curated image catalog (Phase 9)
     * and by redeploying from a personal snapshot/backup (Phase 6), neither of which has a
     * name that resolveImageName/getImageByName could match.
     */
    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public VpsProvisionResponse createVPSFromImage(String imageId, String ram, String region, String networkId, List<String> securityGroups,
                                                    String keypairName, String serverGroupId) {
        OSClient.OSClientV3 client = authService.getClient();
        org.openstack4j.model.image.v2.Image image = client.imagesV2().get(imageId);
        if (image == null) {
            throw new RuntimeException("Image OpenStack introuvable: " + imageId);
        }
        return provisionServer(client, image, ram, networkId, securityGroups, keypairName, serverGroupId);
    }

    private VpsProvisionResponse provisionServer(OSClient.OSClientV3 client, org.openstack4j.model.image.v2.Image image,
                                                   String ram, String networkId, List<String> securityGroups,
                                                   String keypairName, String serverGroupId) {
        String imageId   = image.getId();
        String flavorId  = getFlavorIdByRam(client, ram, image);
        String resolvedNetworkId = (networkId != null && !networkId.isBlank()) ? networkId : getDefaultNetworkId(client);
        String serverName = generateServerName();

        log.info("Provisioning OpenStack VM: name={}, image={}, flavor={}, network={}, keypair={}, serverGroup={}",
                serverName, imageId, flavorId, resolvedNetworkId, keypairName, serverGroupId);

        org.openstack4j.model.compute.builder.ServerCreateBuilder builder = Builders.server()
                .name(serverName)
                .image(imageId)
                .flavor(flavorId)
                .networks(Collections.singletonList(resolvedNetworkId));
        if (keypairName != null && !keypairName.isBlank()) {
            builder.keypairName(keypairName);
        }
        if (serverGroupId != null && !serverGroupId.isBlank()) {
            builder.addSchedulerHint("group", serverGroupId);
        }
        ServerCreate sc = builder.build();

        Server server = client.compute().servers().boot(sc);
        log.info("VM created: id={}, initial status={}", server.getId(), server.getStatus());

        // Wait up to 120s for ACTIVE, polling every 5s
        server = waitForServerStatus(client, server.getId(), Server.Status.ACTIVE, 120, 5);

        if (securityGroups != null && !securityGroups.isEmpty()) {
            for (String name : securityGroups) {
                try {
                    client.compute().servers().addSecurityGroup(server.getId(), name);
                } catch (Exception e) {
                    log.error("Failed to assign security group {} to server {}: {}", name, server.getId(), e.getMessage());
                }
            }
        }

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
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void startVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.START);
        log.info("Started OpenStack server: {}", externalId);
    }

    @Override
    public Map<String, Double> getDiagnostics(String externalId) {
        try {
            Map<String, ? extends Number> raw = authService.getClient().compute().servers().diagnostics(externalId);
            if (raw == null) return Collections.emptyMap();
            Map<String, Double> result = new HashMap<>();
            raw.forEach((k, v) -> result.put(k, v != null ? v.doubleValue() : null));
            return result;
        } catch (Exception e) {
            log.warn("Diagnostics unavailable for server {}: {}", externalId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public boolean ping() {
        return authService.ping();
    }

    /**
     * Every sub-call is isolated: one service being unreachable or forbidding a call (e.g.
     * Neutron's quota API, which some policies restrict to admins) degrades only that gauge
     * — used=0 / limit=-1 — instead of failing the whole card. No @Retryable here: the
     * caller caches the result, and retrying 3x with backoff only delayed the dashboard.
     */
    @Override
    public PlatformTotals getPlatformTotals() {
        OSClient.OSClientV3 client = authService.getClient();
        String projectId = client.getToken().getProject() != null ? client.getToken().getProject().getId() : null;
        // Filtered to our project because an admin-role token sees every project's resources;
        // resources with no owner field reported are kept rather than silently dropped.
        java.util.function.Predicate<String> ours = owner -> projectId == null || owner == null || projectId.equals(owner);

        var compute = safe("nova limits", () -> client.compute().quotaSets().limits().getAbsolute());
        var storage = safe("cinder limits", () -> client.blockStorage().getLimits().getAbsolute());
        // Nova's floating-IP/security-group limits are deprecated proxies — Horizon reads the
        // network side from Neutron, so we do too.
        var netQuota = projectId == null ? null : safe("neutron quotas", () -> client.networking().quotas().get(projectId));
        var net = client.networking();
        int networks = count("networks", () -> net.network().list().stream().filter(n -> ours.test(n.getTenantId())).count());
        int ports = count("ports", () -> net.port().list().stream().filter(p -> ours.test(p.getTenantId())).count());
        int routers = count("routers", () -> net.router().list().stream().filter(r -> ours.test(r.getTenantId())).count());
        int floatingIps = count("floating IPs", () -> net.floatingip().list().stream().filter(f -> ours.test(f.getTenantId())).count());
        int securityGroups = count("security groups", () -> net.securitygroup().list().stream().filter(g -> ours.test(g.getTenantId())).count());
        int securityGroupRules = count("security group rules", () -> net.securityrule().list().stream().filter(r -> ours.test(r.getTenantId())).count());
        int runningInstances = count("servers", () -> client.compute().servers().list().stream()
                .filter(s -> s.getStatus() == Server.Status.ACTIVE)
                .count());

        return new PlatformTotals(
                runningInstances,
                compute == null ? UNKNOWN : new PlatformTotals.Quota(compute.getTotalInstancesUsed(), compute.getMaxTotalInstances()),
                compute == null ? UNKNOWN : new PlatformTotals.Quota(compute.getTotalCoresUsed(), compute.getMaxTotalCores()),
                compute == null ? UNKNOWN : new PlatformTotals.Quota(compute.getTotalRAMUsed(), compute.getMaxTotalRAMSize()),
                storage == null ? UNKNOWN : new PlatformTotals.Quota(storage.getTotalVolumesUsed(), storage.getMaxTotalVolumes()),
                storage == null ? UNKNOWN : new PlatformTotals.Quota(storage.getTotalGigabytesUsed(), storage.getMaxTotalVolumeGigabytes()),
                storage == null ? UNKNOWN : new PlatformTotals.Quota(storage.getTotalSnapshotsUsed(), storage.getMaxTotalSnapshots()),
                new PlatformTotals.Quota(floatingIps, netQuota == null ? -1 : netQuota.getFloatingIP()),
                new PlatformTotals.Quota(securityGroups, netQuota == null ? -1 : netQuota.getSecurityGroup()),
                new PlatformTotals.Quota(securityGroupRules, netQuota == null ? -1 : netQuota.getSecurityGroupRule()),
                new PlatformTotals.Quota(networks, netQuota == null ? -1 : netQuota.getNetwork()),
                new PlatformTotals.Quota(ports, netQuota == null ? -1 : netQuota.getPort()),
                new PlatformTotals.Quota(routers, netQuota == null ? -1 : netQuota.getRouter()));
    }

    private static final PlatformTotals.Quota UNKNOWN = new PlatformTotals.Quota(0, -1);

    private <T> T safe(String what, java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (Exception e) {
            log.warn("Platform totals: could not read {}: {}", what, e.getMessage());
            return null;
        }
    }

    private int count(String what, java.util.function.Supplier<Long> call) {
        Long n = safe(what, call);
        return n == null ? 0 : n.intValue();
    }

    // ──────────────────────────── Volumes (Cinder) ─────────────────────────

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public VolumeDetails createVolume(String name, int sizeGb, String region) {
        OSClient.OSClientV3 client = authService.getClient();
        Volume created = client.blockStorage().volumes().create(
                Builders.volume().name(name).size(sizeGb).build());
        log.info("Volume created: id={}, name={}, size={}GB", created.getId(), created.getName(), sizeGb);
        return mapToVolumeDetails(created);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public VolumeDetails getVolume(String externalId) {
        Volume volume = authService.getClient().blockStorage().volumes().get(externalId);
        if (volume == null) {
            throw new RuntimeException("OpenStack volume not found: " + externalId);
        }
        return mapToVolumeDetails(volume);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteVolume(String externalId) {
        authService.getClient().blockStorage().volumes().delete(externalId);
        log.info("Deleted OpenStack volume: {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void attachVolume(String serverExternalId, String volumeExternalId, String device) {
        authService.getClient().compute().servers().attachVolume(serverExternalId, volumeExternalId, device);
        log.info("Attached volume {} to server {}", volumeExternalId, serverExternalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void detachVolume(String serverExternalId, String volumeExternalId) {
        authService.getClient().compute().servers().detachVolume(serverExternalId, volumeExternalId);
        log.info("Detached volume {} from server {}", volumeExternalId, serverExternalId);
    }

    // ──────────────────────────── Networking ───────────────────────────────

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<NetworkOption> listNetworks() {
        List<? extends Network> networks = authService.getClient().networking().network().list();
        return networks.stream()
                .map(n -> new NetworkOption(n.getId(), n.getName(), n.isRouterExternal()))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<SecurityGroupOption> listSecurityGroups() {
        List<? extends SecurityGroup> groups = authService.getClient().networking().securitygroup().list();
        return groups.stream()
                .map(g -> new SecurityGroupOption(g.getId(), g.getName(), g.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void assignSecurityGroup(String serverExternalId, String name) {
        authService.getClient().compute().servers().addSecurityGroup(serverExternalId, name);
        log.info("Assigned security group {} to server {}", name, serverExternalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void removeSecurityGroup(String serverExternalId, String name) {
        authService.getClient().compute().servers().removeSecurityGroup(serverExternalId, name);
        log.info("Removed security group {} from server {}", name, serverExternalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public CreatedNetwork createNetwork(String name, String cidr) {
        OSClient.OSClientV3 client = authService.getClient();
        Network network = client.networking().network()
                .create(org.openstack4j.openstack.networking.domain.NeutronNetwork.builder()
                        .name(name)
                        .adminStateUp(true)
                        .build());
        org.openstack4j.model.network.Subnet subnet = client.networking().subnet()
                .create(org.openstack4j.openstack.networking.domain.NeutronSubnet.builder()
                        .name(name + "-subnet")
                        .networkId(network.getId())
                        .ipVersion(org.openstack4j.model.network.IPVersionType.V4)
                        .cidr(cidr)
                        .enableDHCP(true)
                        .build());
        log.info("Created network {} ({}) with subnet {}", name, network.getId(), subnet.getId());
        return new CreatedNetwork(network.getId(), subnet.getId());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteNetwork(String networkId, String subnetId) {
        OSClient.OSClientV3 client = authService.getClient();
        if (subnetId != null) {
            client.networking().subnet().delete(subnetId);
        }
        client.networking().network().delete(networkId);
        log.info("Deleted network {} (subnet {})", networkId, subnetId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String createSecurityGroup(String name, String description) {
        SecurityGroup group = authService.getClient().networking().securitygroup()
                .create(org.openstack4j.openstack.networking.domain.NeutronSecurityGroup.builder()
                        .name(name)
                        .description(description)
                        .build());
        log.info("Created security group {} ({})", name, group.getId());
        return group.getId();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteSecurityGroup(String externalId) {
        authService.getClient().networking().securitygroup().delete(externalId);
        log.info("Deleted security group {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<SecurityGroupRuleOption> listSecurityGroupRules(String groupExternalId) {
        List<? extends org.openstack4j.model.network.SecurityGroupRule> rules = authService.getClient()
                .networking().securityrule().list(Map.of("security_group_id", groupExternalId));
        return rules.stream()
                .map(r -> new SecurityGroupRuleOption(r.getId(), r.getDirection(), r.getProtocol(),
                        r.getPortRangeMin(), r.getPortRangeMax(), r.getRemoteIpPrefix()))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public SecurityGroupRuleOption addSecurityGroupRule(String groupExternalId, String direction, String protocol,
                                                          Integer portMin, Integer portMax, String cidr) {
        org.openstack4j.model.network.SecurityGroupRule rule = authService.getClient().networking().securityrule()
                .create(org.openstack4j.openstack.networking.domain.NeutronSecurityGroupRule.builder()
                        .securityGroupId(groupExternalId)
                        .direction(direction)
                        .ethertype("IPv4")
                        .protocol(protocol)
                        .portRangeMin(portMin != null ? portMin : 0)
                        .portRangeMax(portMax != null ? portMax : 0)
                        .remoteIpPrefix(cidr)
                        .build());
        log.info("Added {} rule to security group {}: {} {}-{} from {}",
                direction, groupExternalId, protocol, portMin, portMax, cidr);
        return new SecurityGroupRuleOption(rule.getId(), direction, protocol, portMin, portMax, cidr);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void removeSecurityGroupRule(String ruleId) {
        authService.getClient().networking().securityrule().delete(ruleId);
        log.info("Removed security group rule {}", ruleId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<String> listFloatingIpPools() {
        List<String> pools = authService.getClient().compute().floatingIps().getPoolNames();
        return pools != null ? pools : new ArrayList<>();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String allocateFloatingIp(String pool) {
        FloatingIP ip = authService.getClient().compute().floatingIps().allocateIP(pool);
        log.info("Allocated floating IP {} from pool {}", ip.getFloatingIpAddress(), pool);
        return ip.getFloatingIpAddress();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void associateFloatingIp(String serverExternalId, String ip) {
        authService.getClient().compute().floatingIps().addFloatingIP(serverExternalId, ip);
        log.info("Associated floating IP {} to server {}", ip, serverExternalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void releaseFloatingIp(String ip) {
        authService.getClient().compute().floatingIps().deallocateIP(ip);
        log.info("Released floating IP {}", ip);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<FloatingIpDetails> listFloatingIps() {
        return authService.getClient().compute().floatingIps().list().stream()
                .map(ip -> new FloatingIpDetails(ip.getFloatingIpAddress(), ip.getPool(), ip.getInstanceId(), ip.getFixedIpAddress()))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void disassociateFloatingIp(String serverExternalId, String ip) {
        authService.getClient().compute().floatingIps().removeFloatingIP(serverExternalId, ip);
        log.info("Disassociated floating IP {} from server {}", ip, serverExternalId);
    }

    // ──────────────────── Resize / snapshot / rename / metadata ────────────

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<FlavorOption> listFlavors() {
        List<? extends Flavor> flavors = authService.getClient().compute().flavors().list();
        return flavors.stream()
                .map(f -> new FlavorOption(f.getId(), f.getName(), f.getRam(), f.getVcpus(), f.getDisk()))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void resizeVPS(String externalId, String flavorId) {
        authService.getClient().compute().servers().resize(externalId, flavorId);
        log.info("Resize requested for server {} -> flavor {}", externalId, flavorId);
        waitForServerStatus(authService.getClient(), externalId, Server.Status.VERIFY_RESIZE, 180, 5);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void confirmResizeVPS(String externalId) {
        authService.getClient().compute().servers().confirmResize(externalId);
        log.info("Resize confirmed for server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void revertResizeVPS(String externalId) {
        authService.getClient().compute().servers().revertResize(externalId);
        log.info("Resize reverted for server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String snapshotVPS(String externalId, String snapshotName) {
        String imageId = authService.getClient().compute().servers().createSnapshot(externalId, snapshotName);
        log.info("Snapshot {} created from server {} -> image {}", snapshotName, externalId, imageId);
        return imageId;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void renameVPS(String externalId, String newName) {
        authService.getClient().compute().servers().update(externalId, ServerUpdateOptions.create().name(newName));
        log.info("Renamed server {} -> {}", externalId, newName);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<SecurityGroupOption> getCurrentSecurityGroups(String externalId) {
        Server server = authService.getClient().compute().servers().get(externalId);
        if (server == null || server.getSecurityGroups() == null) {
            return Collections.emptyList();
        }
        return server.getSecurityGroups().stream()
                .map(g -> new SecurityGroupOption(g.getName(), g.getName(), null))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public Map<String, String> getMetadata(String externalId) {
        Map<String, String> metadata = authService.getClient().compute().servers().getMetadata(externalId);
        return metadata != null ? metadata : Collections.emptyMap();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public Map<String, String> updateMetadata(String externalId, Map<String, String> metadata) {
        return authService.getClient().compute().servers().updateMetadata(externalId, metadata);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteMetadataItem(String externalId, String key) {
        authService.getClient().compute().servers().deleteMetadataItem(externalId, key);
    }

    /**
     * Nova's real rescue response carries a generated adminPass in the JSON body, but
     * openstack4j's generic {@code action()} call only returns a bare success/fault
     * ActionResponse — no body access. Rather than reaching for a raw-REST workaround we
     * can't verify without a live deployment, we set a password we control right after
     * rescuing (a genuine, separate Nova action) so the admin gets a known credential either
     * way. Verify against the real deployment: Nova may need a moment to actually enter
     * RESCUE before changeAdminPassword succeeds — if so, this needs a short poll here.
     */
    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String rescueVPS(String externalId) {
        OSClient.OSClientV3 client = authService.getClient();
        client.compute().servers().action(externalId, Action.RESCUE);
        String tempPassword = "Rescue-" + java.util.UUID.randomUUID().toString().substring(0, 12);
        client.compute().servers().changeAdminPassword(externalId, tempPassword);
        log.info("Server {} entered rescue mode", externalId);
        return tempPassword;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void unrescueVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.UNRESCUE);
        log.info("Server {} exited rescue mode", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<ImageOption> listRawImages() {
        List<? extends org.openstack4j.model.image.v2.Image> images = authService.getClient().imagesV2().list();
        return images.stream()
                .map(i -> new ImageOption(i.getId(), i.getName(),
                        i.getMinDisk() != null ? i.getMinDisk().intValue() : null,
                        i.getMinRam() != null ? i.getMinRam().intValue() : null))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String importImageFromUrl(String name, String imageUrl, String diskFormat, Integer minDiskGb, Integer minRamMb) {
        OSClient.OSClientV3 client = authService.getClient();
        org.openstack4j.model.image.v2.DiskFormat format;
        try {
            format = org.openstack4j.model.image.v2.DiskFormat.value(diskFormat != null ? diskFormat : "qcow2");
        } catch (Exception e) {
            format = org.openstack4j.model.image.v2.DiskFormat.QCOW2;
        }

        org.openstack4j.model.image.v2.Image reserved = client.imagesV2().create(
                org.openstack4j.openstack.image.v2.domain.GlanceImage.builder()
                        .name(name)
                        .diskFormat(format)
                        .containerFormat(org.openstack4j.model.image.v2.ContainerFormat.BARE)
                        .minDisk(minDiskGb != null ? minDiskGb.longValue() : null)
                        .minRam(minRamMb != null ? minRamMb.longValue() : null)
                        .visibility(org.openstack4j.model.image.v2.Image.ImageVisibility.PRIVATE)
                        .build());

        try {
            client.imagesV2().upload(reserved.getId(),
                    org.openstack4j.model.common.Payloads.create(new java.net.URL(imageUrl)), reserved);
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException("URL d'image invalide: " + imageUrl, e);
        }

        log.info("Imported image {} ({}) from URL {}", name, reserved.getId(), imageUrl);
        return reserved.getId();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteImage(String externalId) {
        authService.getClient().imagesV2().delete(externalId);
        log.info("Deleted image {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<ImageOption> listImagesByPrefix(String prefix) {
        List<? extends org.openstack4j.model.image.v2.Image> images = authService.getClient().imagesV2().list();
        return images.stream()
                .filter(i -> i.getName() != null && i.getName().startsWith(prefix))
                .sorted(java.util.Comparator.comparing(org.openstack4j.model.image.v2.Image::getName).reversed())
                .map(i -> new ImageOption(
                        i.getId(),
                        i.getName(),
                        i.getMinDisk() != null ? i.getMinDisk().intValue() : null,
                        i.getMinRam() != null ? i.getMinRam().intValue() : null))
                .collect(Collectors.toList());
    }

    // ──────────────────────────── Helpers ─────────────────────────────────

    private org.openstack4j.model.image.v2.Image getImageByName(OSClient.OSClientV3 client, String osName) {
        String target = resolveImageName(osName);
        List<? extends org.openstack4j.model.image.v2.Image> images = client.imagesV2().list();
        return images.stream()
                .filter(img -> img.getName() != null && img.getName().toLowerCase().contains(target.toLowerCase()))
                .findFirst()
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

    private String getFlavorIdByRam(OSClient.OSClientV3 client, String ramStr, org.openstack4j.model.image.v2.Image image) {
        int requestedMb = parseRamToMb(ramStr);
        List<? extends Flavor> flavors = client.compute().flavors().list();
        if (flavors.isEmpty()) {
            throw new RuntimeException("No flavors available in OpenStack");
        }

        // Nova rejects a boot when the flavor's disk can't hold the image — must be at least
        // the image's declared min_disk (GB) AND its actual on-disk footprint, whichever is
        // larger. For a compressed format (qcow2...), that footprint is virtual_size (the
        // uncompressed size Nova writes to the flavor's disk), NOT size (the compressed file
        // size in Glance's store) — confirmed live: this deployment's ubuntu-22.04 image has
        // size=694923776 but virtual_size=2361393152, and Nova's boot failure reported the
        // virtual_size figure. Fall back to size only if virtual_size isn't reported at all.
        long minDiskGb = image.getMinDisk() != null ? image.getMinDisk() : 0L;
        long virtualSizeBytes = image.getVirtualSize() != null ? image.getVirtualSize() : 0L;
        long fileSizeBytes = image.getSize() != null ? image.getSize() : 0L;
        long minDiskBytes = minDiskGb * 1024L * 1024 * 1024;
        long imageDiskBytes = Math.max(virtualSizeBytes, fileSizeBytes);
        long requiredBytes = Math.max(minDiskBytes, imageDiskBytes);

        List<Flavor> bigEnough = flavors.stream()
                .filter(f -> (long) f.getDisk() * 1024 * 1024 * 1024 >= requiredBytes)
                .collect(Collectors.toList());
        if (bigEnough.isEmpty()) {
            throw new RuntimeException("No flavor has enough disk for image " + image.getName()
                    + " (needs >= " + requiredBytes + " bytes, largest flavor disk is "
                    + flavors.stream().mapToInt(Flavor::getDisk).max().orElse(0) + "GB)");
        }

        // Among flavors with enough disk, pick the one whose RAM is closest to requested
        Flavor best = bigEnough.stream()
                .min((a, b) -> Math.abs(a.getRam() - requestedMb) - Math.abs(b.getRam() - requestedMb))
                .orElseThrow();
        log.debug("Resolved flavor: {} ({}MB RAM, {}GB disk) for requested {}MB RAM, image needs >= {} bytes disk",
                best.getName(), best.getRam(), best.getDisk(), requestedMb, requiredBytes);
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
                String reason = server.getFault() != null
                        ? server.getFault().getMessage() + (server.getFault().getDetails() != null
                                ? " (" + server.getFault().getDetails() + ")" : "")
                        : "no fault detail returned by Nova";
                throw new RuntimeException("Server " + serverId + " entered ERROR state: " + reason);
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

    private VolumeDetails mapToVolumeDetails(Volume volume) {
        return VolumeDetails.builder()
                .externalId(volume.getId())
                .name(volume.getName())
                .status(volume.getStatus() != null ? volume.getStatus().name() : "UNKNOWN")
                .sizeGb(volume.getSize())
                .build();
    }

    private VpsDetails mapToDetails(Server server) {
        return VpsDetails.builder()
                .externalId(server.getId())
                .name(server.getName())
                .status(server.getStatus() != null ? server.getStatus().name() : "UNKNOWN")
                .ipAddress(extractPublicIp(server))
                .os(server.getImageId())
                .build();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public KeypairDetails createKeypair(String openstackName, String publicKeyOrNull) {
        org.openstack4j.model.compute.Keypair keypair = authService.getClient().compute().keypairs()
                .create(openstackName, publicKeyOrNull);
        log.info("Created keypair {} (server-generated: {})", openstackName, publicKeyOrNull == null);
        return new KeypairDetails(keypair.getName(), keypair.getPublicKey(), keypair.getPrivateKey(), keypair.getFingerprint());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteKeypair(String openstackName) {
        authService.getClient().compute().keypairs().delete(openstackName);
        log.info("Deleted keypair {}", openstackName);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<KeypairDetails> listKeypairs() {
        return authService.getClient().compute().keypairs().list().stream()
                .map(k -> new KeypairDetails(k.getName(), k.getPublicKey(), null, k.getFingerprint()))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public ServerGroupDetails createServerGroup(String name, String policy) {
        org.openstack4j.model.compute.ServerGroup group = authService.getClient().compute().serverGroups()
                .create(name, policy);
        log.info("Created server group {} ({}), policy={}", name, group.getId(), policy);
        return new ServerGroupDetails(group.getId(), group.getName(), group.getPolicies(), group.getMembers());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteServerGroup(String externalId) {
        authService.getClient().compute().serverGroups().delete(externalId);
        log.info("Deleted server group {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<ServerGroupDetails> listServerGroups() {
        return authService.getClient().compute().serverGroups().list().stream()
                .map(g -> new ServerGroupDetails(g.getId(), g.getName(), g.getPolicies(), g.getMembers()))
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public ServerGroupDetails getServerGroup(String externalId) {
        org.openstack4j.model.compute.ServerGroup group = authService.getClient().compute().serverGroups().get(externalId);
        if (group == null) {
            throw new RuntimeException("Server group OpenStack introuvable: " + externalId);
        }
        return new ServerGroupDetails(group.getId(), group.getName(), group.getPolicies(), group.getMembers());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public SnapshotDetails createVolumeSnapshot(String volumeExternalId, String name, String description, boolean force) {
        org.openstack4j.model.storage.block.VolumeSnapshot snapshot = authService.getClient().blockStorage().snapshots()
                .create(Builders.volumeSnapshot()
                        .volume(volumeExternalId)
                        .name(name)
                        .description(description)
                        .force(force)
                        .build());
        log.info("Created volume snapshot {} ({}) from volume {}", name, snapshot.getId(), volumeExternalId);
        return mapToSnapshotDetails(snapshot);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteVolumeSnapshot(String externalId) {
        authService.getClient().blockStorage().snapshots().delete(externalId);
        log.info("Deleted volume snapshot {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<SnapshotDetails> listVolumeSnapshots() {
        return authService.getClient().blockStorage().snapshots().list().stream()
                .map(this::mapToSnapshotDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public SnapshotDetails getVolumeSnapshot(String externalId) {
        org.openstack4j.model.storage.block.VolumeSnapshot snapshot = authService.getClient().blockStorage().snapshots().get(externalId);
        if (snapshot == null) {
            throw new RuntimeException("Snapshot OpenStack introuvable: " + externalId);
        }
        return mapToSnapshotDetails(snapshot);
    }

    private SnapshotDetails mapToSnapshotDetails(org.openstack4j.model.storage.block.VolumeSnapshot snapshot) {
        return new SnapshotDetails(
                snapshot.getId(),
                snapshot.getName(),
                snapshot.getVolumeId(),
                snapshot.getStatus() != null ? snapshot.getStatus().name() : "UNKNOWN",
                snapshot.getSize()
        );
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public RouterDetails createRouter(String name, String externalNetworkIdOrNull) {
        org.openstack4j.model.network.builder.RouterBuilder builder =
                org.openstack4j.openstack.networking.domain.NeutronRouter.builder()
                        .name(name)
                        .adminStateUp(true);
        if (externalNetworkIdOrNull != null && !externalNetworkIdOrNull.isBlank()) {
            builder.externalGateway(externalNetworkIdOrNull);
        }
        org.openstack4j.model.network.Router router = authService.getClient().networking().router().create(builder.build());
        log.info("Created router {} ({})", name, router.getId());
        return mapToRouterDetails(router);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deleteRouter(String externalId) {
        authService.getClient().networking().router().delete(externalId);
        log.info("Deleted router {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<RouterDetails> listRouters() {
        return authService.getClient().networking().router().list().stream()
                .map(this::mapToRouterDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public RouterDetails getRouter(String externalId) {
        org.openstack4j.model.network.Router router = authService.getClient().networking().router().get(externalId);
        if (router == null) {
            throw new RuntimeException("Routeur OpenStack introuvable: " + externalId);
        }
        return mapToRouterDetails(router);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void setRouterGateway(String routerId, String externalNetworkId) {
        org.openstack4j.model.network.Router current = authService.getClient().networking().router().get(routerId);
        org.openstack4j.model.network.Router updated = current.toBuilder().externalGateway(externalNetworkId).build();
        authService.getClient().networking().router().update(updated);
        log.info("Set gateway of router {} to network {}", routerId, externalNetworkId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void clearRouterGateway(String routerId) {
        org.openstack4j.model.network.Router current = authService.getClient().networking().router().get(routerId);
        org.openstack4j.model.network.Router updated = current.toBuilder().clearExternalGateway().build();
        authService.getClient().networking().router().update(updated);
        log.info("Cleared gateway of router {}", routerId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void attachRouterInterface(String routerId, String subnetId) {
        authService.getClient().networking().router()
                .attachInterface(routerId, org.openstack4j.model.network.AttachInterfaceType.SUBNET, subnetId);
        log.info("Attached subnet {} to router {}", subnetId, routerId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void detachRouterInterface(String routerId, String subnetId) {
        authService.getClient().networking().router().detachInterface(routerId, subnetId, null);
        log.info("Detached subnet {} from router {}", subnetId, routerId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<RouterInterfaceDetails> listRouterInterfaces(String routerId) {
        return authService.getClient().networking().port().list().stream()
                .filter(port -> routerId.equals(port.getDeviceId()))
                .map(port -> new RouterInterfaceDetails(
                        port.getId(),
                        port.getFixedIps().isEmpty() ? null : port.getFixedIps().iterator().next().getSubnetId(),
                        port.getNetworkId()
                ))
                .collect(Collectors.toList());
    }

    private RouterDetails mapToRouterDetails(org.openstack4j.model.network.Router router) {
        String gatewayNetworkId = router.getExternalGatewayInfo() != null
                ? router.getExternalGatewayInfo().getNetworkId()
                : null;
        return new RouterDetails(router.getId(), router.getName(), gatewayNetworkId, router.isAdminStateUp());
    }

    // ──────────────────── Additional VM lifecycle actions ────────────────────

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void pauseVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.PAUSE);
        log.info("Paused server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void unpauseVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.UNPAUSE);
        log.info("Unpaused server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void suspendVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.SUSPEND);
        log.info("Suspended server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void resumeVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.RESUME);
        log.info("Resumed server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void shelveVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.SHELVE);
        log.info("Shelved server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void unshelveVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.UNSHELVE);
        log.info("Unshelved server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void lockVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.LOCK);
        log.info("Locked server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void unlockVPS(String externalId) {
        authService.getClient().compute().servers().action(externalId, Action.UNLOCK);
        log.info("Unlocked server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void softRebootVPS(String externalId) {
        authService.getClient().compute().servers().reboot(externalId, RebootType.SOFT);
        log.info("Soft-rebooted server {}", externalId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void rebuildVPS(String externalId, String imageId) {
        authService.getClient().compute().servers()
                .rebuild(externalId, org.openstack4j.model.compute.actions.RebuildOptions.create().image(imageId));
        log.info("Rebuilding server {} from image {}", externalId, imageId);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String getConsoleUrl(String externalId) {
        org.openstack4j.model.compute.VNCConsole console = authService.getClient().compute().servers()
                .getVNCConsole(externalId, org.openstack4j.model.compute.VNCConsole.Type.NOVNC);
        return console != null ? console.getURL() : null;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String getConsoleLog(String externalId, int numLines) {
        return authService.getClient().compute().servers().getConsoleOutput(externalId, numLines);
    }

    // ──────────────────── Network interfaces ────────────────────

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<InterfaceDetails> listInterfaces(String externalId) {
        return authService.getClient().compute().servers().interfaces().list(externalId).stream()
                .map(this::mapToInterfaceDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public InterfaceDetails attachInterface(String externalId, String networkId) {
        org.openstack4j.model.compute.InterfaceAttachment attachment =
                authService.getClient().compute().servers().interfaces().create(externalId, networkId);
        log.info("Attached interface on network {} to server {}", networkId, externalId);
        return mapToInterfaceDetails(attachment);
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void detachInterface(String externalId, String attachmentId) {
        authService.getClient().compute().servers().interfaces().detach(externalId, attachmentId);
        log.info("Detached interface {} from server {}", attachmentId, externalId);
    }

    private InterfaceDetails mapToInterfaceDetails(org.openstack4j.model.compute.InterfaceAttachment attachment) {
        String fixedIp = (attachment.getFixedIps() != null && !attachment.getFixedIps().isEmpty())
                ? attachment.getFixedIps().get(0).getIpAddress()
                : null;
        return new InterfaceDetails(attachment.getPortId(), attachment.getPortId(), attachment.getNetId(),
                attachment.getMacAddr(), fixedIp);
    }

    // ──────────────────── Port-level security groups ────────────────────

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<String> getPortSecurityGroups(String portId) {
        org.openstack4j.model.network.Port port = authService.getClient().networking().port().get(portId);
        if (port == null) {
            throw new RuntimeException("Port OpenStack introuvable: " + portId);
        }
        List<String> groups = port.getSecurityGroups();
        return groups != null ? groups : new ArrayList<>();
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void updatePortSecurityGroups(String portId, List<String> groupIds) {
        // Built fresh (not via toBuilder()) so only id + securityGroups are populated —
        // PortBuilder.securityGroup() only ever adds, it has no "clear" method, and
        // toBuilder() would seed the port's EXISTING groups first, turning this into an
        // append instead of a replace. Every other field stays null/absent (Jackson is
        // globally NON_NULL) so Neutron's PUT only touches security_groups.
        org.openstack4j.model.network.builder.PortBuilder builder =
                org.openstack4j.openstack.networking.domain.NeutronPort.builder();
        for (String groupId : groupIds) {
            builder.securityGroup(groupId);
        }
        org.openstack4j.openstack.networking.domain.NeutronPort port =
                (org.openstack4j.openstack.networking.domain.NeutronPort) builder.build();
        port.setId(portId);
        authService.getClient().networking().port().update(port);
        log.info("Updated security groups on port {}: {}", portId, groupIds);
    }
}
