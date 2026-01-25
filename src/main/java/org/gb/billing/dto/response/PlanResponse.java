package org.gb.billing.dto.response;

import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for billing plan details.
 * 
 * Contains complete plan information including feature limits.
 */
public class PlanResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String billingCycle;
    private Boolean isActive;
    private List<FeatureLimitResponse> featureLimits;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public PlanResponse() {
    }

    /**
     * Creates a PlanResponse from a BillingPlan entity.
     * 
     * @param plan the billing plan entity
     * @return PlanResponse DTO
     */
    public static PlanResponse fromEntity(BillingPlan plan) {
        PlanResponse response = new PlanResponse();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setPrice(plan.getPrice());
        response.setBillingCycle(plan.getBillingCycle().name());
        response.setIsActive(plan.getIsActive());
        response.setCreatedAt(plan.getCreatedAt());
        response.setUpdatedAt(plan.getUpdatedAt());
        
        if (plan.getFeatureLimits() != null) {
            response.setFeatureLimits(
                plan.getFeatureLimits().stream()
                    .map(FeatureLimitResponse::fromEntity)
                    .collect(Collectors.toList())
            );
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<FeatureLimitResponse> getFeatureLimits() {
        return featureLimits;
    }

    public void setFeatureLimits(List<FeatureLimitResponse> featureLimits) {
        this.featureLimits = featureLimits;
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
