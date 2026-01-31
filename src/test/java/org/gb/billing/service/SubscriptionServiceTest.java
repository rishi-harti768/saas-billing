package org.gb.billing.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.gb.billing.entity.*;
import org.gb.billing.exception.DuplicateSubscriptionException;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.gb.billing.repository.SubscriptionTransitionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionTransitionLogRepository transitionLogRepository;

    private MeterRegistry meterRegistry;
    private SubscriptionService subscriptionService;
    private Long userId = 1L;
    private Long tenantId = 1L;
    private UUID proPlanId = UUID.randomUUID();
    private BillingPlan proPlan;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        proPlan = new BillingPlan("Pro", "Professional", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        proPlan.setId(proPlanId);
        
        // Use real state machine with mocked repository
        SubscriptionStateMachine stateMachine = new SubscriptionStateMachine(transitionLogRepository);
        subscriptionService = new SubscriptionService(subscriptionRepository, planRepository, stateMachine, meterRegistry);
    }

    @Test
    void shouldCreateSubscription() {
        // Given
        when(planRepository.findByIdAndNotDeleted(proPlanId)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Subscription result = subscriptionService.createSubscription(userId, tenantId, proPlanId);

        // Then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPlan()).isEqualTo(proPlan);
        assertThat(result.getStatus()).isEqualTo(SubscriptionState.ACTIVE);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(transitionLogRepository).save(any(SubscriptionTransitionLog.class));
    }

    @Test
    void shouldThrowExceptionWhenDuplicateSubscription() {
        // Given
        when(planRepository.findByIdAndNotDeleted(proPlanId)).thenReturn(Optional.of(proPlan));
        Subscription existing = new Subscription(userId, tenantId, proPlan);
        when(subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(eq(userId), eq(tenantId)))
                .thenReturn(Optional.of(existing));

        // When/Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, tenantId, proPlanId))
                .isInstanceOf(DuplicateSubscriptionException.class);
    }

    @Test
    void shouldGetSubscription() {
        // Given
        UUID subId = UUID.randomUUID();
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        when(subscriptionRepository.findByIdAndTenantId(subId, tenantId)).thenReturn(Optional.of(subscription));

        // When
        Subscription result = subscriptionService.getSubscriptionById(subId, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldUpgradeSubscription() {
        // Given
        UUID upgradePlanId = UUID.randomUUID();
        BillingPlan upgradePlan = new BillingPlan("Enterprise", "Enterprise Plan", new BigDecimal("99.99"), BillingCycle.MONTHLY);
        upgradePlan.setId(upgradePlanId);
        
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());
        
        when(subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId)).thenReturn(Optional.of(subscription));
        when(planRepository.findByIdAndNotDeleted(upgradePlanId)).thenReturn(Optional.of(upgradePlan));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // When
        subscriptionService.upgradeSubscription(subscription.getId(), tenantId, upgradePlanId);

        // Then
        verify(subscriptionRepository).save(subscription);
        verify(transitionLogRepository).save(any(SubscriptionTransitionLog.class));
        assertThat(subscription.getPlan()).isEqualTo(upgradePlan);
    }

    @Test
    void shouldCancelSubscription() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionState.ACTIVE);
        
        when(subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        subscriptionService.cancelSubscription(subscription.getId(), tenantId, userId);

        // Then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionState.CANCELED);
        verify(subscriptionRepository).save(subscription);
        verify(transitionLogRepository).save(any(SubscriptionTransitionLog.class));
    }

    @Test
    void shouldThrowExceptionWhenCancelingAlreadyCanceledSubscription() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionState.CANCELED);

        when(subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId)).thenReturn(Optional.of(subscription));

        // When/Then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(subscription.getId(), tenantId, userId))
                .isInstanceOf(org.gb.billing.exception.InvalidStateTransitionException.class);
    }

    @Test
    void shouldTransitionToPastDue() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionState.ACTIVE);

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        subscriptionService.transitionToPastDue(subscription.getId(), "Payment failed");

        // Then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionState.PAST_DUE);
        verify(transitionLogRepository).save(any(SubscriptionTransitionLog.class));
    }

    @Test
    void shouldTransitionToActive() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionState.PAST_DUE);

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        subscriptionService.transitionToActive(subscription.getId(), "Payment successful");

        // Then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionState.ACTIVE);
        verify(transitionLogRepository).save(any(SubscriptionTransitionLog.class));
    }
}
