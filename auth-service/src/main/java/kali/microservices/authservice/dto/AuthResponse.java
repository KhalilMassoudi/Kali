package kali.microservices.authservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private String redirectTo;
    private boolean requiresTwoFactor;
    private String pendingToken;

    public static AuthResponse success(String token, String role, String redirectTo) {
        AuthResponse response = new AuthResponse();
        response.token = token;
        response.role = role;
        response.redirectTo = redirectTo;
        return response;
    }

    public static AuthResponse pendingTwoFactor(String pendingToken) {
        AuthResponse response = new AuthResponse();
        response.requiresTwoFactor = true;
        response.pendingToken = pendingToken;
        return response;
    }
}
