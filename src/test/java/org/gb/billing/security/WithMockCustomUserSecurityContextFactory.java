package org.gb.billing.security;

import org.gb.billing.entity.Role;
import org.gb.billing.entity.Tenant;
import org.gb.billing.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // Create a User entity
        User user = new User(annotation.email(), "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", Role.valueOf(annotation.role()));
        user.setId(annotation.id());
        
        // Create and set tenant if tenantId is provided
        if (annotation.tenantId() > 0) {
            Tenant tenant = new Tenant("Test Tenant");
            tenant.setId(annotation.tenantId());
            user.setTenant(tenant);
        }

        // Create authentication token with the User entity as principal
        Authentication auth = new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities());
        context.setAuthentication(auth);

        return context;
    }
}
