package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionCountByPlan {
    private UUID planId;
    private String planName;
    private long activeCount;
    
    // Manual constructor for compatibility
    public SubscriptionCountByPlan(UUID planId, String planName, long activeCount) {
        this.planId = planId;
        this.planName = planName;
        this.activeCount = activeCount;
    }
    
    // Manual getters and setters
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    
    public long getActiveCount() { return activeCount; }
    public void setActiveCount(long activeCount) { this.activeCount = activeCount; }
}
