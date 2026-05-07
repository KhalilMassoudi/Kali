package kali.microservices.authservice.service;

public interface IJwtService {
    public String generateToken(String email, String role);
    public boolean isTokenValid(String token);
    public String extractEmail(String token);
}
