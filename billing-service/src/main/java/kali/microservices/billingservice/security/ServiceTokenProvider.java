package kali.microservices.billingservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Mints short-lived, self-signed admin JWTs (using the same shared jwt.secret every
 * service trusts) so the metering job can call infrastructure-service's admin-only
 * fleet-wide endpoints without a real user session - there's no human request context
 * behind a scheduled tick.
 */
@Service
public class ServiceTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String mintServiceAdminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 0L);
        claims.put("role", "ADMIN");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("billing-service@internal")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
