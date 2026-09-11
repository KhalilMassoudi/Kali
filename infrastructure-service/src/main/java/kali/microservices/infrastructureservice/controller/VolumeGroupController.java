package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateVolumeGroupRequest;
import kali.microservices.infrastructureservice.entities.ClientVolumeGroup;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.VolumeGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/infrastructure/volume-groups")
@RequiredArgsConstructor
public class VolumeGroupController {

    private final VolumeGroupService service;
    private final AuthContext authContext;

    @GetMapping("/supported")
    public ResponseEntity<Map<String, Boolean>> isSupported(@RequestHeader("Authorization") String authHeader) {
        authContext.resolve(authHeader);
        return ResponseEntity.ok(Map.of("supported", service.isSupported()));
    }

    @PostMapping
    public ResponseEntity<ClientVolumeGroup> create(@RequestHeader("Authorization") String authHeader,
                                                       @Valid @RequestBody CreateVolumeGroupRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClientVolumeGroup>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        ClientVolumeGroup group = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, group.getUserId());
        service.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/group-snapshots")
    public ResponseEntity<List<Map<String, Object>>> listGroupSnapshots(@RequestHeader("Authorization") String authHeader) {
        authContext.resolve(authHeader);
        return ResponseEntity.ok(service.listGroupSnapshots());
    }

    @PostMapping("/{id}/group-snapshots")
    public ResponseEntity<Map<String, Object>> createGroupSnapshot(@RequestHeader("Authorization") String authHeader,
                                                                       @PathVariable Long id,
                                                                       @RequestBody Map<String, String> body) {
        ClientVolumeGroup group = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, group.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createGroupSnapshot(group.getExternalId(), body.get("name"), body.get("description")));
    }

    @DeleteMapping("/group-snapshots/{id}")
    public ResponseEntity<Void> deleteGroupSnapshot(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        authContext.resolve(authHeader);
        service.deleteGroupSnapshot(id);
        return ResponseEntity.noContent().build();
    }
}
