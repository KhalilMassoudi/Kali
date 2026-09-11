package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateServerGroupRequest;
import kali.microservices.infrastructureservice.entities.ClientServerGroup;
import kali.microservices.infrastructureservice.openstack.ServerGroupDetails;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.ServerGroupManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/server-groups")
@RequiredArgsConstructor
public class ServerGroupController {

    private final ServerGroupManagementService service;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<ClientServerGroup> create(@RequestHeader("Authorization") String authHeader,
                                                       @Valid @RequestBody CreateServerGroupRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClientServerGroup>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerGroupDetails> getLiveDetails(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long id) {
        ClientServerGroup group = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, group.getUserId());
        return ResponseEntity.ok(service.getLiveDetails(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        ClientServerGroup group = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, group.getUserId());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
