package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateDomainRequest;
import kali.microservices.infrastructureservice.entities.Domain;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.DomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<Domain> registerDomain(@RequestHeader("Authorization") String authHeader,
                                                  @Valid @RequestBody CreateDomainRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(domainService.registerDomain(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Domain>> getDomainsByUser(@RequestHeader("Authorization") String authHeader,
                                                           @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(domainService.getDomainsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Domain> getDomainById(@RequestHeader("Authorization") String authHeader,
                                                 @PathVariable Long id) {
        Domain domain = domainService.getDomainById(id);
        authContext.requireOwnerOrAdmin(authHeader, domain.getUserId());
        return ResponseEntity.ok(domain);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDomain(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable Long id) {
        Domain domain = domainService.getDomainById(id);
        authContext.requireOwnerOrAdmin(authHeader, domain.getUserId());
        domainService.deleteDomain(id);
        return ResponseEntity.noContent().build();
    }
}
