package org.gb.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for creating a new billing plan.
 * 
 * Validation Rules:
 * - Name: required, 3-100 characters
 * - Description: optional, max 500 characters
 * - Price: required, >= 0
 * - Billing cycle: required, must be MONTHLY or YEARLY
 * - Feature limits: optional list
 */
public class CreatePlanRequest {

    @NotBlank(message = "Plan name is required")
    @Size(min = 3, max = 100, message = "Plan name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s-]+$", message = "Plan name can only contain letters, numbers, spaces, and hyphens")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 4, message = "Price must have at most 10 integer digits and 4 decimal places")
    private BigDecimal price;

    @NotBlank(message = "Billing cycle is required")
    @Pattern(regexp = "MONTHLY|YEARLY", message = "Billing cycle must be MONTHLY or YEARLY")
    private String billingCycle;

    private Boolean isActive = true;

    @Valid
    private List<FeatureLimitRequest> featureLimits = new ArrayList<>();

    // Constructors
    public CreatePlanRequest() {
    }

    public CreatePlanRequest(String name, String description, BigDecimal price, String billingCycle) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.billingCycle = billingCycle;
    }

    // Getters and Setters
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

    public List<FeatureLimitRequest> getFeatureLimits() {
        return featureLimits;
    }

    public void setFeatureLimits(List<FeatureLimitRequest> featureLimits) {
        this.featureLimits = featureLimits;
    }
}
