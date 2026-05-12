package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateClusterRequest;
import kali.microservices.infrastructureservice.entities.K8sCluster;
import kali.microservices.infrastructureservice.service.K8sClusterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/clusters")
@RequiredArgsConstructor
public class K8sClusterController {

    private final K8sClusterService k8sClusterService;

    @PostMapping
    public ResponseEntity<K8sCluster> createCluster(@Valid @RequestBody CreateClusterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(k8sClusterService.createCluster(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<K8sCluster>> getClustersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(k8sClusterService.getClustersByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<K8sCluster> getClusterById(@PathVariable Long id) {
        return ResponseEntity.ok(k8sClusterService.getClusterById(id));
    }

    @PatchMapping("/{id}/scale")
    public ResponseEntity<K8sCluster> scaleCluster(@PathVariable Long id,
                                                    @RequestParam int nodeCount) {
        return ResponseEntity.ok(k8sClusterService.scaleCluster(id, nodeCount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCluster(@PathVariable Long id) {
        k8sClusterService.deleteCluster(id);
        return ResponseEntity.noContent().build();
    }
}