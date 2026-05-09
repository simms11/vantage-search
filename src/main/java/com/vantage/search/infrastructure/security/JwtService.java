package com.vantage.search.infrastructure.security;

import com.vantage.search.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expirationMs = properties.expirationMs();
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Verifies the JWT signature and returns its claims. The hot path (the
     * authentication filter) calls this once per request and reuses the
     * resulting {@link Claims} for username, jti, and expiration checks.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public OffsetDateTime extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant().atOffset(ZoneOffset.UTC);
    }

    public boolean isTokenValid(Claims claims, UserDetails userDetails) {
        // Tokens without a jti can never be revoked through the blacklist.
        // Reject them so a future code path that forgets to set .id() can't
        // mint un-revocable tokens.
        if (claims.getId() == null || claims.getId().isBlank()) {
            return false;
        }
        return userDetails.getUsername().equals(claims.getSubject())
                && claims.getExpiration() != null
                && claims.getExpiration().after(new Date());
    }
}
