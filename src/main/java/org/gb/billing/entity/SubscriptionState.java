package org.gb.billing.entity;

/**
 * Subscription state enum representing the lifecycle status of a subscription.
 * 
 * State Transition Rules (enforced by SubscriptionStateMachine):
 * - ACTIVE → PAST_DUE (payment failure)
 * - ACTIVE → CANCELED (user cancellation)
 * - PAST_DUE → ACTIVE (successful payment retry)
 * - PAST_DUE → CANCELED (grace period expired or user cancellation)
 * - CANCELED → (terminal state, no transitions allowed)
 * 
 * @see org.gb.billing.service.SubscriptionStateMachine for transition validation
 */
public enum SubscriptionState {
    /**
     * Active subscription with successful payments.
     * Customer has full access to plan features.
     */
    ACTIVE,
    
    /**
     * Subscription with failed payment.
     * Customer may have limited access during grace period.
     * Will transition to CANCELED if payment not resolved within grace period.
     */
    PAST_DUE,
    
    /**
     * Canceled subscription (terminal state).
     * Customer loses access to plan features.
     * No further state transitions allowed.
     */
    CANCELED
}
