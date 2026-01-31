package org.gb.billing.dto.response;

import java.util.UUID;

/**
 * DTO for subscription count by plan (analytics).
 */
public class SubscriptionCountByPlan {
    private UUID planId;
    private String planName;
    private long activeCount;
    
    // Constructor for JPA projection
    public SubscriptionCountByPlan(UUID planId, String planName, long activeCount) {
        this.planId = planId;
        this.planName = planName;
        this.activeCount = activeCount;
    }
    
    // Default constructor
    public SubscriptionCountByPlan() {}
    
    // Getters and setters
    public UUID getPlanId() { 
        return planId; 
    }
    
    public void setPlanId(UUID planId) { 
        this.planId = planId; 
    }
    
    public String getPlanName() { 
        return planName; 
    }
    
    public void setPlanName(String planName) { 
        this.planName = planName; 
    }
    
    public long getActiveCount() { 
        return activeCount; 
    }
    
    public void setActiveCount(long activeCount) { 
        this.activeCount = activeCount; 
    }
}
