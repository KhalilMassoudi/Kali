package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateClusterRequest;
import kali.microservices.infrastructureservice.entities.K8sCluster;
import kali.microservices.infrastructureservice.security.AuthContext;
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
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<K8sCluster> createCluster(@RequestHeader("Authorization") String authHeader,
                                                      @Valid @RequestBody CreateClusterRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(k8sClusterService.createCluster(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<K8sCluster>> getClustersByUser(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(k8sClusterService.getClustersByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<K8sCluster> getClusterById(@RequestHeader("Authorization") String authHeader,
                                                       @PathVariable Long id) {
        K8sCluster cluster = k8sClusterService.getClusterById(id);
        authContext.requireOwnerOrAdmin(authHeader, cluster.getUserId());
        return ResponseEntity.ok(cluster);
    }

    @PatchMapping("/{id}/scale")
    public ResponseEntity<K8sCluster> scaleCluster(@RequestHeader("Authorization") String authHeader,
                                                     @PathVariable Long id,
                                                     @RequestParam int nodeCount) {
        requireOwnerOrAdmin(authHeader, id);
        return ResponseEntity.ok(k8sClusterService.scaleCluster(id, nodeCount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCluster(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable Long id) {
        requireOwnerOrAdmin(authHeader, id);
        k8sClusterService.deleteCluster(id);
        return ResponseEntity.noContent().build();
    }

    private void requireOwnerOrAdmin(String authHeader, Long clusterId) {
        K8sCluster cluster = k8sClusterService.getClusterById(clusterId);
        authContext.requireOwnerOrAdmin(authHeader, cluster.getUserId());
    }
}
