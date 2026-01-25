package org.gb.billing.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting to delete a plan that has active subscriptions.
 * Results in HTTP 409 (Conflict) response.
 * 
 * Business Rule: Plans with active or past-due subscriptions cannot be deleted.
 * Soft delete is used instead to maintain referential integrity.
 * 
 * @see org.gb.billing.service.PlanService
 */
public class PlanHasActiveSubscriptionsException extends RuntimeException {

    private final UUID planId;
    private final long activeSubscriptionCount;

    public PlanHasActiveSubscriptionsException(UUID planId, long activeSubscriptionCount) {
        super(String.format("Cannot delete plan %s: %d active subscriptions exist", 
                           planId, activeSubscriptionCount));
        this.planId = planId;
        this.activeSubscriptionCount = activeSubscriptionCount;
    }

    public UUID getPlanId() {
        return planId;
    }

    public long getActiveSubscriptionCount() {
        return activeSubscriptionCount;
    }
}
