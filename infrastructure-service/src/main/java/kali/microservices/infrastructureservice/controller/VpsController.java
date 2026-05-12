package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateVpsRequest;
import kali.microservices.infrastructureservice.entities.VpsServer;
import kali.microservices.infrastructureservice.service.VpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/vps")
@RequiredArgsConstructor
public class VpsController {

    private final VpsService vpsService;

    @PostMapping
    public ResponseEntity<VpsServer> createVps(@Valid @RequestBody CreateVpsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vpsService.createVps(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VpsServer>> getVpsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(vpsService.getVpsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VpsServer> getVpsById(@PathVariable Long id) {
        return ResponseEntity.ok(vpsService.getVpsById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VpsServer> updateStatus(@PathVariable Long id,
                                                   @RequestParam VpsServer.VpsStatus status) {
        return ResponseEntity.ok(vpsService.updateVpsStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVps(@PathVariable Long id) {
        vpsService.deleteVps(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Infrastructure Service (VPS) is running!");
    }
}