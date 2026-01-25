package org.gb.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for updating an existing billing plan.
 * 
 * All fields are optional - only provided fields will be updated.
 * 
 * Validation Rules:
 * - Description: max 500 characters
 * - Price: if provided, must be >= 0
 * - Feature limits: optional list (replaces existing limits)
 */
public class UpdatePlanRequest {

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 4, message = "Price must have at most 10 integer digits and 4 decimal places")
    private BigDecimal price;

    private Boolean isActive;

    @Valid
    private List<FeatureLimitRequest> featureLimits;

    // Constructors
    public UpdatePlanRequest() {
    }

    // Getters and Setters
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
