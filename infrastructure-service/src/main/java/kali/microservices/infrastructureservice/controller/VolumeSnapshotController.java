package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateVolumeSnapshotRequest;
import kali.microservices.infrastructureservice.entities.VpsVolumeSnapshot;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.VolumeSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/volume-snapshots")
@RequiredArgsConstructor
public class VolumeSnapshotController {

    private final VolumeSnapshotService service;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<VpsVolumeSnapshot> create(@RequestHeader("Authorization") String authHeader,
                                                       @Valid @RequestBody CreateVolumeSnapshotRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSnapshot(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VpsVolumeSnapshot>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @GetMapping("/volume/{volumeId}")
    public ResponseEntity<List<VpsVolumeSnapshot>> getByVolume(@RequestHeader("Authorization") String authHeader,
                                                                  @PathVariable Long volumeId) {
        List<VpsVolumeSnapshot> snapshots = service.getByVolume(volumeId);
        if (!snapshots.isEmpty()) {
            authContext.requireOwnerOrAdmin(authHeader, snapshots.get(0).getUserId());
        }
        return ResponseEntity.ok(snapshots);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        VpsVolumeSnapshot snapshot = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, snapshot.getUserId());
        service.deleteSnapshot(id);
        return ResponseEntity.noContent().build();
    }
}
