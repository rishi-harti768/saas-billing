package org.gb.billing.service;

import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.FeatureLimit;
import org.gb.billing.exception.PlanHasActiveSubscriptionsException;
import org.gb.billing.exception.PlanNotFoundException;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing billing plans.
 * 
 * Provides CRUD operations with caching, validation, and business rule enforcement:
 * - Plans are cached for performance (read-heavy workload)
 * - Soft delete prevents data loss
 * - Plans with active subscriptions cannot be deleted
 * - All operations are transactional
 * 
 * @see BillingPlan for entity details
 * @see PlanRepository for data access
 */
@Service
@Transactional
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public PlanService(PlanRepository planRepository, SubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Creates a new billing plan with feature limits.
     * 
     * @param plan the plan to create
     * @return the created plan
     * @throws IllegalArgumentException if plan name already exists
     */
    @CacheEvict(value = "plans", allEntries = true)
    public BillingPlan createPlan(BillingPlan plan) {
        logger.info("Creating new billing plan: {}", plan.getName());

        // Validate unique plan name
        if (planRepository.existsByNameAndNotDeleted(plan.getName())) {
            throw new IllegalArgumentException("Plan with name '" + plan.getName() + "' already exists");
        }

        BillingPlan saved = planRepository.save(plan);
        logger.info("Successfully created plan: {} (ID: {})", saved.getName(), saved.getId());
        
        return saved;
    }

    /**
     * Retrieves all non-deleted plans.
     * Results are cached for 10 minutes.
     * 
     * @return list of all active plans
     */
    @Cacheable(value = "plans", key = "'all'", sync = true)
    @Transactional(readOnly = true)
    public List<BillingPlan> getAllPlans() {
        logger.debug("Fetching all billing plans");
        return planRepository.findAllNonDeleted();
    }

    /**
     * Retrieves a plan by ID.
     * Results are cached for 10 minutes.
     * 
     * @param id the plan ID
     * @return the billing plan
     * @throws PlanNotFoundException if plan not found
     */
    @Cacheable(value = "plans", key = "#id")
    @Transactional(readOnly = true)
    public BillingPlan getPlanById(UUID id) {
        logger.debug("Fetching plan by ID: {}", id);
        
        return planRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new PlanNotFoundException(id));
    }

    /**
     * Updates an existing billing plan.
     * Evicts cache to ensure fresh data.
     * 
     * @param id the plan ID
     * @param updates the plan updates
     * @return the updated plan
     * @throws PlanNotFoundException if plan not found
     */
    @CacheEvict(value = "plans", allEntries = true)
    public BillingPlan updatePlan(UUID id, BillingPlan updates) {
        logger.info("Updating plan: {}", id);

        BillingPlan existing = planRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new PlanNotFoundException(id));

        // Update fields
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getPrice() != null) {
            existing.setPrice(updates.getPrice());
        }
        if (updates.getIsActive() != null) {
            existing.setIsActive(updates.getIsActive());
        }

        // Update feature limits if provided
        if (updates.getFeatureLimits() != null) {
            existing.getFeatureLimits().clear();
            for (FeatureLimit limit : updates.getFeatureLimits()) {
                existing.addFeatureLimit(limit);
            }
        }

        BillingPlan saved = planRepository.save(existing);
        logger.info("Successfully updated plan: {} (ID: {})", saved.getName(), saved.getId());
        
        return saved;
    }

    /**
     * Soft deletes a billing plan.
     * Prevents deletion if active subscriptions exist.
     * Evicts cache to ensure fresh data.
     * 
     * @param id the plan ID
     * @throws PlanNotFoundException if plan not found
     * @throws PlanHasActiveSubscriptionsException if active subscriptions exist
     */
    @CacheEvict(value = "plans", allEntries = true)
    public void deletePlan(UUID id) {
        logger.info("Deleting plan: {}", id);

        BillingPlan plan = planRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new PlanNotFoundException(id));

        // Check for active subscriptions
        long activeSubscriptions = subscriptionRepository.countActiveSubscriptionsByPlanId(id);
        if (activeSubscriptions > 0) {
            throw new PlanHasActiveSubscriptionsException(id, activeSubscriptions);
        }

        // Soft delete
        plan.softDelete();
        planRepository.save(plan);
        
        logger.info("Successfully soft-deleted plan: {} (ID: {})", plan.getName(), plan.getId());
    }

    /**
     * Adds a feature limit to a plan.
     * 
     * @param planId the plan ID
     * @param limit the feature limit to add
     * @return the updated plan
     * @throws PlanNotFoundException if plan not found
     */
    @CacheEvict(value = "plans", allEntries = true)
    public BillingPlan addFeatureLimit(UUID planId, FeatureLimit limit) {
        logger.info("Adding feature limit {} to plan: {}", limit.getLimitType(), planId);

        BillingPlan plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        plan.addFeatureLimit(limit);
        BillingPlan saved = planRepository.save(plan);
        
        logger.info("Successfully added feature limit to plan: {}", planId);
        return saved;
    }

    /**
     * Retrieves only active plans (isActive = true).
     * 
     * @return list of active plans
     */
    @Cacheable(value = "plans", key = "'active'", sync = true)
    @Transactional(readOnly = true)
    public List<BillingPlan> getActivePlans() {
        logger.debug("Fetching active billing plans");
        return planRepository.findAllActive();
    }
}
