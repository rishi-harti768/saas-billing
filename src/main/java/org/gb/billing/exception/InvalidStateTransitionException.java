package org.gb.billing.exception;

import org.gb.billing.entity.SubscriptionState;

/**
 * Exception thrown when an invalid subscription state transition is attempted.
 * Results in HTTP 400 (Bad Request) response.
 * 
 * Valid transitions:
 * - ACTIVE → PAST_DUE (payment failure)
 * - ACTIVE → CANCELED (user cancellation)
 * - PAST_DUE → ACTIVE (successful payment)
 * - PAST_DUE → CANCELED (grace period expired or user cancellation)
 * - CANCELED → (no transitions allowed - terminal state)
 * 
 * @see org.gb.billing.service.SubscriptionStateMachine
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final SubscriptionState fromState;
    private final SubscriptionState toState;

    public InvalidStateTransitionException(SubscriptionState fromState, SubscriptionState toState) {
        super(String.format("Invalid state transition from %s to %s", fromState, toState));
        this.fromState = fromState;
        this.toState = toState;
    }

    public InvalidStateTransitionException(SubscriptionState fromState, SubscriptionState toState, String reason) {
        super(String.format("Invalid state transition from %s to %s: %s", fromState, toState, reason));
        this.fromState = fromState;
        this.toState = toState;
    }

    public SubscriptionState getFromState() {
        return fromState;
    }

    public SubscriptionState getToState() {
        return toState;
    }
}
