package org.gb.billing.service;

import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.SubscriptionState;
import org.gb.billing.entity.SubscriptionTransitionLog;
import org.gb.billing.exception.InvalidStateTransitionException;
import org.gb.billing.repository.SubscriptionTransitionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Subscription state machine service for managing valid state transitions.
 * 
 * Valid State Transitions:
 * - ACTIVE → PAST_DUE (payment failure)
 * - ACTIVE → CANCELED (user cancellation)
 * - PAST_DUE → ACTIVE (successful payment retry)
 * - PAST_DUE → CANCELED (grace period expired or user cancellation)
 * - CANCELED → (terminal state, no transitions allowed)
 * 
 * All transitions are logged to SubscriptionTransitionLog for audit trail.
 * 
 * @see Subscription for subscription entity
 * @see SubscriptionState for state enum
 * @see SubscriptionTransitionLog for audit log
 */
@Service
public class SubscriptionStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionStateMachine.class);

    private final SubscriptionTransitionLogRepository transitionLogRepository;

    // Define valid state transitions
    private static final Map<SubscriptionState, Set<SubscriptionState>> VALID_TRANSITIONS = Map.of(
        SubscriptionState.ACTIVE, EnumSet.of(SubscriptionState.PAST_DUE, SubscriptionState.CANCELED),
        SubscriptionState.PAST_DUE, EnumSet.of(SubscriptionState.ACTIVE, SubscriptionState.CANCELED),
        SubscriptionState.CANCELED, EnumSet.noneOf(SubscriptionState.class) // Terminal state
    );

    public SubscriptionStateMachine(SubscriptionTransitionLogRepository transitionLogRepository) {
        this.transitionLogRepository = transitionLogRepository;
    }

    /**
     * Validates if a state transition is allowed.
     * 
     * @param fromState the current state
     * @param toState the target state
     * @throws InvalidStateTransitionException if transition is not allowed
     */
    public void validateTransition(SubscriptionState fromState, SubscriptionState toState) {
        if (fromState == null) {
            throw new IllegalArgumentException("Current state cannot be null");
        }
        if (toState == null) {
            throw new IllegalArgumentException("Target state cannot be null");
        }

        Set<SubscriptionState> allowedTransitions = VALID_TRANSITIONS.get(fromState);
        
        if (allowedTransitions == null || !allowedTransitions.contains(toState)) {
            throw new InvalidStateTransitionException(fromState, toState);
        }
    }

    /**
     * Checks if a state transition is valid without throwing an exception.
     * 
     * @param fromState the current state
     * @param toState the target state
     * @return true if transition is valid, false otherwise
     */
    public boolean isValidTransition(SubscriptionState fromState, SubscriptionState toState) {
        if (fromState == null || toState == null) {
            return false;
        }

        Set<SubscriptionState> allowedTransitions = VALID_TRANSITIONS.get(fromState);
        return allowedTransitions != null && allowedTransitions.contains(toState);
    }

    /**
     * Transitions a subscription to a new state with logging.
     * 
     * @param subscription the subscription to transition
     * @param toState the target state
     * @param reason the reason for transition
     * @param transitionedBy the user ID who triggered the transition (null for system)
     * @throws InvalidStateTransitionException if transition is not allowed
     */
    @Transactional
    public void transitionTo(Subscription subscription, SubscriptionState toState, 
                            String reason, Long transitionedBy) {
        SubscriptionState fromState = subscription.getStatus();
        
        // Validate transition
        validateTransition(fromState, toState);
        
        // Update subscription state
        subscription.setStatus(toState);
        
        // Log transition
        SubscriptionTransitionLog log = new SubscriptionTransitionLog(
            subscription.getId(),
            fromState,
            toState,
            reason,
            transitionedBy
        );
        transitionLogRepository.save(log);
        
        logger.info("Subscription {} transitioned from {} to {} - Reason: {}", 
                   subscription.getId(), fromState, toState, reason);
    }

    /**
     * Logs the initial subscription creation.
     * 
     * @param subscription the newly created subscription
     * @param createdBy the user ID who created the subscription
     */
    @Transactional
    public void logInitialCreation(Subscription subscription, Long createdBy) {
        SubscriptionTransitionLog log = new SubscriptionTransitionLog(
            subscription.getId(),
            null, // fromStatus is null for initial creation
            subscription.getStatus(),
            "Subscription created",
            createdBy
        );
        transitionLogRepository.save(log);
        
        logger.info("Subscription {} created with initial state: {}", 
                   subscription.getId(), subscription.getStatus());
    }

    /**
     * Gets all allowed transitions from a given state.
     * 
     * @param fromState the current state
     * @return set of allowed target states
     */
    public Set<SubscriptionState> getAllowedTransitions(SubscriptionState fromState) {
        return VALID_TRANSITIONS.getOrDefault(fromState, EnumSet.noneOf(SubscriptionState.class));
    }

    /**
     * Checks if a state is terminal (no outgoing transitions).
     * 
     * @param state the state to check
     * @return true if state is terminal, false otherwise
     */
    public boolean isTerminalState(SubscriptionState state) {
        return state == SubscriptionState.CANCELED;
    }

    /**
     * Logs a subscription upgrade.
     * 
     * @param subscription the upgraded subscription
     * @param fromPlan the previous plan
     * @param toPlan the new plan
     */
    @Transactional
    public void logUpgrade(Subscription subscription, org.gb.billing.entity.BillingPlan fromPlan, org.gb.billing.entity.BillingPlan toPlan, Long userId) {
        SubscriptionTransitionLog log = new SubscriptionTransitionLog(
            subscription.getId(),
            subscription.getStatus(),
            subscription.getStatus(),
            String.format("Upgrade from %s to %s", fromPlan.getName(), toPlan.getName()),
            userId
        );
        transitionLogRepository.save(log);
        
        logger.info("Subscription {} upgraded from plan {} to {}", 
                   subscription.getId(), fromPlan.getName(), toPlan.getName());
    }

    /**
     * Retrieves transition log for a subscription.
     * 
     * @param subscriptionId the subscription ID
     * @return list of transition logs
     */
    public java.util.List<SubscriptionTransitionLog> getTransitionLog(UUID subscriptionId) {
        return transitionLogRepository.findBySubscriptionIdOrderByTransitionedAtDesc(subscriptionId);
    }
}
