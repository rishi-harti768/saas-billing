package org.gb.billing.dto.response;

/**
 * DTO for user response (registration).
 * Excludes sensitive information like password.
 */
public class UserResponse {
    private Long id;
    private String email;
    private String role;
    private Long tenantId;
    private String firstName;
    private String lastName;

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String role, Long tenantId, String firstName, String lastName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
