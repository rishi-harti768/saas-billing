package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RevenueByPlan {
    private UUID planId;
    private String planName;
    private BigDecimal revenueAmount;
    private BigDecimal percentageOfTotal;

    public RevenueByPlan() {}

    public RevenueByPlan(UUID planId, String planName, BigDecimal revenueAmount, BigDecimal percentageOfTotal) {
        this.planId = planId;
        this.planName = planName;
        this.revenueAmount = revenueAmount;
        this.percentageOfTotal = percentageOfTotal;
    }

    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public BigDecimal getRevenueAmount() { return revenueAmount; }
    public void setRevenueAmount(BigDecimal revenueAmount) { this.revenueAmount = revenueAmount; }
    public BigDecimal getPercentageOfTotal() { return percentageOfTotal; }
    public void setPercentageOfTotal(BigDecimal percentageOfTotal) { this.percentageOfTotal = percentageOfTotal; }
}
