package org.gb.billing.service;

import org.gb.billing.dto.ChurnStats;
import org.gb.billing.dto.MrrStats;
import org.gb.billing.dto.RevenueStats;
import org.gb.billing.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Long tenantId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantId = 1L;
    }

    @Test
    void getMrrStats_shouldCalculateTotalAndReturnBreakdown() {
        // Given
        RevenueStats octStats = mock(RevenueStats.class);
        when(octStats.getPeriod()).thenReturn("2023-10");
        when(octStats.getAmount()).thenReturn(new BigDecimal("100.00"));

        RevenueStats novStats = mock(RevenueStats.class);
        when(novStats.getPeriod()).thenReturn("2023-11");
        when(novStats.getAmount()).thenReturn(new BigDecimal("200.00"));

        when(subscriptionRepository.calculateMonthlyRevenue()).thenReturn(Arrays.asList(octStats, novStats));

        // When
        MrrStats result = analyticsService.getMrrStats();

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("300.00"), result.totalMrr());
        assertEquals(2, result.monthlyBreakdown().size());
        verify(subscriptionRepository, times(1)).calculateMonthlyRevenue();
    }

    @Test
    void getChurnStats_shouldCalculatePercentage() {
        // Given
        when(subscriptionRepository.countTotalSubscriptions()).thenReturn(100L);
        when(subscriptionRepository.countCancelledSubscriptions()).thenReturn(10L);

        // When
        ChurnStats result = analyticsService.getChurnStats();

        // Then
        assertNotNull(result);
        assertEquals(100L, result.totalSubscribers());
        assertEquals(10L, result.cancelledSubscribers());
        assertEquals(10.0, result.churnRatePercentage(), 0.01);
    }

    @Test
    void getChurnStats_shouldHandleDivideByZero() {
        // Given
        when(subscriptionRepository.countTotalSubscriptions()).thenReturn(0L);

        // When
        ChurnStats result = analyticsService.getChurnStats();

        // Then
        assertEquals(0.0, result.churnRatePercentage());
    }
}
