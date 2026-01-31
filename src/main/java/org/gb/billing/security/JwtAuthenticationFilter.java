package org.gb.billing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter that extracts and validates JWT tokens from requests.
 * Runs once per request to authenticate users and populate the security context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Extract user information from token
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);
                Long tenantId = jwtTokenProvider.getTenantIdFromToken(jwt);
                
                // Set tenant context for multi-tenant filtering
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                    logger.debug("Set tenant context: tenantId={}", tenantId);
                }
                
                // Create CustomUserDetails as principal (contains userId and tenantId for rate limiting)
                CustomUserDetails userDetails = new CustomUserDetails(
                        userId,
                        email,
                        null,  // No password needed for JWT authentication
                        role,
                        tenantId,
                        true   // Assume active if token is valid
                );
                
                // Create authentication token with CustomUserDetails as principal
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authenticated user: {} with role: {}", email, role);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear tenant context after request to prevent leakage
            TenantContext.clear();
        }
    }

    /**
     * Extract JWT token from the Authorization header.
     * 
     * @param request the HTTP request
     * @return the JWT token, or null if not present
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
