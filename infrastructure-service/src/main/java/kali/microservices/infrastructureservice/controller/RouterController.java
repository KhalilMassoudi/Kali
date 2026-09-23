package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateRouterRequest;
import kali.microservices.infrastructureservice.dto.RouterInterfaceRequest;
import kali.microservices.infrastructureservice.dto.SetRouterGatewayRequest;
import kali.microservices.infrastructureservice.entities.ClientRouter;
import kali.microservices.infrastructureservice.openstack.RouterInterfaceDetails;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.RouterManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/routers")
@RequiredArgsConstructor
public class RouterController {

    private final RouterManagementService service;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<ClientRouter> create(@RequestHeader("Authorization") String authHeader,
                                                 @Valid @RequestBody CreateRouterRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClientRouter>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                           @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @GetMapping("/{id}/interfaces")
    public ResponseEntity<List<RouterInterfaceDetails>> listInterfaces(@RequestHeader("Authorization") String authHeader,
                                                                          @PathVariable Long id) {
        ClientRouter router = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, router.getUserId());
        return ResponseEntity.ok(service.listInterfaces(id));
    }

    @PostMapping("/{id}/interfaces")
    public ResponseEntity<Void> attachInterface(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody RouterInterfaceRequest request) {
        ClientRouter router = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, router.getUserId());
        service.attachInterface(id, request.getSubnetId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/interfaces/{subnetId}")
    public ResponseEntity<Void> detachInterface(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable Long id, @PathVariable String subnetId) {
        ClientRouter router = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, router.getUserId());
        service.detachInterface(id, subnetId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/gateway")
    public ResponseEntity<ClientRouter> setGateway(@RequestHeader("Authorization") String authHeader,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody SetRouterGatewayRequest request) {
        ClientRouter router = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, router.getUserId());
        return ResponseEntity.ok(service.setGateway(id, request.getExternalNetworkId()));
    }

    @DeleteMapping("/{id}/gateway")
    public ResponseEntity<ClientRouter> clearGateway(@RequestHeader("Authorization") String authHeader,
                                                       @PathVariable Long id) {
        ClientRouter router = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, router.getUserId());
        return ResponseEntity.ok(service.clearGateway(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        ClientRouter router = service.getById(id);
        authContext.requireOwnerOrAdmin(authHeader, router.getUserId());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
