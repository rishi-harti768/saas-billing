package org.gb.billing.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to create a duplicate active subscription.
 * Results in HTTP 409 (Conflict) response.
 * 
 * Business Rule: Users can have only one active or past-due subscription at a time.
 * 
 * @see org.gb.billing.service.SubscriptionService
 */
public class DuplicateSubscriptionException extends RuntimeException {

    private final Long userId;
    private final UUID existingSubscriptionId;

    public DuplicateSubscriptionException(Long userId, UUID existingSubscriptionId) {
        super(String.format("User %s already has an active subscription: %s", userId, existingSubscriptionId));
        this.userId = userId;
        this.existingSubscriptionId = existingSubscriptionId;
    }

    public Long getUserId() {
        return userId;
    }

    public UUID getExistingSubscriptionId() {
        return existingSubscriptionId;
    }
}
