package com.foodwise.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handles JWT creation and validation.
 *
 * Security controls:
 *  - Algorithm hardcoded to HS256 — never derived from token header
 *  - 'none' algorithm rejected by JJWT 0.12.x by default
 *  - exp claim always set and validated
 *  - Secret must be ≥ 32 chars from env var; startup fails otherwise
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        // SECURITY: fail fast if secret is missing or too short
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "FATAL: JWT_SECRET environment variable is not set. " +
                "Generate one with: openssl rand -hex 32");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "FATAL: JWT_SECRET must be at least 32 bytes (256 bits) for HS256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        // SECURITY: signWith uses hardcoded HS256 — algorithm never derived from token
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validates token and returns parsed claims.
     * JJWT 0.12.x rejects expired tokens and 'none' alg automatically.
     *
     * @throws JwtException if token is invalid, expired, or tampered
     */
    public Claims validateAndParseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            validateAndParseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(validateAndParseClaims(token).getSubject());
    }

    public String extractUsername(String token) {
        return validateAndParseClaims(token).get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return validateAndParseClaims(token).get("roles", List.class);
    }
}
