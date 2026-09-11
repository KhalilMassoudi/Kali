package kali.microservices.infrastructureservice.controller;

import jakarta.validation.Valid;
import kali.microservices.infrastructureservice.dto.CreateClientProjectRequest;
import kali.microservices.infrastructureservice.dto.ProjectResourcesSummary;
import kali.microservices.infrastructureservice.dto.UpdateClientProjectRequest;
import kali.microservices.infrastructureservice.entities.ClientProject;
import kali.microservices.infrastructureservice.security.AuthContext;
import kali.microservices.infrastructureservice.service.ClientProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infrastructure/projects")
@RequiredArgsConstructor
public class ClientProjectController {

    private final ClientProjectService clientProjectService;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<ClientProject> createProject(@RequestHeader("Authorization") String authHeader,
                                                          @Valid @RequestBody CreateClientProjectRequest request) {
        authContext.requireOwnerOrAdmin(authHeader, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(clientProjectService.createProject(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClientProject>> getByUser(@RequestHeader("Authorization") String authHeader,
                                                            @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(clientProjectService.getProjectsByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientProject> getById(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable Long id) {
        ClientProject project = clientProjectService.getProjectById(id);
        authContext.requireOwnerOrAdmin(authHeader, project.getUserId());
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientProject> renameProject(@RequestHeader("Authorization") String authHeader,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody UpdateClientProjectRequest request) {
        ClientProject project = clientProjectService.getProjectById(id);
        authContext.requireOwnerOrAdmin(authHeader, project.getUserId());
        return ResponseEntity.ok(clientProjectService.renameProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@RequestHeader("Authorization") String authHeader,
                                                @PathVariable Long id) {
        ClientProject project = clientProjectService.getProjectById(id);
        authContext.requireOwnerOrAdmin(authHeader, project.getUserId());
        clientProjectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/resources")
    public ResponseEntity<ProjectResourcesSummary> getResources(@RequestHeader("Authorization") String authHeader,
                                                                    @PathVariable Long id) {
        ClientProject project = clientProjectService.getProjectById(id);
        authContext.requireOwnerOrAdmin(authHeader, project.getUserId());
        return ResponseEntity.ok(clientProjectService.getProjectResources(id));
    }
}
