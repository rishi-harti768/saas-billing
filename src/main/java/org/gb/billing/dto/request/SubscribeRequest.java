package org.gb.billing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for subscribing to a billing plan.
 * 
 * Validation Rules:
 * - Plan ID: required
 * 
 * User ID and Tenant ID are extracted from JWT token (security context).
 */
public class SubscribeRequest {

    @NotNull(message = "Plan ID is required")
    private UUID planId;

    // Constructors
    public SubscribeRequest() {
    }

    public SubscribeRequest(UUID planId) {
        this.planId = planId;
    }

    // Getters and Setters
    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }
}
