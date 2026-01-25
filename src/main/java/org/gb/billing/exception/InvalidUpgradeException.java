package org.gb.billing.exception;

import org.gb.billing.entity.SubscriptionState;

import java.util.UUID;

/**
 * Exception thrown when an invalid subscription upgrade is attempted.
 * Results in HTTP 400 (Bad Request) response.
 * 
 * Invalid upgrade scenarios:
 * - Upgrading from PAST_DUE status (payment must be resolved first)
 * - Upgrading from CANCELED status (subscription is terminated)
 * - Upgrading to the same plan (no change)
 * - Upgrading to a lower-tier plan (downgrade not supported in Phase 2)
 * 
 * @see org.gb.billing.service.SubscriptionService
 */
public class InvalidUpgradeException extends RuntimeException {

    private final UUID subscriptionId;
    private final SubscriptionState currentState;

    public InvalidUpgradeException(UUID subscriptionId, SubscriptionState currentState, String reason) {
        super(String.format("Cannot upgrade subscription %s in state %s: %s", 
                           subscriptionId, currentState, reason));
        this.subscriptionId = subscriptionId;
        this.currentState = currentState;
    }

    public InvalidUpgradeException(String message) {
        super(message);
        this.subscriptionId = null;
        this.currentState = null;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public SubscriptionState getCurrentState() {
        return currentState;
    }
}
