package kali.microservices.chatservice.security;

public record AuthenticatedUser(Long userId, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
