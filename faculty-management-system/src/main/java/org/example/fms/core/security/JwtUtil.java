package org.example.fms.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * Utility class to generate and validate JWTs.
 */
public class JwtUtil {

    // For production, this secret key should be loaded securely from environment
    // variables.
    // Keys.secretKeyFor(SignatureAlgorithm.HS256) generates a secure random key.
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME_MILLIS = 3600000; // 1 hour

    /**
     * Generates a JWT given a user ID and role.
     *
     * @param userId The ID of the authenticated user.
     * @param role   The primary role of the authenticated user.
     * @return The serialized JWT string.
     */
    public static String generateToken(String userId, String role) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expirationDate = new Date(nowMillis + EXPIRATION_TIME_MILLIS);

        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Validates a JWT and returns the claims if valid.
     *
     * @param token The JWT string.
     * @return Claims object if the token is valid.
     * @throws JwtException if the token is invalid or expired.
     */
    public static Claims validateTokenAndGetClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
