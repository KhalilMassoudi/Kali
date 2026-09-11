package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateVolumeRequest;
import kali.microservices.infrastructureservice.entities.VpsVolume;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.VolumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/volumes")
@RequiredArgsConstructor
public class VolumeController {

    private final VolumeService volumeService;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<VpsVolume> createVolume(@RequestHeader("Authorization") String authHeader,
                                                   @Valid @RequestBody CreateVolumeRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(volumeService.createVolume(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VpsVolume>> getVolumesByUser(@RequestHeader("Authorization") String authHeader,
                                                              @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(volumeService.getVolumesByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VpsVolume> getVolumeById(@RequestHeader("Authorization") String authHeader,
                                                    @PathVariable Long id) {
        VpsVolume volume = volumeService.getVolumeById(id);
        authContext.requireOwnerOrAdmin(authHeader, volume.getUserId());
        return ResponseEntity.ok(volume);
    }

    @PostMapping("/{id}/attach")
    public ResponseEntity<VpsVolume> attachVolume(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable Long id, @RequestParam Long vpsId) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(volumeService.attachVolume(id, vpsId));
    }

    @PostMapping("/{id}/detach")
    public ResponseEntity<VpsVolume> detachVolume(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(volumeService.detachVolume(id));
    }

    @PutMapping("/{id}/project")
    public ResponseEntity<VpsVolume> assignProject(@RequestHeader("Authorization") String authHeader,
                                                    @PathVariable Long id,
                                                    @RequestBody kali.microservices.infrastructureservice.dto.AssignProjectRequest request) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(volumeService.assignProject(id, request.getProjectId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVolume(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        volumeService.deleteVolume(id);
        return ResponseEntity.noContent().build();
    }

    private void requireOwnerOrAdmin(String authHeader, Long volumeId) {
        VpsVolume volume = volumeService.getVolumeById(volumeId);
        authContext.requireOwnerOrAdmin(authHeader, volume.getUserId());
    }
}
