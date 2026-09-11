package kali.microservices.authservice.controller;

import jakarta.validation.Valid;
import kali.microservices.authservice.dto.AdminUserDto;
import kali.microservices.authservice.dto.NotifyUserRequest;
import kali.microservices.authservice.dto.SmtpConfigDto;
import kali.microservices.authservice.dto.TestEmailRequest;
import kali.microservices.authservice.dto.UpdateSmtpConfigRequest;
import kali.microservices.authservice.dto.UpdateUserEnabledRequest;
import kali.microservices.authservice.dto.UpdateUserRoleRequest;
import kali.microservices.authservice.service.AuthService;
import kali.microservices.authservice.service.NotificationService;
import kali.microservices.authservice.service.SmtpConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final SmtpConfigService smtpConfigService;
    private final NotificationService notificationService;

    @GetMapping("/smtp-config")
    public ResponseEntity<SmtpConfigDto> getSmtpConfig(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(smtpConfigService.getConfig(authorization));
    }

    @PutMapping("/smtp-config")
    public ResponseEntity<SmtpConfigDto> updateSmtpConfig(
            @Valid @RequestBody UpdateSmtpConfigRequest request,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(smtpConfigService.updateConfig(authorization, request));
    }

    @PostMapping("/smtp-config/test")
    public ResponseEntity<Void> sendTestEmail(
            @Valid @RequestBody TestEmailRequest request,
            @RequestHeader("Authorization") String authorization) {
        smtpConfigService.sendTestEmail(authorization, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notifyUser(
            @Valid @RequestBody NotifyUserRequest request,
            @RequestHeader("Authorization") String authorization) {
        notificationService.notifyUser(authorization, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> listUsers(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.listUsers(authorization));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<AdminUserDto> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.updateUserRole(authorization, id, request.getRole()));
    }

    @PutMapping("/users/{id}/enabled")
    public ResponseEntity<AdminUserDto> updateUserEnabled(
            @PathVariable Long id,
            @RequestBody UpdateUserEnabledRequest request,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.setUserEnabled(authorization, id, request.isEnabled()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorization) {
        authService.deleteUser(authorization, id);
        return ResponseEntity.ok().build();
    }
}
