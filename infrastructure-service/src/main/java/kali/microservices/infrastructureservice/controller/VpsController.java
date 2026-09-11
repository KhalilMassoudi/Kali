package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.BackupScheduleRequest;
import kali.microservices.infrastructureservice.dto.CreateVpsRequest;
import kali.microservices.infrastructureservice.dto.PageResponse;
import kali.microservices.infrastructureservice.dto.VmHealthResponse;
import kali.microservices.infrastructureservice.entities.VpsActivityLog;
import kali.microservices.infrastructureservice.entities.VpsBackupSchedule;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.openstack.ImageOption;
import kali.microservices.infrastructureservice.openstack.SecurityGroupOption;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.security.AuthenticatedUser;
import kali.microservices.infrastructureservice.service.BackupScheduleService;
import kali.microservices.infrastructureservice.service.VpsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/infrastructure/vps")
@RequiredArgsConstructor
public class VpsController {

    private final VpsService vpsService;
    private final AuthContext authContext;
    private final BackupScheduleService backupScheduleService;

    @PostMapping
    public ResponseEntity<VpsServer> createVps(@RequestHeader("Authorization") String authHeader,
                                                @Valid @RequestBody CreateVpsRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vpsService.createVps(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VpsServer>> getVpsByUser(@RequestHeader("Authorization") String authHeader,
                                                          @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(vpsService.getVpsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VpsServer> getVpsById(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable Long id) {
        VpsServer vps = vpsService.getVpsById(id);
        authContext.requireOwnerOrAdmin(authHeader, vps.getUserId());
        return ResponseEntity.ok(vps);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VpsServer> updateStatus(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable Long id,
                                                   @RequestParam VpsServer.VpsStatus status) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.updateVpsStatus(id, status));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<VpsServer> stopVps(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.stopVps(id, user.userId()));
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<VpsServer> restartVps(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.restartVps(id, user.userId()));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<VpsServer> startVps(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.startVps(id, user.userId()));
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<VpsServer> refreshStatus(@RequestHeader("Authorization") String authHeader,
                                                    @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.refreshStatus(id));
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<VmHealthResponse> getHealth(@RequestHeader("Authorization") String authHeader,
                                                        @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.getHealth(id));
    }

    @PostMapping("/{id}/floating-ip")
    public ResponseEntity<VpsServer> allocateFloatingIp(@RequestHeader("Authorization") String authHeader,
                                                          @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.allocateAndAssociateFloatingIp(id, user.userId()));
    }

    @DeleteMapping("/{id}/floating-ip")
    public ResponseEntity<VpsServer> releaseFloatingIp(@RequestHeader("Authorization") String authHeader,
                                                         @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.releaseFloatingIp(id, user.userId()));
    }

    @PostMapping("/{id}/security-groups")
    public ResponseEntity<Void> assignSecurityGroup(@RequestHeader("Authorization") String authHeader,
                                                      @PathVariable Long id, @RequestParam String name) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        vpsService.assignSecurityGroup(id, name, user.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/security-groups/{name}")
    public ResponseEntity<Void> removeSecurityGroup(@RequestHeader("Authorization") String authHeader,
                                                      @PathVariable Long id, @PathVariable String name) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        vpsService.removeSecurityGroup(id, name, user.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVps(@RequestHeader("Authorization") String authHeader,
                                           @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        vpsService.deleteVps(id, user.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<VpsServer> renameVps(@RequestHeader("Authorization") String authHeader,
                                                @PathVariable Long id, @RequestParam String name) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.renameVps(id, name, user.userId()));
    }

    @PostMapping("/{id}/resize")
    public ResponseEntity<VpsServer> resizeVps(@RequestHeader("Authorization") String authHeader,
                                                @PathVariable Long id, @RequestParam String flavorId) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.resizeVps(id, flavorId, user.userId()));
    }

    @PostMapping("/{id}/resize/confirm")
    public ResponseEntity<VpsServer> confirmResize(@RequestHeader("Authorization") String authHeader,
                                                     @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.confirmResize(id, user.userId()));
    }

