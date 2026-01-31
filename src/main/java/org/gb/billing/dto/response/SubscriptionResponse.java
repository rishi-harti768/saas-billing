package org.gb.billing.dto.response;

import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.SubscriptionState;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for subscription details.
 * 
 * Contains subscription information including plan details and billing dates.
 */
public class SubscriptionResponse {

    private UUID id;
    private Long userId;
    private SubscriptionState status;
    private PlanSummary plan;
    private Instant startDate;
    private Instant nextBillingDate;
    private Instant canceledAt;
    private Long daysUntilNextBilling;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public SubscriptionResponse() {
    }

    /**
     * Creates a SubscriptionResponse from a Subscription entity.
     * 
     * @param subscription the subscription entity
     * @return SubscriptionResponse DTO
     */
    public static SubscriptionResponse fromEntity(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscription.getId());
        response.setUserId(subscription.getUserId());
        response.setStatus(subscription.getStatus());
        response.setStartDate(subscription.getStartDate());
        response.setNextBillingDate(subscription.getNextBillingDate());
        response.setCanceledAt(subscription.getCanceledAt());
        response.setCreatedAt(subscription.getCreatedAt());
        response.setUpdatedAt(subscription.getUpdatedAt());
        
        // Calculate days until next billing
        if (subscription.getNextBillingDate() != null) {
            response.setDaysUntilNextBilling(subscription.daysUntilNextBilling());
        }
        
        // Include plan summary
        if (subscription.getPlan() != null) {
            response.setPlan(PlanSummary.fromEntity(subscription.getPlan()));
        }
        
        return response;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SubscriptionState getStatus() {
        return status;
    }

    public void setStatus(SubscriptionState status) {
        this.status = status;
    }

    public PlanSummary getPlan() {
        return plan;
    }

    public void setPlan(PlanSummary plan) {
        this.plan = plan;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(Instant nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }

    public Long getDaysUntilNextBilling() {
        return daysUntilNextBilling;
    }

    public void setDaysUntilNextBilling(Long daysUntilNextBilling) {
        this.daysUntilNextBilling = daysUntilNextBilling;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
