package kali.microservices.billingservice.security;

public record AuthenticatedUser(Long userId, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isOwnerOrAdmin(Long resourceOwnerId) {
        return isAdmin() || (userId != null && userId.equals(resourceOwnerId));
    }
}
