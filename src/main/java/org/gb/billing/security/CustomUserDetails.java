package org.gb.billing.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom implementation of Spring Security's UserDetails interface.
 * Extends the standard UserDetails with tenant ID for multi-tenant support.
 */
public class CustomUserDetails implements UserDetails {
    
    private final Long userId;
    private final String email;
    private final String password;
    private final String role;
    private final Long tenantId;
    private final boolean active;

    public CustomUserDetails(Long userId, String email, String password, String role, Long tenantId, boolean active) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
        this.active = active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // Custom getters
    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
