package kali.microservices.authservice.controller;

import jakarta.validation.Valid;
import kali.microservices.authservice.dto.NotifyUserRequest;
import kali.microservices.authservice.repository.UserRepository;
import kali.microservices.authservice.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Service-to-service endpoints for background jobs that have no user session (billing's
 * metering alerts, support's ticket notifications). Deliberately outside /api/: the gateway and
 * the frontend's nginx only forward /api/**, so this is reachable only from inside the cluster.
 * Every call must also carry the shared INTERNAL_API_SECRET; if it isn't configured, every call
 * is rejected rather than left open.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    public static final String SECRET_HEADER = "X-Internal-Secret";

    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${internal.api.secret:}")
    private String internalSecret;

    @PostMapping("/notify")
    public ResponseEntity<Void> notifyUser(@RequestHeader(value = SECRET_HEADER, required = false) String secret,
                                           @Valid @RequestBody NotifyUserRequest request) {
        requireSecret(secret);
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        boolean sent = mailService.sendNotification(user.getEmail(), request.getSubject(), request.getBody());
        // 503 lets the caller retry later (e.g. SMTP not configured yet) instead of marking it sent.
        return sent ? ResponseEntity.accepted().build() : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@RequestHeader(value = SECRET_HEADER, required = false) String secret,
                                                       @PathVariable Long id) {
        requireSecret(secret);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "role", user.getRole().name(),
                "enabled", user.isEnabled()));
    }

    private void requireSecret(String provided) {
        if (internalSecret == null || internalSecret.isBlank() || provided == null
                || !MessageDigest.isEqual(internalSecret.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interne refusé");
        }
    }
}
