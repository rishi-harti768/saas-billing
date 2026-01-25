package org.gb.billing.dto.response;

import org.gb.billing.entity.BillingPlan;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Summary DTO for billing plan information within subscription responses.
 * 
 * Contains essential plan details without full feature limit information.
 */
public class PlanSummary {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String billingCycle;

    // Constructors
    public PlanSummary() {
    }

    public PlanSummary(UUID id, String name, String description, BigDecimal price, String billingCycle) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.billingCycle = billingCycle;
    }

    /**
     * Creates a PlanSummary from a BillingPlan entity.
     * 
     * @param plan the billing plan entity
     * @return PlanSummary DTO
     */
    public static PlanSummary fromEntity(BillingPlan plan) {
        PlanSummary summary = new PlanSummary();
        summary.setId(plan.getId());
        summary.setName(plan.getName());
        summary.setDescription(plan.getDescription());
        summary.setPrice(plan.getPrice());
        summary.setBillingCycle(plan.getBillingCycle().name());
        return summary;
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
}
