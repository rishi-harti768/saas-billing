package org.gb.billing.service;

import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.FeatureLimit;
import org.gb.billing.exception.PlanHasActiveSubscriptionsException;
import org.gb.billing.exception.PlanNotFoundException;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlanService.
 * Uses Mockito to mock repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private PlanService planService;

    private BillingPlan proPlan;
    private UUID planId;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        proPlan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        proPlan.setId(planId);
    }

    @Test
    void shouldCreatePlan() {
        // Given
        when(planRepository.existsByNameAndNotDeleted("Pro")).thenReturn(false);
        when(planRepository.save(any(BillingPlan.class))).thenReturn(proPlan);

        // When
        BillingPlan created = planService.createPlan(proPlan);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Pro");
        verify(planRepository).save(any(BillingPlan.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicatePlan() {
        // Given
        when(planRepository.existsByNameAndNotDeleted("Pro")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> planService.createPlan(proPlan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldGetAllPlans() {
        // Given
        List<BillingPlan> plans = List.of(proPlan);
        when(planRepository.findAllNonDeleted()).thenReturn(plans);

        // When
        List<BillingPlan> result = planService.getAllPlans();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Pro");
        verify(planRepository).findAllNonDeleted();
    }

    @Test
    void shouldGetPlanById() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));

        // When
        BillingPlan found = planService.getPlanById(planId);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Pro");
    }

    @Test
    void shouldThrowExceptionWhenPlanNotFound() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> planService.getPlanById(planId))
                .isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void shouldUpdatePlan() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));
        when(planRepository.save(any(BillingPlan.class))).thenReturn(proPlan);

        BillingPlan updates = new BillingPlan("Pro", "Updated description", new BigDecimal("39.99"), BillingCycle.MONTHLY);

        // When
        BillingPlan updated = planService.updatePlan(planId, updates);

        // Then
        assertThat(updated).isNotNull();
        verify(planRepository).save(any(BillingPlan.class));
    }

    @Test
    void shouldDeletePlanWhenNoActiveSubscriptions() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.countActiveSubscriptionsByPlanId(planId)).thenReturn(0L);
        when(planRepository.save(any(BillingPlan.class))).thenReturn(proPlan);

        // When
        planService.deletePlan(planId);

        // Then
        verify(planRepository).save(argThat(plan -> plan.getDeleted()));
    }

    @Test
    void shouldThrowExceptionWhenDeletingPlanWithActiveSubscriptions() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.countActiveSubscriptionsByPlanId(planId)).thenReturn(5L);

        // When/Then
        assertThatThrownBy(() -> planService.deletePlan(planId))
                .isInstanceOf(PlanHasActiveSubscriptionsException.class)
                .hasMessageContaining("5 active subscriptions");
    }

    @Test
    void shouldAddFeatureLimitToPlan() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));
        when(planRepository.save(any(BillingPlan.class))).thenReturn(proPlan);

        FeatureLimit limit = new FeatureLimit("max_users", 10);

        // When
        planService.addFeatureLimit(planId, limit);

        // Then
        verify(planRepository).save(argThat(plan -> plan.getFeatureLimits().size() > 0));
    }
}
