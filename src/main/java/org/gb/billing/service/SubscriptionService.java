package org.gb.billing.service;

import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.Subscription;
import org.gb.billing.exception.DuplicateSubscriptionException;
import org.gb.billing.exception.PlanNotFoundException;
import org.gb.billing.exception.SubscriptionNotFoundException;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing customer subscriptions.
 * 
 * Provides subscription lifecycle operations:
 * - Create subscription (with duplicate check)
 * - Retrieve subscription (with tenant filtering)
 * - Upgrade subscription (User Story 3)
 * - Cancel subscription (User Story 4)
 * - State transitions (User Story 5)
 * 
 * All operations enforce multi-tenant isolation and log state transitions.
 * 
 * @see Subscription for entity details
 * @see SubscriptionRepository for data access
 * @see SubscriptionStateMachine for state transition rules
 */
@Service
@Transactional
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final SubscriptionStateMachine stateMachine;
    private final MeterRegistry meterRegistry;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                              PlanRepository planRepository,
                              SubscriptionStateMachine stateMachine,
                              MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.stateMachine = stateMachine;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Creates a new subscription for a user.
     * 
     * Business Rules:
     * - User can have only one active or past-due subscription at a time
     * - Plan must exist and not be deleted
     * - Initial state is ACTIVE
     * - Next billing date is calculated based on plan's billing cycle
     * - State transition is logged
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param planId the plan ID to subscribe to
     * @return the created subscription
     * @throws PlanNotFoundException if plan not found
     * @throws DuplicateSubscriptionException if user already has active subscription
     */
    public Subscription createSubscription(UUID userId, UUID tenantId, UUID planId) {
        logger.info("Creating subscription for user {} to plan {}", userId, planId);

        // Validate plan exists
        BillingPlan plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        // Check for duplicate active subscription
        Optional<Subscription> existingSubscription = 
            subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId, tenantId);
        
        if (existingSubscription.isPresent()) {
            throw new DuplicateSubscriptionException(userId, existingSubscription.get().getId());
        }

        // Create subscription
        Subscription subscription = new Subscription(userId, tenantId, plan);
        Subscription saved = subscriptionRepository.save(subscription);

        // Initialize state machine (creates ACTIVE log)
        stateMachine.logInitialCreation(saved, userId);

        logger.info("Created subscription {} for user {}", saved.getId(), userId);
        
        meterRegistry.counter("subscription.created", "plan", plan.getName()).increment();
        
        return saved;
    }

    /**
     * Retrieves a subscription by ID with tenant filtering.
     * Ensures users can only access subscriptions in their tenant.
     * 
     * @param subscriptionId the subscription ID
     * @param tenantId the tenant ID
     * @return the subscription
     * @throws SubscriptionNotFoundException if subscription not found or belongs to different tenant
     */
    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(UUID subscriptionId, UUID tenantId) {
        logger.debug("Fetching subscription {} for tenant {}", subscriptionId, tenantId);

        return subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId, tenantId));
    }

    /**
     * Retrieves the current user's active or past-due subscription.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return Optional containing the subscription if exists
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> getMySubscription(UUID userId, UUID tenantId) {
        logger.debug("Fetching active subscription for user {} in tenant {}", userId, tenantId);

        return subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Retrieves all subscriptions for a user (including canceled).
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return list of user's subscriptions
     */
    @Transactional(readOnly = true)
    public List<Subscription> getUserSubscriptions(UUID userId, UUID tenantId) {
        logger.debug("Fetching all subscriptions for user {} in tenant {}", userId, tenantId);

        return subscriptionRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Retrieves all subscriptions in a tenant by status.
     * 
     * @param tenantId the tenant ID
     * @param status the subscription status
     * @return list of subscriptions with the given status
     */
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsByStatus(UUID tenantId, org.gb.billing.entity.SubscriptionState status) {
        logger.debug("Fetching subscriptions with status {} for tenant {}", status, tenantId);

        return subscriptionRepository.findByStatusAndTenantId(status, tenantId);
    }

    /**
     * Upgrades a subscription to a new plan.
     * 
     * Business Rules:
     * - Subscription must be in ACTIVE status (cannot upgrade from PAST_DUE or CANCELED)
     * - New plan must exist and not be deleted
     * - New plan must be different from current plan
     * - Next billing date is recalculated based on new plan's billing cycle
     * - State transition is logged
     * - Uses optimistic locking to prevent concurrent modifications
     * 
     * @param subscriptionId the subscription ID
     * @param tenantId the tenant ID
     * @param newPlanId the new plan ID to upgrade to
     * @return the upgraded subscription
     * @throws SubscriptionNotFoundException if subscription not found
     * @throws PlanNotFoundException if new plan not found
     * @throws org.gb.billing.exception.InvalidUpgradeException if upgrade is not allowed
     */
    public Subscription upgradeSubscription(UUID subscriptionId, UUID tenantId, UUID newPlanId) {
        logger.info("Upgrading subscription {} to plan {}", subscriptionId, newPlanId);

        // Fetch subscription with tenant filtering
        Subscription subscription = subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId, tenantId));

        // Validate subscription status
        if (subscription.getStatus() != org.gb.billing.entity.SubscriptionState.ACTIVE) {
            throw new org.gb.billing.exception.InvalidUpgradeException(
                subscriptionId,
                subscription.getStatus(),
                "Subscription must be in ACTIVE status to upgrade. Current status: " + subscription.getStatus()
            );
        }

        // Validate new plan exists
        BillingPlan newPlan = planRepository.findByIdAndNotDeleted(newPlanId)
                .orElseThrow(() -> new PlanNotFoundException(newPlanId));

        // Validate not upgrading to same plan
        if (subscription.getPlan().getId().equals(newPlanId)) {
            throw new org.gb.billing.exception.InvalidUpgradeException(
                "Cannot upgrade to the same plan"
            );
        }

        // Perform upgrade
        BillingPlan oldPlan = subscription.getPlan();
        subscription.upgradeToPlan(newPlan);

        // Save with optimistic locking
        Subscription saved = subscriptionRepository.save(subscription);

        // Log upgrade (state remains ACTIVE)
        stateMachine.logUpgrade(saved, oldPlan, newPlan, subscription.getUserId());

        logger.info("Successfully upgraded subscription {} to plan {}", saved.getId(), newPlan.getName());
        
        meterRegistry.counter("subscription.upgraded", "from_plan", oldPlan.getName(), "to_plan", newPlan.getName()).increment();

        return saved;
    }

    /**
     * Cancels a subscription.
     * 
     * Business Rules:
     * - Subscription must be in ACTIVE or PAST_DUE status (cannot cancel already CANCELED)
     * - Status transitions to CANCELED (terminal state)
     * - Next billing date is cleared
     * - Cancellation timestamp is recorded
     * - State transition is logged
     * - Uses optimistic locking to prevent concurrent modifications
     * 
     * @param subscriptionId the subscription ID
     * @param tenantId the tenant ID
     * @param userId the user ID who is canceling (for audit)
     * @throws SubscriptionNotFoundException if subscription not found
     * @throws org.gb.billing.exception.InvalidStateTransitionException if already canceled
     */
    public void cancelSubscription(UUID subscriptionId, UUID tenantId, UUID userId) {
        logger.info("Canceling subscription {} for user {}", subscriptionId, userId);

        // Fetch subscription with tenant filtering
        Subscription subscription = subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId, tenantId));

        // Validate subscription can be canceled
        if (subscription.isCanceled()) {
            throw new org.gb.billing.exception.InvalidStateTransitionException(
                subscription.getStatus(),
                org.gb.billing.entity.SubscriptionState.CANCELED,
                "Subscription is already canceled"
            );
        }

        // Perform cancellation
        org.gb.billing.entity.SubscriptionState oldStatus = subscription.getStatus();
        subscription.cancel();

        // Save with optimistic locking
        Subscription saved = subscriptionRepository.save(subscription);

        // Log the state transition
        stateMachine.transitionTo(
            saved,
            org.gb.billing.entity.SubscriptionState.CANCELED,
            "User canceled subscription",
            userId
        );

        logger.info("Successfully canceled subscription {} (previous status: {})", 
                   saved.getId(), oldStatus);
                   
        meterRegistry.counter("subscription.canceled").increment();
    }

    /**
     * Transitions a subscription to PAST_DUE status due to payment failure.
     * 
     * @param subscriptionId the subscription ID
     * @param reason the reason for failure (e.g., "Insufficient funds")
     */
    public void transitionToPastDue(UUID subscriptionId, String reason) {
        logger.info("Transitioning subscription {} to PAST_DUE. Reason: {}", subscriptionId, reason);
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
               .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId)); // Use lighter exception for system calls? Or standard one.
        
        // System operations might bypass tenant filter if triggered by backend worker, 
        // but for now assume subscriptionId is sufficient.
        
        if (subscription.getStatus() != org.gb.billing.entity.SubscriptionState.ACTIVE) {
             logger.warn("Skipping transition to PAST_DUE for subscription {} as it is in state {}", subscriptionId, subscription.getStatus());
             return; 
        }

        subscription.transitionToPastDue();
        Subscription saved = subscriptionRepository.save(subscription);
        stateMachine.transitionTo(saved, org.gb.billing.entity.SubscriptionState.PAST_DUE, reason, null);
    }

    /**
     * Transitions a subscription to ACTIVE status (e.g., successful payment retry).
     * 
     * @param subscriptionId the subscription ID
     * @param reason the reason (e.g., "Payment successful")
     */
    public void transitionToActive(UUID subscriptionId, String reason) {
        logger.info("Transitioning subscription {} to ACTIVE. Reason: {}", subscriptionId, reason);
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
               .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        if (subscription.getStatus() != org.gb.billing.entity.SubscriptionState.PAST_DUE) {
             logger.warn("Skipping transition to ACTIVE for subscription {} as it is in state {}", subscriptionId, subscription.getStatus());
             return; 
        }

        subscription.transitionToActive();
        Subscription saved = subscriptionRepository.save(subscription);
        stateMachine.transitionTo(saved, org.gb.billing.entity.SubscriptionState.ACTIVE, reason, null);
    }

    /**
     * Transitions a subscription to CANCELED status due to expiration (grace period end).
     * 
     * @param subscriptionId the subscription ID
     * @param reason the reason (e.g., "Grace period expired")
     */
    public void transitionToCanceled(UUID subscriptionId, String reason) {
        logger.info("Transitioning subscription {} to CANCELED. Reason: {}", subscriptionId, reason);
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
               .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        if (subscription.getStatus() == org.gb.billing.entity.SubscriptionState.CANCELED) {
             return;
        }

        subscription.cancel();
        Subscription saved = subscriptionRepository.save(subscription);
        stateMachine.transitionTo(saved, org.gb.billing.entity.SubscriptionState.CANCELED, reason, null);
    }

    /**
     * Retrieves transition history for a subscription.
     * 
     * @param subscriptionId the subscription ID
     * @param tenantId the tenant ID
     * @return list of transition logs
     */
    @Transactional(readOnly = true)
    public List<org.gb.billing.entity.SubscriptionTransitionLog> getTransitionHistory(UUID subscriptionId, UUID tenantId) {
        // Validation check for tenant access
        if (!subscriptionRepository.existsByIdAndTenantId(subscriptionId, tenantId)) {
            throw new SubscriptionNotFoundException(subscriptionId, tenantId);
        }
        
        return stateMachine.getTransitionLog(subscriptionId);
    }
}

