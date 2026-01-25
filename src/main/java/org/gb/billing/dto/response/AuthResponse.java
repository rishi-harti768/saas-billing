package org.gb.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for authentication response containing JWT token and user information.
 * Used by POST /api/v1/auth/login and POST /api/v1/auth/register endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private Long expiresIn; // Expiration time in milliseconds
    private String role;
    private Long tenantId; // Null for ROLE_ADMIN
    private String email;

    // Constructors
    public AuthResponse() {
    }

    public AuthResponse(String token, Long expiresIn, String role, Long tenantId, String email) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.role = role;
        this.tenantId = tenantId;
        this.email = email;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
