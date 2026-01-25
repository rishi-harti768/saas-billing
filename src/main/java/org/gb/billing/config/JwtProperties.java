package org.gb.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT token generation and validation.
 * Binds properties from application.yml (jwt.secret and jwt.expiration).
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * Secret key for signing JWT tokens.
     * MUST be at least 32 characters and should be set via environment variable in production.
     */
    private String secret;
    
    /**
     * JWT token expiration time in milliseconds.
     * Default: 86400000 (24 hours)
     */
    private Long expiration;

    // Getters and Setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
}
