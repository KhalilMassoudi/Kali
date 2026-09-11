package kali.microservices.infrastructureservice.openstack;

import java.util.List;
import java.util.Map;

public interface CloudProvider {
    VpsProvisionResponse createVPS(String os, String ram, String region, String networkId, List<String> securityGroups,
                                    String keypairName, String serverGroupId);
    VpsProvisionResponse createVPSFromImage(String imageId, String ram, String region, String networkId, List<String> securityGroups,
                                             String keypairName, String serverGroupId);
    List<VpsDetails> listVPS(String userId);
    VpsDetails getVPS(String externalId);
    void deleteVPS(String externalId);
    void restartVPS(String externalId);
    void stopVPS(String externalId);
    void startVPS(String externalId);
    Map<String, Double> getDiagnostics(String externalId);
    boolean ping();

    // Volumes (Cinder)
    VolumeDetails createVolume(String name, int sizeGb, String region);
    VolumeDetails getVolume(String externalId);
    void deleteVolume(String externalId);
    void attachVolume(String serverExternalId, String volumeExternalId, String device);
    void detachVolume(String serverExternalId, String volumeExternalId);

    // Networking
    List<NetworkOption> listNetworks();
    List<SecurityGroupOption> listSecurityGroups();
    void assignSecurityGroup(String serverExternalId, String name);
    void removeSecurityGroup(String serverExternalId, String name);
    List<String> listFloatingIpPools();
    String allocateFloatingIp(String pool);
    void associateFloatingIp(String serverExternalId, String ip);
    void releaseFloatingIp(String ip);
    List<FloatingIpDetails> listFloatingIps();
    void disassociateFloatingIp(String serverExternalId, String ip);

    // Client-owned networks (Neutron)
    CreatedNetwork createNetwork(String name, String cidr);
    void deleteNetwork(String networkId, String subnetId);

    // Client-owned security groups + rules (Neutron)
    String createSecurityGroup(String name, String description);
    void deleteSecurityGroup(String externalId);
    List<SecurityGroupRuleOption> listSecurityGroupRules(String groupExternalId);
    SecurityGroupRuleOption addSecurityGroupRule(String groupExternalId, String direction, String protocol,
                                                  Integer portMin, Integer portMax, String cidr);
    void removeSecurityGroupRule(String ruleId);

    // Resize / snapshot / rename / metadata
    List<FlavorOption> listFlavors();
    void resizeVPS(String externalId, String flavorId);
    void confirmResizeVPS(String externalId);
    void revertResizeVPS(String externalId);
    String snapshotVPS(String externalId, String snapshotName);
    void renameVPS(String externalId, String newName);
    List<SecurityGroupOption> getCurrentSecurityGroups(String externalId);
    Map<String, String> getMetadata(String externalId);
    Map<String, String> updateMetadata(String externalId, Map<String, String> metadata);
    void deleteMetadataItem(String externalId, String key);

    // Rescue mode (admin-only)
    String rescueVPS(String externalId);
    void unrescueVPS(String externalId);

    // Admin-managed OS image catalog (Glance)
    List<ImageOption> listRawImages();
    String importImageFromUrl(String name, String imageUrl, String diskFormat, Integer minDiskGb, Integer minRamMb);
    void deleteImage(String externalId);
    List<ImageOption> listImagesByPrefix(String prefix);

    // Key pairs (Nova) — publicKey null triggers server-side generation; the returned
    // KeypairDetails.privateKey is populated ONLY on that create call and must never be persisted.
    KeypairDetails createKeypair(String openstackName, String publicKeyOrNull);
    void deleteKeypair(String openstackName);
    List<KeypairDetails> listKeypairs();

    // Server groups (Nova) — policy is one of affinity/anti-affinity/soft-affinity/soft-anti-affinity
    ServerGroupDetails createServerGroup(String name, String policy);
    void deleteServerGroup(String externalId);
    List<ServerGroupDetails> listServerGroups();
    ServerGroupDetails getServerGroup(String externalId);

    // Volume snapshots (Cinder) — distinct from the VM-backup feature (Glance server image)
    SnapshotDetails createVolumeSnapshot(String volumeExternalId, String name, String description, boolean force);
    void deleteVolumeSnapshot(String externalId);
    List<SnapshotDetails> listVolumeSnapshots();
    SnapshotDetails getVolumeSnapshot(String externalId);

    // Routers (Neutron)
    RouterDetails createRouter(String name, String externalNetworkIdOrNull);
    void deleteRouter(String externalId);
    List<RouterDetails> listRouters();
    RouterDetails getRouter(String externalId);
    void setRouterGateway(String routerId, String externalNetworkId);
    void clearRouterGateway(String routerId);
    void attachRouterInterface(String routerId, String subnetId);
    void detachRouterInterface(String routerId, String subnetId);
    List<RouterInterfaceDetails> listRouterInterfaces(String routerId);

    // Additional VM lifecycle actions (Nova)
    void pauseVPS(String externalId);
    void unpauseVPS(String externalId);
    void suspendVPS(String externalId);
    void resumeVPS(String externalId);
    void shelveVPS(String externalId);
    void unshelveVPS(String externalId);
    void lockVPS(String externalId);
    void unlockVPS(String externalId);
    void softRebootVPS(String externalId);
    void rebuildVPS(String externalId, String imageId);
    String getConsoleUrl(String externalId);
    String getConsoleLog(String externalId, int numLines);

    // Network interfaces (Nova)
    List<InterfaceDetails> listInterfaces(String externalId);
    InterfaceDetails attachInterface(String externalId, String networkId);
    void detachInterface(String externalId, String attachmentId);

    // Port-level security groups (Neutron)
    List<String> getPortSecurityGroups(String portId);
    void updatePortSecurityGroups(String portId, List<String> groupIds);
}
