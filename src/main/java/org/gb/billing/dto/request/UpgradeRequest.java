package org.gb.billing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for upgrading a subscription to a new plan.
 * 
 * Validation Rules:
 * - New Plan ID: required
 * 
 * Business Rules (enforced by service):
 * - Cannot upgrade from PAST_DUE status (payment must be resolved first)
 * - Cannot upgrade from CANCELED status (subscription is terminated)
 * - Cannot upgrade to the same plan (no change)
 * - New plan must be a higher tier (downgrades not supported in Phase 2)
 */
public class UpgradeRequest {

    @NotNull(message = "New plan ID is required")
    private UUID newPlanId;

    // Constructors
    public UpgradeRequest() {
    }

    public UpgradeRequest(UUID newPlanId) {
        this.newPlanId = newPlanId;
    }

    // Getters and Setters
    public UUID getNewPlanId() {
        return newPlanId;
    }

    public void setNewPlanId(UUID newPlanId) {
        this.newPlanId = newPlanId;
    }
}
