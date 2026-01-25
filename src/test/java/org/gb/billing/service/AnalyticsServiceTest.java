package org.gb.billing.service;

import org.gb.billing.dto.response.*;
import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.SubscriptionState;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    void shouldGetSubscriptionCountByPlan() {
        // Given
        UUID planId = UUID.randomUUID();
        String planName = "Pro";
        long count = 10L;
        List<Object[]> queryResult = Collections.singletonList(new Object[]{planId, planName, count});

        when(subscriptionRepository.countActiveSubscriptionsByPlan(tenantId)).thenReturn(queryResult);

        // When
        List<SubscriptionCountByPlan> result = analyticsService.getSubscriptionCountByPlan(tenantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlanId()).isEqualTo(planId);
        assertThat(result.get(0).getPlanName()).isEqualTo(planName);
        assertThat(result.get(0).getActiveCount()).isEqualTo(count);
    }

    @Test
    void shouldGetSubscriptionCountByStatus() {
        // Given
        SubscriptionState status = SubscriptionState.ACTIVE;
        long count = 25L;
        List<Object[]> queryResult = Collections.singletonList(new Object[]{status, count});

        when(subscriptionRepository.countSubscriptionsByStatus(tenantId)).thenReturn(queryResult);

        // When
        List<SubscriptionCountByStatus> result = analyticsService.getSubscriptionCountByStatus(tenantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(status);
        assertThat(result.get(0).getCount()).isEqualTo(count);
    }

    @Test
    void shouldCalculateChurnRate() {
        // Given
        long startCount = 100L;
        long churnedCount = 5L;

        when(subscriptionRepository.countActiveSubscriptionsAtDate(eq(tenantId), any(Instant.class)))
                .thenReturn(startCount);
        when(subscriptionRepository.countCanceledSubscriptionsBetween(eq(tenantId), any(Instant.class), any(Instant.class)))
                .thenReturn(churnedCount);

        // When
        ChurnRateResponse result = analyticsService.calculateChurnRate(tenantId);

        // Then
        assertThat(result.getChurnRatePercentage()).isEqualTo(5.0);
        assertThat(result.getChurnedSubscriptions()).isEqualTo(churnedCount);
        assertThat(result.getTotalSubscriptionsStartOfPeriod()).isEqualTo(startCount);
    }

    @Test
    void shouldCalculateRevenueSummary() {
        // Given
        UUID planId1 = UUID.randomUUID();
        UUID planId2 = UUID.randomUUID();

        BillingPlan plan1 = new BillingPlan("Pro", "Pro Plan", new BigDecimal("100.00"), BillingCycle.MONTHLY);
        plan1.setId(planId1);
        
        BillingPlan plan2 = new BillingPlan("Enterprise", "Ent Plan", new BigDecimal("1200.00"), BillingCycle.YEARLY);
        plan2.setId(planId2);

        List<BillingPlan> allPlans = Arrays.asList(plan1, plan2);
        
        // 5 Pro Plans ($500 MRR)
        // 2 Enterprise Plans ($200 MRR)
        List<Object[]> planCounts = Arrays.asList(
            new Object[]{planId1, "Pro", 5L},
            new Object[]{planId2, "Enterprise", 2L}
        );

        when(planRepository.findAll()).thenReturn(allPlans);
        when(subscriptionRepository.countActiveSubscriptionsByPlan(tenantId)).thenReturn(planCounts);

        // When
        RevenueSummaryResponse result = analyticsService.getRevenueSummary(tenantId);

        // Then
        // Total MRR = (100 * 5) + (1200/12 * 2) = 500 + 200 = 700
        assertThat(result.getMonthlyRecurringRevenue()).isEqualByComparingTo(new BigDecimal("700.00"));
        // Total ARR = 700 * 12 = 8400
        assertThat(result.getAnnualRecurringRevenue()).isEqualByComparingTo(new BigDecimal("8400.00"));
        
        assertThat(result.getRevenueByPlan()).hasSize(2);
    }
}
