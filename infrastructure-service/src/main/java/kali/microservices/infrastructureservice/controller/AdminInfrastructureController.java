package kali.microservices.infrastructureservice.controller;

import kali.microservices.infrastructureservice.dto.AdminClientSummary;
import kali.microservices.infrastructureservice.entities.ClientFloatingIp;
import kali.microservices.infrastructureservice.entities.ClientKeypair;
import kali.microservices.infrastructureservice.entities.ClientProject;
import kali.microservices.infrastructureservice.entities.ClientRouter;
import kali.microservices.infrastructureservice.entities.ClientServerGroup;
import kali.microservices.infrastructureservice.entities.Domain;
import kali.microservices.infrastructureservice.entities.K8sCluster;
import kali.microservices.infrastructureservice.entities.VpsNetwork;
import kali.microservices.infrastructureservice.entities.VpsSecurityGroup;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.entities.VpsVolume;
import kali.microservices.infrastructureservice.entities.VpsVolumeSnapshot;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.ClientProjectService;
import kali.microservices.infrastructureservice.service.DomainService;
import kali.microservices.infrastructureservice.service.FloatingIpManagementService;
import kali.microservices.infrastructureservice.service.RouterManagementService;
import kali.microservices.infrastructureservice.service.K8sClusterService;
import kali.microservices.infrastructureservice.service.KeypairManagementService;
import kali.microservices.infrastructureservice.service.NetworkService;
import kali.microservices.infrastructureservice.service.SecurityGroupManagementService;
import kali.microservices.infrastructureservice.service.ServerGroupManagementService;
import kali.microservices.infrastructureservice.service.VolumeService;
import kali.microservices.infrastructureservice.service.VolumeSnapshotService;
import kali.microservices.infrastructureservice.service.VpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only, fleet-wide views across every client's resources — everything else in
 * infrastructure-service is scoped per-user via {@link AuthContext#requireOwnerOrAdmin}.
 */
@RestController
@RequestMapping("/api/infrastructure/admin")
@RequiredArgsConstructor
public class AdminInfrastructureController {

    private final VpsService vpsService;
    private final VolumeService volumeService;
    private final K8sClusterService k8sClusterService;
    private final DomainService domainService;
    private final NetworkService networkService;
    private final SecurityGroupManagementService securityGroupManagementService;
    private final ClientProjectService clientProjectService;
    private final KeypairManagementService keypairManagementService;
    private final ServerGroupManagementService serverGroupManagementService;
    private final VolumeSnapshotService volumeSnapshotService;
    private final FloatingIpManagementService floatingIpManagementService;
    private final RouterManagementService routerManagementService;
    private final AuthContext authContext;

    @GetMapping("/vps")
    public ResponseEntity<List<VpsServer>> getAllVps(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(vpsService.getAllVps());
    }

    @GetMapping("/volumes")
    public ResponseEntity<List<VpsVolume>> getAllVolumes(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(volumeService.getAllVolumes());
    }

    @GetMapping("/clusters")
    public ResponseEntity<List<K8sCluster>> getAllClusters(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(k8sClusterService.getAllClusters());
    }

    @GetMapping("/domains")
    public ResponseEntity<List<Domain>> getAllDomains(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(domainService.getAllDomains());
    }

    @GetMapping("/networks")
    public ResponseEntity<List<VpsNetwork>> getAllNetworks(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(networkService.getAllNetworks());
    }

    @GetMapping("/security-groups")
    public ResponseEntity<List<VpsSecurityGroup>> getAllSecurityGroups(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(securityGroupManagementService.getAll());
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ClientProject>> getAllProjects(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(clientProjectService.getAllProjects());
    }

    @GetMapping("/clients")
    public ResponseEntity<List<AdminClientSummary>> getClientSummaries(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(clientProjectService.getAdminClientSummaries());
    }

    @GetMapping("/keypairs")
    public ResponseEntity<List<ClientKeypair>> getAllKeypairs(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(keypairManagementService.getAll());
    }

    @GetMapping("/server-groups")
    public ResponseEntity<List<ClientServerGroup>> getAllServerGroups(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(serverGroupManagementService.getAll());
    }

    @GetMapping("/volume-snapshots")
    public ResponseEntity<List<VpsVolumeSnapshot>> getAllVolumeSnapshots(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(volumeSnapshotService.getAll());
    }

    @GetMapping("/floating-ips")
    public ResponseEntity<List<ClientFloatingIp>> getAllFloatingIps(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(floatingIpManagementService.getAll());
    }

    @GetMapping("/routers")
    public ResponseEntity<List<ClientRouter>> getAllRouters(@RequestHeader("Authorization") String authHeader) {
        authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(routerManagementService.getAll());
    }
}