    @PostMapping("/{id}/resize/revert")
    public ResponseEntity<VpsServer> revertResize(@RequestHeader("Authorization") String authHeader,
                                                    @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.revertResize(id, user.userId()));
    }

    @PostMapping("/{id}/snapshot")
    public ResponseEntity<Map<String, String>> snapshotVps(@RequestHeader("Authorization") String authHeader,
                                                             @PathVariable Long id, @RequestParam String name) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(Map.of("imageId", vpsService.snapshotVps(id, name, user.userId())));
    }

    @GetMapping("/{id}/security-groups")
    public ResponseEntity<List<SecurityGroupOption>> getCurrentSecurityGroups(
            @RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.getCurrentSecurityGroups(id));
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<Map<String, String>> getMetadata(@RequestHeader("Authorization") String authHeader,
                                                             @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.getMetadata(id));
    }

    @PutMapping("/{id}/metadata")
    public ResponseEntity<Map<String, String>> updateMetadata(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long id,
                                                                @RequestBody Map<String, String> metadata) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.updateMetadata(id, metadata, user.userId()));
    }

    @DeleteMapping("/{id}/metadata/{key}")
    public ResponseEntity<Void> deleteMetadataItem(@RequestHeader("Authorization") String authHeader,
                                                    @PathVariable Long id, @PathVariable String key) {
        requireOwnerOrAdmin(authHeader, id);
        vpsService.deleteMetadataItem(id, key);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/project")
    public ResponseEntity<VpsServer> assignProject(@RequestHeader("Authorization") String authHeader,
                                                     @PathVariable Long id,
                                                     @RequestBody kali.microservices.infrastructureservice.dto.AssignProjectRequest request) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.assignProject(id, request.getProjectId()));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<PageResponse<VpsActivityLog>> getActivity(@RequestHeader("Authorization") String authHeader,
                                                                      @PathVariable Long id,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.getActivity(id, page, size));
    }

    @PostMapping("/{id}/rescue")
    public ResponseEntity<Map<String, String>> rescue(@RequestHeader("Authorization") String authHeader,
                                                        @PathVariable Long id) {
        AuthenticatedUser user = authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(Map.of("adminPass", vpsService.rescueVps(id, user.userId())));
    }

    @PostMapping("/{id}/unrescue")
    public ResponseEntity<VpsServer> unrescue(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable Long id) {
        AuthenticatedUser user = authContext.requireAdmin(authHeader);
        return ResponseEntity.ok(vpsService.unrescueVps(id, user.userId()));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<VpsServer> pause(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.pauseVps(id, user.userId()));
    }

    @PostMapping("/{id}/unpause")
    public ResponseEntity<VpsServer> unpause(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.unpauseVps(id, user.userId()));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<VpsServer> suspend(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.suspendVps(id, user.userId()));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<VpsServer> resume(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.resumeVps(id, user.userId()));
    }

    @PostMapping("/{id}/shelve")
    public ResponseEntity<VpsServer> shelve(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.shelveVps(id, user.userId()));
    }

    @PostMapping("/{id}/unshelve")
    public ResponseEntity<VpsServer> unshelve(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.unshelveVps(id, user.userId()));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<VpsServer> lock(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.lockVps(id, user.userId()));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<VpsServer> unlock(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.unlockVps(id, user.userId()));
    }

    @PostMapping("/{id}/soft-reboot")
    public ResponseEntity<VpsServer> softReboot(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.softRebootVps(id, user.userId()));
    }

    @PostMapping("/{id}/rebuild")
    public ResponseEntity<Void> rebuild(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable Long id, @RequestParam String imageId) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        vpsService.rebuildVps(id, user.userId(), imageId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/console")
    public ResponseEntity<Map<String, String>> getConsole(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(Map.of("url", vpsService.getConsoleUrl(id)));
    }

    @GetMapping("/{id}/console-log")
    public ResponseEntity<Map<String, String>> getConsoleLog(@RequestHeader("Authorization") String authHeader,
                                                                 @PathVariable Long id,
                                                                 @RequestParam(defaultValue = "100") int lines) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(Map.of("log", vpsService.getConsoleLog(id, lines)));
    }

    @GetMapping("/{id}/interfaces")
    public ResponseEntity<List<kali.microservices.infrastructureservice.openstack.InterfaceDetails>> listInterfaces(
            @RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.listInterfaces(id));
    }

    @PostMapping("/{id}/interfaces")
    public ResponseEntity<kali.microservices.infrastructureservice.openstack.InterfaceDetails> attachInterface(
            @RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestParam String networkId) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(vpsService.attachInterface(id, user.userId(), networkId));
    }

    @DeleteMapping("/{id}/interfaces/{attachmentId}")
    public ResponseEntity<Void> detachInterface(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable Long id, @PathVariable String attachmentId) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        vpsService.detachInterface(id, user.userId(), attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/interfaces/{portId}/security-groups")
    public ResponseEntity<List<String>> getPortSecurityGroups(@RequestHeader("Authorization") String authHeader,
                                                                  @PathVariable Long id, @PathVariable String portId) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(vpsService.getPortSecurityGroups(portId));
    }

    @PutMapping("/{id}/interfaces/{portId}/security-groups")
    public ResponseEntity<Void> updatePortSecurityGroups(@RequestHeader("Authorization") String authHeader,
                                                            @PathVariable Long id, @PathVariable String portId,
                                                            @RequestBody List<String> groupIds) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        vpsService.updatePortSecurityGroups(id, user.userId(), portId, groupIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/backup-schedule")
    public ResponseEntity<VpsBackupSchedule> getBackupSchedule(@RequestHeader("Authorization") String authHeader,
                                                                  @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        VpsBackupSchedule schedule = backupScheduleService.get(id);
        return schedule != null ? ResponseEntity.ok(schedule) : ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/backup-schedule")
    public ResponseEntity<VpsBackupSchedule> upsertBackupSchedule(@RequestHeader("Authorization") String authHeader,
                                                                     @PathVariable Long id,
                                                                     @Valid @RequestBody BackupScheduleRequest request) {
        AuthenticatedUser user = requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(backupScheduleService.upsert(id, user.userId(), request));
    }

    @DeleteMapping("/{id}/backup-schedule")
    public ResponseEntity<Void> deleteBackupSchedule(@RequestHeader("Authorization") String authHeader,
                                                        @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        backupScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/backups")
    public ResponseEntity<List<ImageOption>> listBackups(@RequestHeader("Authorization") String authHeader,
                                                            @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(backupScheduleService.listBackups(id));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Infrastructure Service (VPS) is running!");
    }

    private AuthenticatedUser requireOwnerOrAdmin(String authHeader, Long vpsId) {
        VpsServer vps = vpsService.getVpsById(vpsId);
        return authContext.requireOwnerOrAdmin(authHeader, vps.getUserId());
    }
}
