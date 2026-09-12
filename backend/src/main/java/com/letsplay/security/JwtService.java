package com.letsplay.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service component responsible for JSON Web Tokens lifecycle management.
 * Integrated from security module.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /**
     * Helper mapping the raw secret byte configuration to a secure HMAC-SHA key.
     *
     * @return cryptographically secured HMAC SecretKey instance.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed, structured JSON Web Token for an authenticated user session.
     *
     * @param publicId the user's system-wide unique public identifier (mapped to the token subject claim).
     * @param role     the authorization role classification.
     * @return the serialized JWT string.
     */
    public String generateToken(String publicId, String role) {
        return Jwts.builder()
                .subject(publicId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the subject claim (the user's public ID) from a signed token.
     */
    public String extractPublicId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates whether a token signature is cryptographically valid and not expired.
     *
     * @param token raw JWT string to evaluate.
     * @return {@code true} if valid, {@code false} if parsing triggers a signature discrepancy or expiration.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Centralized parser checking the token signature against the cryptographically secure signing key.
     *
     * @param token JWT string to parse.
     * @return the set of verified claims.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}