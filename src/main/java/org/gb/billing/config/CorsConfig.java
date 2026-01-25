package org.gb.billing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration for frontend integration.
 * Allows specified origins to access the API from different domains.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:Authorization}")
    private String exposedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * Configure CORS settings for the application.
     * 
     * Default configuration:
     * - Allowed origins: http://localhost:3000, http://localhost:4200 (React, Angular dev servers)
     * - Allowed methods: GET, POST, PUT, DELETE, OPTIONS
     * - Allowed headers: All (*)
     * - Exposed headers: Authorization (for JWT tokens)
     * - Allow credentials: true (for cookies/auth headers)
     * - Max age: 3600 seconds (1 hour)
     * 
     * Override in application.yml:
     * cors:
     *   allowed-origins: https://app.example.com,https://admin.example.com
     *   allowed-methods: GET,POST,PUT,DELETE,OPTIONS
     *   allowed-headers: Content-Type,Authorization
     *   exposed-headers: Authorization
     *   allow-credentials: true
     *   max-age: 3600
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // Set allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        
        // Set allowed headers
        if ("*".equals(allowedHeaders)) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }
        
        // Set exposed headers (headers that browser can access)
        configuration.setExposedHeaders(Arrays.asList(exposedHeaders.split(",")));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(allowCredentials);
        
        // Set preflight cache duration
        configuration.setMaxAge(maxAge);
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
