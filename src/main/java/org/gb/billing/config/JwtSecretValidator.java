package org.gb.billing.config;

import org.gb.billing.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Validates JWT secret configuration on application startup.
 * Ensures the JWT secret is not the default placeholder and meets minimum length requirements.
 */
@Component
public class JwtSecretValidator implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtSecretValidator.class);
    private static final String DEFAULT_SECRET_PREFIX = "CHANGE_THIS_SECRET";
    private static final int MINIMUM_SECRET_LENGTH = 32;
    
    private final JwtProperties jwtProperties;

    public JwtSecretValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String secret = jwtProperties.getSecret();
        
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured. Please set jwt.secret in application.yml or JWT_SECRET environment variable.");
        }
        
        if (secret.startsWith(DEFAULT_SECRET_PREFIX)) {
            logger.warn("⚠️  WARNING: Using default JWT secret! This is INSECURE for production.");
            logger.warn("⚠️  Please set JWT_SECRET environment variable with a secure random value.");
            logger.warn("⚠️  Generate a secure secret using: openssl rand -base64 32");
        }
        
        if (secret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(
                    String.format("JWT secret must be at least %d characters long. Current length: %d", 
                            MINIMUM_SECRET_LENGTH, secret.length())
            );
        }
        
        logger.info("✓ JWT secret validation passed (length: {} characters)", secret.length());
    }
}
