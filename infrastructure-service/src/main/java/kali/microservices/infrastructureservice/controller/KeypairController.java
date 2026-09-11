package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateKeypairRequest;
import kali.microservices.infrastructureservice.dto.CreateKeypairResponse;
import kali.microservices.infrastructureservice.entities.ClientKeypair;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.KeypairManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/keypairs")
@RequiredArgsConstructor
public class KeypairController {

    private final KeypairManagementService service;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<CreateKeypairResponse> create(@RequestHeader("Authorization") String authHeader,
                                                           @Valid @RequestBody CreateKeypairRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClientKeypair>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                            @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        ClientKeypair keypair = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, keypair.getUserId());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
