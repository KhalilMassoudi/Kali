package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateDomainRequest;
import kali.microservices.infrastructureservice.entities.Domain;
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

    @PostMapping
    public ResponseEntity<Domain> registerDomain(@Valid @RequestBody CreateDomainRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(domainService.registerDomain(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Domain>> getDomainsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(domainService.getDomainsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Domain> getDomainById(@PathVariable Long id) {
        return ResponseEntity.ok(domainService.getDomainById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDomain(@PathVariable Long id) {
        domainService.deleteDomain(id);
        return ResponseEntity.noContent().build();
    }
}