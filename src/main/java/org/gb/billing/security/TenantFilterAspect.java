package org.gb.billing.security;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect to automatically enable Hibernate tenant filter before repository method execution.
 * Ensures multi-tenant data isolation by filtering queries based on the current tenant ID.
 */
@Aspect
@Component
public class TenantFilterAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantFilterAspect.class);
    private static final String TENANT_FILTER_NAME = "tenantFilter";
    
    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Enable tenant filter before any repository method execution.
     * Only applies the filter if a tenant ID is present in the TenantContext.
     */
    @Before("execution(* org.gb.billing.repository.*.*(..))")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.getTenantId();
        
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            org.hibernate.Filter filter = session.enableFilter(TENANT_FILTER_NAME);
            filter.setParameter("tenantId", tenantId);
            
            logger.debug("Enabled tenant filter with tenantId: {}", tenantId);
        } else {
            logger.debug("No tenant ID in context, skipping tenant filter (likely admin user)");
        }
    }
}
