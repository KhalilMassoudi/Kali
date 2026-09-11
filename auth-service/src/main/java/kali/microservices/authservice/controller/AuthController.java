package kali.microservices.authservice.controller;

import jakarta.validation.Valid;
import kali.microservices.authservice.dto.AuthResponse;
import kali.microservices.authservice.dto.ChangePasswordRequest;
import kali.microservices.authservice.dto.DisableTwoFactorRequest;
import kali.microservices.authservice.dto.ForgotPasswordRequest;
import kali.microservices.authservice.dto.LoginRequest;
import kali.microservices.authservice.dto.RegisterRequest;
import kali.microservices.authservice.dto.ResetPasswordRequest;
import kali.microservices.authservice.dto.TwoFactorCodeRequest;
import kali.microservices.authservice.dto.TwoFactorLoginVerifyRequest;
import kali.microservices.authservice.dto.TwoFactorSetupResponse;
import kali.microservices.authservice.dto.UpdateProfileRequest;
import kali.microservices.authservice.dto.UserInfoDto;
import kali.microservices.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getCurrentUser(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.getUserInfo(authorization));
    }

    @PutMapping("/me")
    public ResponseEntity<UserInfoDto> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.updateProfile(authorization, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authorization) {
        authService.changePassword(authorization, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api-key/regenerate")
    public ResponseEntity<UserInfoDto> regenerateApiKey(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.regenerateApiKey(authorization));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateAccount(@RequestHeader("Authorization") String authorization) {
        authService.deactivateAccount(authorization);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setupTwoFactor(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.setupTwoFactor(authorization));
    }

    @PostMapping("/2fa/verify-setup")
    public ResponseEntity<Void> verifyTwoFactorSetup(
            @Valid @RequestBody TwoFactorCodeRequest request,
            @RequestHeader("Authorization") String authorization) {
        authService.verifyTwoFactorSetup(authorization, request.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Void> disableTwoFactor(
            @Valid @RequestBody DisableTwoFactorRequest request,
            @RequestHeader("Authorization") String authorization) {
        authService.disableTwoFactor(authorization, request.getPassword(), request.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/login-verify")
    public ResponseEntity<AuthResponse> verifyTwoFactorLogin(@Valid @RequestBody TwoFactorLoginVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyTwoFactorLogin(request.getPendingToken(), request.getCode()));
    }

    @PostMapping("/admin/create")
    public ResponseEntity<AuthResponse> createAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.createAdmin(request, authorization));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running!");
    }
}