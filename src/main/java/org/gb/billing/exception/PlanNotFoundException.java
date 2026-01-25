package org.gb.billing.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested billing plan is not found.
 * Results in HTTP 404 (Not Found) response.
 * 
 * @see org.gb.billing.service.PlanService
 */
public class PlanNotFoundException extends RuntimeException {

    private final UUID planId;

    public PlanNotFoundException(UUID planId) {
        super(String.format("Billing plan not found with ID: %s", planId));
        this.planId = planId;
    }

    public PlanNotFoundException(String planName) {
        super(String.format("Billing plan not found with name: %s", planName));
        this.planId = null;
    }

    public UUID getPlanId() {
        return planId;
    }
}
