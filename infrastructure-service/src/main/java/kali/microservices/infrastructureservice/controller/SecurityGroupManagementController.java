package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.AddSecurityGroupRuleRequest;
import kali.microservices.infrastructureservice.dto.CreateSecurityGroupRequest;
import kali.microservices.infrastructureservice.entities.VpsSecurityGroup;
import kali.microservices.infrastructureservice.openstack.SecurityGroupRuleOption;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.SecurityGroupManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Client-owned security groups (create/delete a group + manage its rules). Distinct from the
 * existing VM-scoped assign/remove endpoints on {@link VpsController}, which stay as-is and
 * now naturally list a client's own groups alongside shared/admin-created ones.
 */
@RestController
@RequestMapping("/api/infrastructure/security-groups")
@RequiredArgsConstructor
public class SecurityGroupManagementController {

    private final SecurityGroupManagementService service;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<VpsSecurityGroup> create(@RequestHeader("Authorization") String authHeader,
                                                     @Valid @RequestBody CreateSecurityGroupRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VpsSecurityGroup>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                               @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/rules")
    public ResponseEntity<List<SecurityGroupRuleOption>> listRules(@RequestHeader("Authorization") String authHeader,
                                                                      @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(service.listRules(id));
    }

    @PostMapping("/{id}/rules")
    public ResponseEntity<SecurityGroupRuleOption> addRule(@RequestHeader("Authorization") String authHeader,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody AddSecurityGroupRuleRequest request) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRule(id, request.getDirection(),
                request.getProtocol(), request.getPortMin(), request.getPortMax(), request.getCidr()));
    }

    @DeleteMapping("/{id}/rules/{ruleId}")
    public ResponseEntity<Void> removeRule(@RequestHeader("Authorization") String authHeader,
                                             @PathVariable Long id, @PathVariable String ruleId) {
        requireOwnerOrAdmin(authHeader, id);
        service.removeRule(id, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/project")
    public ResponseEntity<VpsSecurityGroup> assignProject(@RequestHeader("Authorization") String authHeader,
                                                             @PathVariable Long id,
                                                             @RequestBody kali.microservices.infrastructureservice.dto.AssignProjectRequest request) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(service.assignProject(id, request.getProjectId()));
    }

    private void requireOwnerOrAdmin(String authHeader, Long id) {
        VpsSecurityGroup group = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, group.getUserId());
    }
}
