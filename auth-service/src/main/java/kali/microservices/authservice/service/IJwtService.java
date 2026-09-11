package kali.microservices.authservice.service;

public interface IJwtService {
    public String generateToken(Long userId, String email, String role);
    public boolean isTokenValid(String token);
    public String extractEmail(String token);
    public String generatePendingTwoFactorToken(Long userId, String email);
    public Long extractPendingTwoFactorUserId(String token);
}
