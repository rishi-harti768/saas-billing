package org.gb.billing.entity;

/**
 * User roles in the system for authorization.
 * 
 * ROLE_ADMIN: SaaS platform owner with full system access (no tenant association)
 * ROLE_USER: Subscriber with tenant-scoped access (must have tenant association)
 */
public enum Role {
    /**
     * SaaS platform owner with full system access.
     * Admin users have no tenant association (tenantId = null).
     */
    ROLE_ADMIN,
    
    /**
     * Subscriber with tenant-scoped access.
     * User accounts must have a valid tenant association (tenantId != null).
     */
    ROLE_USER
}
