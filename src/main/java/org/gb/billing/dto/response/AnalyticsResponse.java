package org.gb.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for subscription analytics dashboard.
 * 
 * Provides comprehensive metrics for business intelligence:
 * - Total subscription counts by status
 * - Active subscriptions by plan
 * - Revenue metrics
 * - Churn rate
 */
public class AnalyticsResponse {

    private TenantMetrics tenantMetrics;
    private List<PlanMetrics> planMetrics;
    private RevenueMetrics revenueMetrics;

    // Constructors
    public AnalyticsResponse() {
    }

    public AnalyticsResponse(TenantMetrics tenantMetrics, List<PlanMetrics> planMetrics, RevenueMetrics revenueMetrics) {
        this.tenantMetrics = tenantMetrics;
        this.planMetrics = planMetrics;
        this.revenueMetrics = revenueMetrics;
    }

    // Getters and Setters
    public TenantMetrics getTenantMetrics() {
        return tenantMetrics;
    }

    public void setTenantMetrics(TenantMetrics tenantMetrics) {
        this.tenantMetrics = tenantMetrics;
    }

    public List<PlanMetrics> getPlanMetrics() {
        return planMetrics;
    }

    public void setPlanMetrics(List<PlanMetrics> planMetrics) {
        this.planMetrics = planMetrics;
    }

    public RevenueMetrics getRevenueMetrics() {
        return revenueMetrics;
    }

    public void setRevenueMetrics(RevenueMetrics revenueMetrics) {
        this.revenueMetrics = revenueMetrics;
    }

    /**
     * Tenant-level subscription metrics.
     */
    public static class TenantMetrics {
        private Long totalSubscriptions;
        private Long activeSubscriptions;
        private Long pastDueSubscriptions;
        private Long canceledSubscriptions;
        private Double churnRate;

        public TenantMetrics() {
        }

        public TenantMetrics(Long totalSubscriptions, Long activeSubscriptions, Long pastDueSubscriptions, Long canceledSubscriptions) {
            this.totalSubscriptions = totalSubscriptions;
            this.activeSubscriptions = activeSubscriptions;
            this.pastDueSubscriptions = pastDueSubscriptions;
            this.canceledSubscriptions = canceledSubscriptions;
            
            // Calculate churn rate
            if (totalSubscriptions > 0) {
                this.churnRate = (canceledSubscriptions.doubleValue() / totalSubscriptions.doubleValue()) * 100;
            } else {
                this.churnRate = 0.0;
            }
        }

        // Getters and Setters
        public Long getTotalSubscriptions() {
            return totalSubscriptions;
        }

        public void setTotalSubscriptions(Long totalSubscriptions) {
            this.totalSubscriptions = totalSubscriptions;
        }

        public Long getActiveSubscriptions() {
            return activeSubscriptions;
        }

        public void setActiveSubscriptions(Long activeSubscriptions) {
            this.activeSubscriptions = activeSubscriptions;
        }

        public Long getPastDueSubscriptions() {
            return pastDueSubscriptions;
        }

        public void setPastDueSubscriptions(Long pastDueSubscriptions) {
            this.pastDueSubscriptions = pastDueSubscriptions;
        }

        public Long getCanceledSubscriptions() {
            return canceledSubscriptions;
        }

        public void setCanceledSubscriptions(Long canceledSubscriptions) {
            this.canceledSubscriptions = canceledSubscriptions;
        }

        public Double getChurnRate() {
            return churnRate;
        }

        public void setChurnRate(Double churnRate) {
            this.churnRate = churnRate;
        }
    }

    /**
     * Per-plan subscription metrics.
     */
    public static class PlanMetrics {
        private String planId;
        private String planName;
        private Long activeSubscriptions;
        private BigDecimal monthlyRecurringRevenue;
        private BigDecimal yearlyRecurringRevenue;

        public PlanMetrics() {
        }

        public PlanMetrics(String planId, String planName, Long activeSubscriptions, BigDecimal monthlyRecurringRevenue, BigDecimal yearlyRecurringRevenue) {
            this.planId = planId;
            this.planName = planName;
            this.activeSubscriptions = activeSubscriptions;
            this.monthlyRecurringRevenue = monthlyRecurringRevenue;
            this.yearlyRecurringRevenue = yearlyRecurringRevenue;
        }

        // Getters and Setters
        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public String getPlanName() {
            return planName;
        }

        public void setPlanName(String planName) {
            this.planName = planName;
        }

        public Long getActiveSubscriptions() {
            return activeSubscriptions;
        }

        public void setActiveSubscriptions(Long activeSubscriptions) {
            this.activeSubscriptions = activeSubscriptions;
        }

        public BigDecimal getMonthlyRecurringRevenue() {
            return monthlyRecurringRevenue;
        }

        public void setMonthlyRecurringRevenue(BigDecimal monthlyRecurringRevenue) {
            this.monthlyRecurringRevenue = monthlyRecurringRevenue;
        }

        public BigDecimal getYearlyRecurringRevenue() {
            return yearlyRecurringRevenue;
        }

        public void setYearlyRecurringRevenue(BigDecimal yearlyRecurringRevenue) {
            this.yearlyRecurringRevenue = yearlyRecurringRevenue;
        }
    }

    /**
     * Revenue metrics.
     */
    public static class RevenueMetrics {
        private BigDecimal totalMonthlyRecurringRevenue;
        private BigDecimal totalYearlyRecurringRevenue;
        private BigDecimal averageRevenuePerUser;

        public RevenueMetrics() {
        }

        public RevenueMetrics(BigDecimal totalMonthlyRecurringRevenue, BigDecimal totalYearlyRecurringRevenue, BigDecimal averageRevenuePerUser) {
            this.totalMonthlyRecurringRevenue = totalMonthlyRecurringRevenue;
            this.totalYearlyRecurringRevenue = totalYearlyRecurringRevenue;
            this.averageRevenuePerUser = averageRevenuePerUser;
        }

        // Getters and Setters
        public BigDecimal getTotalMonthlyRecurringRevenue() {
            return totalMonthlyRecurringRevenue;
        }

        public void setTotalMonthlyRecurringRevenue(BigDecimal totalMonthlyRecurringRevenue) {
            this.totalMonthlyRecurringRevenue = totalMonthlyRecurringRevenue;
        }

        public BigDecimal getTotalYearlyRecurringRevenue() {
            return totalYearlyRecurringRevenue;
        }

        public void setTotalYearlyRecurringRevenue(BigDecimal totalYearlyRecurringRevenue) {
            this.totalYearlyRecurringRevenue = totalYearlyRecurringRevenue;
        }

        public BigDecimal getAverageRevenuePerUser() {
            return averageRevenuePerUser;
        }

        public void setAverageRevenuePerUser(BigDecimal averageRevenuePerUser) {
            this.averageRevenuePerUser = averageRevenuePerUser;
        }
    }
}
