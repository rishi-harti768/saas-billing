package org.gb.billing.dto.response;

import org.gb.billing.entity.FeatureLimit;

import java.util.UUID;

/**
 * Response DTO for feature limit details.
 */
public class FeatureLimitResponse {

    private UUID id;
    private String limitType;
    private Integer limitValue;
    private Boolean isUnlimited;

    // Constructors
    public FeatureLimitResponse() {
    }

    public FeatureLimitResponse(UUID id, String limitType, Integer limitValue) {
        this.id = id;
        this.limitType = limitType;
        this.limitValue = limitValue;
        this.isUnlimited = limitValue == -1;
    }

    /**
     * Creates a FeatureLimitResponse from a FeatureLimit entity.
     * 
     * @param limit the feature limit entity
     * @return FeatureLimitResponse DTO
     */
    public static FeatureLimitResponse fromEntity(FeatureLimit limit) {
        FeatureLimitResponse response = new FeatureLimitResponse();
        response.setId(limit.getId());
        response.setLimitType(limit.getLimitType());
        response.setLimitValue(limit.getLimitValue());
        response.setIsUnlimited(limit.isUnlimited());
        return response;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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
        this.isUnlimited = limitValue == -1;
    }

    public Boolean getIsUnlimited() {
        return isUnlimited;
    }

    public void setIsUnlimited(Boolean isUnlimited) {
        this.isUnlimited = isUnlimited;
    }
}
