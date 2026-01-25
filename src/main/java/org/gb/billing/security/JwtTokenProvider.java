package org.gb.billing.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.gb.billing.config.JwtProperties;
import org.gb.billing.exception.ExpiredTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating and validating JWT tokens.
 * Uses JJWT library with HS256 algorithm for token signing.
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Create a secure key from the secret string
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token for a user.
     * 
     * @param userId the user's ID
     * @param email the user's email
     * @param role the user's role
     * @param tenantId the user's tenant ID (null for admins)
     * @return the generated JWT token
     */
    public String generateToken(Long userId, String email, String role, Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        if (tenantId != null) {
            claims.put("tenantId", tenantId);
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        String token = Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        logger.debug("Generated JWT token for user: {} with role: {}", email, role);
        return token;
    }

    /**
     * Validate a JWT token.
     * 
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            logger.warn("JWT token has expired");
            throw new ExpiredTokenException("JWT token has expired");
        } catch (MalformedJwtException ex) {
            logger.warn("Invalid JWT token format");
            return false;
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token");
            return false;
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty");
            return false;
        } catch (JwtException ex) {
            logger.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID from JWT token.
     * 
     * @param token the JWT token
     * @return the user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract email from JWT token.
     * 
     * @param token the JWT token
     * @return the email address
     */
    public String getEmailFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extract role from JWT token.
     * 
     * @param token the JWT token
     * @return the user role
     */
    public String getRoleFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extract tenant ID from JWT token.
     * 
     * @param token the JWT token
     * @return the tenant ID, or null if not present (for admins)
     */
    public Long getTenantIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        Object tenantId = claims.get("tenantId");
        return tenantId != null ? ((Number) tenantId).longValue() : null;
    }

    /**
     * Extract all claims from JWT token.
     * 
     * @param token the JWT token
     * @return the claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
