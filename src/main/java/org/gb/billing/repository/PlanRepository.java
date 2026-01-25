package org.gb.billing.repository;

import org.gb.billing.entity.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for BillingPlan entity operations.
 * 
 * Provides CRUD operations and custom queries for billing plan management.
 * Results are cached using @Cacheable annotations in PlanService.
 * 
 * @see BillingPlan for entity details
 * @see org.gb.billing.service.PlanService for business logic
 */
@Repository
public interface PlanRepository extends JpaRepository<BillingPlan, UUID> {

    /**
     * Finds a plan by name (case-sensitive).
     * 
     * @param name the plan name
     * @return Optional containing the plan if found
     */
    Optional<BillingPlan> findByName(String name);

    /**
     * Finds all non-deleted plans.
     * Used for listing available plans to customers.
     * 
     * @return list of active plans
     */
    @Query("SELECT p FROM BillingPlan p WHERE p.deleted = false")
    List<BillingPlan> findAllNonDeleted();

    /**
     * Finds all active (non-deleted and isActive=true) plans.
     * 
     * @return list of active plans
     */
    @Query("SELECT p FROM BillingPlan p WHERE p.deleted = false AND p.isActive = true")
    List<BillingPlan> findAllActive();

    /**
     * Checks if a plan exists by name (excluding deleted plans).
     * 
     * @param name the plan name
     * @return true if plan exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM BillingPlan p WHERE p.name = :name AND p.deleted = false")
    boolean existsByNameAndNotDeleted(String name);

    /**
     * Finds a non-deleted plan by ID.
     * 
     * @param id the plan ID
     * @return Optional containing the plan if found and not deleted
     */
    @Query("SELECT p FROM BillingPlan p WHERE p.id = :id AND p.deleted = false")
    Optional<BillingPlan> findByIdAndNotDeleted(UUID id);
}
