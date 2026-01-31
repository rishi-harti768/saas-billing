package org.gb.billing.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RevenueSummaryResponse {
    private BigDecimal monthlyRecurringRevenue; // MRR
    private BigDecimal annualRecurringRevenue;  // ARR
    private List<RevenueByPlan> revenueByPlan;

    public RevenueSummaryResponse() {}
    
    // Manual constructor to work around Lombok annotation processing issue
    public RevenueSummaryResponse(BigDecimal monthlyRecurringRevenue, BigDecimal annualRecurringRevenue, List<RevenueByPlan> revenueByPlan) {
        this.monthlyRecurringRevenue = monthlyRecurringRevenue;
        this.annualRecurringRevenue = annualRecurringRevenue;
        this.revenueByPlan = revenueByPlan;
    }

    public BigDecimal getMonthlyRecurringRevenue() { return monthlyRecurringRevenue; }
    public void setMonthlyRecurringRevenue(BigDecimal monthlyRecurringRevenue) { this.monthlyRecurringRevenue = monthlyRecurringRevenue; }
    public BigDecimal getAnnualRecurringRevenue() { return annualRecurringRevenue; }
    public void setAnnualRecurringRevenue(BigDecimal annualRecurringRevenue) { this.annualRecurringRevenue = annualRecurringRevenue; }
    public List<RevenueByPlan> getRevenueByPlan() { return revenueByPlan; }
    public void setRevenueByPlan(List<RevenueByPlan> revenueByPlan) { this.revenueByPlan = revenueByPlan; }
}
