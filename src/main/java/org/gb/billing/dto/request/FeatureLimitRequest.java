package org.gb.billing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for feature limit within a billing plan.
 * 
 * Validation Rules:
 * - Limit type: required, 1-50 characters (e.g., "max_users", "max_storage_gb")
 * - Limit value: required, >= -1 (-1 means unlimited)
 */
public class FeatureLimitRequest {

    @NotBlank(message = "Limit type is required")
    @Size(min = 1, max = 50, message = "Limit type must be between 1 and 50 characters")
    private String limitType;

    @NotNull(message = "Limit value is required")
    @Min(value = -1, message = "Limit value must be -1 (unlimited) or greater")
    private Integer limitValue;

    // Constructors
    public FeatureLimitRequest() {
    }

    public FeatureLimitRequest(String limitType, Integer limitValue) {
        this.limitType = limitType;
        this.limitValue = limitValue;
    }

    // Getters and Setters
    public String getLimitType() {
        return limitType;
    }

    public void setLimitType(String limitType) {
        this.limitType = limitType;
    }

    public Integer getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Integer limitValue) {
        this.limitValue = limitValue;
    }
}
