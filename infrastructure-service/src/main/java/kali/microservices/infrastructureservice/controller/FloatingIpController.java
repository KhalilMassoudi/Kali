package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.AllocateFloatingIpRequest;
import kali.microservices.infrastructureservice.dto.AssociateFloatingIpRequest;
import kali.microservices.infrastructureservice.entities.ClientFloatingIp;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.FloatingIpManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/floating-ips")
@RequiredArgsConstructor
public class FloatingIpController {

    private final FloatingIpManagementService service;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<ClientFloatingIp> allocate(@RequestHeader("Authorization") String authHeader,
                                                        @Valid @RequestBody AllocateFloatingIpRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.allocate(request.getUserId(), request.getPool()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClientFloatingIp>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @PostMapping("/{id}/associate")
    public ResponseEntity<ClientFloatingIp> associate(@RequestHeader("Authorization") String authHeader,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody AssociateFloatingIpRequest request) {
        ClientFloatingIp ip = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, ip.getUserId());
        return ResponseEntity.ok(service.associate(id, request.getVpsId()));
    }

    @PostMapping("/{id}/disassociate")
    public ResponseEntity<ClientFloatingIp> disassociate(@RequestHeader("Authorization") String authHeader,
                                                             @PathVariable Long id) {
        ClientFloatingIp ip = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, ip.getUserId());
        return ResponseEntity.ok(service.disassociate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> release(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        ClientFloatingIp ip = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, ip.getUserId());
        service.release(id);
        return ResponseEntity.noContent().build();
    }
}
