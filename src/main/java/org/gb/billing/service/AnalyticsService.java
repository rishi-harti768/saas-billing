package org.gb.billing.service;

import org.gb.billing.dto.response.*;
import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.SubscriptionState;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics and reporting on subscriptions.
 * 
 * Provides metrics for:
 * - Active subscriptions by plan and status
 * - Churn rate calculation
 * - Subscription growth trends
 * - Revenue estimation (MRR/ARR)
 * 
 * All metrics are scoped to a specific tenant.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public AnalyticsService(SubscriptionRepository subscriptionRepository, PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    /**
     * Gets count of active subscriptions grouped by plan.
     */
    @Cacheable(value = "analytics-plans", key = "#tenantId")
    public List<SubscriptionCountByPlan> getSubscriptionCountByPlan(UUID tenantId) {
        logger.debug("Generating subscription count by plan report for tenant {}", tenantId);
        
        List<Object[]> results = subscriptionRepository.countActiveSubscriptionsByPlan(tenantId);
        
        return results.stream()
                .map(row -> new SubscriptionCountByPlan(
                        (UUID) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Gets count of subscriptions grouped by status.
     */
    @Cacheable(value = "analytics-status", key = "#tenantId")
    public List<SubscriptionCountByStatus> getSubscriptionCountByStatus(UUID tenantId) {
        logger.debug("Generating subscription count by status report for tenant {}", tenantId);
        
        List<Object[]> results = subscriptionRepository.countSubscriptionsByStatus(tenantId);
        
        return results.stream()
                .map(row -> new SubscriptionCountByStatus(
                        (SubscriptionState) row[0],
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Calculates churn rate for the last 30 days.
     * Formula: (Churned Subscriptions / Subscriptions at Start of Period) * 100
     */
    @Cacheable(value = "analytics-churn", key = "#tenantId")
    public ChurnRateResponse calculateChurnRate(UUID tenantId) {
        logger.debug("Calculating churn rate for tenant {}", tenantId);
        
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        
        // 1. Subscriptions at start of period (active at 30 days ago)
        long startCount = subscriptionRepository.countActiveSubscriptionsAtDate(tenantId, thirtyDaysAgo);
        
        // 2. Subscriptions churned during period (canceled between 30 days ago and now)
        long churnedCount = subscriptionRepository.countCanceledSubscriptionsBetween(tenantId, thirtyDaysAgo, now);
        
        Double churnRate = 0.0;
        if (startCount > 0) {
            churnRate = ((double) churnedCount / startCount) * 100.0;
            // Round to 2 decimal places
            churnRate = BigDecimal.valueOf(churnRate)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        
        return new ChurnRateResponse(
                churnRate,
                churnedCount,
                startCount,
                "LAST_30_DAYS"
        );
    }

    /**
     * Gets subscription growth data (new vs canceled) for the last 30 days.
     */
    @Cacheable(value = "analytics-growth", key = "#tenantId")
    public List<SubscriptionGrowthData> getSubscriptionGrowth(UUID tenantId) {
        logger.debug("Generating subscription growth report for tenant {}", tenantId);
        
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        
        // Fetch raw data
        List<Object[]> newSubs = subscriptionRepository.countNewSubscriptionsByDate(tenantId, thirtyDaysAgo);
        List<Object[]> canceledSubs = subscriptionRepository.countCanceledSubscriptionsByDate(tenantId, thirtyDaysAgo);
        
        // Aggregate by date map
        Map<LocalDate, SubscriptionGrowthData> dailyData = new TreeMap<>();
        
        // Process new subscriptions
        for (Object[] row : newSubs) {
            LocalDate date = ((Date) row[0]).toLocalDate();
            long count = ((Number) row[1]).longValue();
            
            dailyData.computeIfAbsent(date, d -> new SubscriptionGrowthData(d, 0, 0, 0))
                    .setNewSubscriptions(count);
        }
        
        // Process canceled subscriptions
        for (Object[] row : canceledSubs) {
            LocalDate date = ((Date) row[0]).toLocalDate();
            long count = ((Number) row[1]).longValue();
            
            dailyData.computeIfAbsent(date, d -> new SubscriptionGrowthData(d, 0, 0, 0))
                    .setCanceledSubscriptions(count);
        }
        
        // Calculate net growth
        dailyData.values().forEach(data -> 
            data.setNetGrowth(data.getNewSubscriptions() - data.getCanceledSubscriptions())
        );
        
        return new ArrayList<>(dailyData.values());
    }

    /**
     * Calculates estimated Monthly Recurring Revenue (MRR) and Annual Recurring Revenue (ARR).
     */
    @Cacheable(value = "analytics-revenue", key = "#tenantId")
    public RevenueSummaryResponse getRevenueSummary(UUID tenantId) {
        logger.debug("Generating revenue summary for tenant {}", tenantId);
        
        List<Object[]> planCounts = subscriptionRepository.countActiveSubscriptionsByPlan(tenantId);
        List<BillingPlan> plans = planRepository.findAll();
        Map<UUID, BillingPlan> planMap = plans.stream()
                .collect(Collectors.toMap(BillingPlan::getId, p -> p));
        
        BigDecimal mrr = BigDecimal.ZERO;
        List<RevenueByPlan> revenueByPlanList = new ArrayList<>();
        
        for (Object[] row : planCounts) {
            UUID planId = (UUID) row[0];
            String planName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            
            BillingPlan plan = planMap.get(planId);
            if (plan != null) {
                BigDecimal planRevenue;
                if (plan.getBillingCycle() == BillingCycle.MONTHLY) {
                    planRevenue = plan.getPrice().multiply(BigDecimal.valueOf(count));
                } else {
                    planRevenue = plan.getPrice()
                            .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(count));
                }
                
                mrr = mrr.add(planRevenue);
                
                revenueByPlanList.add(new RevenueByPlan(
                        planId,
                        planName,
                        planRevenue,
                        BigDecimal.ZERO // Will calculate percentage later
                ));
            }
        }
        
        // Calculate percentages
        final BigDecimal totalMrr = mrr.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : mrr; // Avoid DBZ
        revenueByPlanList.forEach(item -> {
            item.setPercentageOfTotal(
                    item.getRevenueAmount()
                            .divide(totalMrr, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
            );
        });
        
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));
        
        return new RevenueSummaryResponse(mrr, arr, revenueByPlanList);
    }
}
