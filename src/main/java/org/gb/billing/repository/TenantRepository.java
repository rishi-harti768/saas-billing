package org.gb.billing.repository;

import org.gb.billing.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Tenant entity.
 * Provides data access methods for tenant management.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    /**
     * Find a tenant by its name.
     * 
     * @param name the tenant name to search for
     * @return an Optional containing the tenant if found, empty otherwise
     */
    Optional<Tenant> findByName(String name);
}
