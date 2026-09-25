package kali.microservices.billingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors just the fields billing needs from auth-service's AdminUserDto / UserInfoDto. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSnapshot(Long id, String email, String firstName, String lastName, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public String displayName() {
        String name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        return name.isEmpty() ? email : name;
    }
}
