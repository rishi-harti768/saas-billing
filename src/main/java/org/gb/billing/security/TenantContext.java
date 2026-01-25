package org.gb.billing.security;

/**
 * Thread-local storage for the current tenant ID.
 * Used by the Hibernate tenant filter to enforce multi-tenant data isolation.
 * 
 * The tenant ID is set by JwtAuthenticationFilter after extracting it from the JWT token.
 * It's cleared after each request to prevent leakage between requests.
 */
public class TenantContext {
    
    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    /**
     * Set the current tenant ID for this thread.
     * 
     * @param tenantId the tenant ID to set
     */
    public static void setTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    /**
     * Get the current tenant ID for this thread.
     * 
     * @return the current tenant ID, or null if not set
     */
    public static Long getTenantId() {
        return currentTenantId.get();
    }

    /**
     * Clear the current tenant ID for this thread.
     * Should be called after each request to prevent leakage.
     */
    public static void clear() {
        currentTenantId.remove();
    }
}
