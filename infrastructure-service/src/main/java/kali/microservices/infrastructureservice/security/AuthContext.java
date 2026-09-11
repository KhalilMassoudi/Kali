package kali.microservices.infrastructureservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;

/**
 * Independently verifies JWTs issued by auth-service, using the shared {@code jwt.secret}.
 * Unlike relying on the Gateway's forwarded X-User-* headers, this works even if
 * infrastructure-service is reached directly (bypassing the Gateway) and doesn't require
 * trusting an intermediate hop.
 */
@Service
public class AuthContext {

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public AuthenticatedUser resolve(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token manquant ou invalide");
        }
        String token = authorizationHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = claims.get("userId", Long.class);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            return new AuthenticatedUser(userId, email, role);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalide ou expiré");
        }
    }

    public AuthenticatedUser requireAdmin(String authorizationHeader) {
        AuthenticatedUser user = resolve(authorizationHeader);
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs");
        }
        return user;
    }

    public AuthenticatedUser requireOwnerOrAdmin(String authorizationHeader, Long resourceOwnerId) {
        AuthenticatedUser user = resolve(authorizationHeader);
        if (!user.isOwnerOrAdmin(resourceOwnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return user;
    }
}
