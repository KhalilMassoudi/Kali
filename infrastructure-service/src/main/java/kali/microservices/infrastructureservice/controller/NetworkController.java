package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateNetworkRequest;
import kali.microservices.infrastructureservice.entities.VpsNetwork;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.NetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/networks")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<VpsNetwork> createNetwork(@RequestHeader("Authorization") String authHeader,
                                                      @Valid @RequestBody CreateNetworkRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(networkService.createNetwork(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VpsNetwork>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                         @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(networkService.getNetworksByUser(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNetwork(@RequestHeader("Authorization") String authHeader,
                                                @PathVariable Long id) {
        VpsNetwork network = networkService.getNetworkById(id);
        authContext.requireOwnerOrAdmin(authHeader, network.getUserId());
        networkService.deleteNetwork(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/project")
    public ResponseEntity<VpsNetwork> assignProject(@RequestHeader("Authorization") String authHeader,
                                                       @PathVariable Long id,
                                                       @RequestBody kali.microservices.infrastructureservice.dto.AssignProjectRequest request) {
        VpsNetwork network = networkService.getNetworkById(id);
        authContext.requireOwnerOrAdmin(authHeader, network.getUserId());
        return ResponseEntity.ok(networkService.assignProject(id, request.getProjectId()));
    }
}
