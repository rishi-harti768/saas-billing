package org.gb.billing.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested subscription is not found.
 * Results in HTTP 404 (Not Found) response.
 * 
 * @see org.gb.billing.service.SubscriptionService
 */
public class SubscriptionNotFoundException extends RuntimeException {

    private final UUID subscriptionId;

    public SubscriptionNotFoundException(UUID subscriptionId) {
        super(String.format("Subscription not found with ID: %s", subscriptionId));
        this.subscriptionId = subscriptionId;
    }

    public SubscriptionNotFoundException(UUID subscriptionId, Long tenantId) {
        super(String.format("Subscription not found with ID: %s for tenant: %s", subscriptionId, tenantId));
        this.subscriptionId = subscriptionId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }
}
