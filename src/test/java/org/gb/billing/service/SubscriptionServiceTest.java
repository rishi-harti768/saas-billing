package org.gb.billing.service;

import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.SubscriptionState;
import org.gb.billing.exception.DuplicateSubscriptionException;
import org.gb.billing.exception.InvalidUpgradeException;
import org.gb.billing.exception.PlanNotFoundException;
import org.gb.billing.exception.SubscriptionNotFoundException;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionService.
 * Tests subscription lifecycle operations with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionStateMachine stateMachine;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private BillingPlan proPlan;
    private UUID planId;
    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        proPlan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        proPlan.setId(planId);
    }

    @Test
    void shouldCreateSubscription() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Subscription subscription = subscriptionService.createSubscription(userId, tenantId, planId);

        // Then
        assertThat(subscription).isNotNull();
        assertThat(subscription.getUserId()).isEqualTo(userId);
        assertThat(subscription.getTenantId()).isEqualTo(tenantId);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionState.ACTIVE);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(stateMachine).logInitialCreation(any(Subscription.class), eq(userId));
    }

    @Test
    void shouldThrowExceptionWhenPlanNotFound() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, tenantId, planId))
                .isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyHasActiveSubscription() {
        // Given
        Subscription existingSubscription = new Subscription(userId, tenantId, proPlan);
        existingSubscription.setId(UUID.randomUUID());

        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(existingSubscription));

        // When/Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, tenantId, planId))
                .isInstanceOf(DuplicateSubscriptionException.class)
                .hasMessageContaining("already has an active subscription");
    }

    @Test
    void shouldGetSubscriptionById() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        UUID subscriptionId = UUID.randomUUID();
        subscription.setId(subscriptionId);

        when(subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId))
                .thenReturn(Optional.of(subscription));

        // When
        Subscription found = subscriptionService.getSubscriptionById(subscriptionId, tenantId);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(subscriptionId);
    }

    @Test
    void shouldThrowExceptionWhenSubscriptionNotFound() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        when(subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(subscriptionId, tenantId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void shouldGetMySubscription() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        when(subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(subscription));

        // When
        Optional<Subscription> found = subscriptionService.getMySubscription(userId, tenantId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoActiveSubscription() {
        // Given
        when(subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.empty());

        // When
        Optional<Subscription> found = subscriptionService.getMySubscription(userId, tenantId);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCalculateNextBillingDateForMonthlyPlan() {
        // Given
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Subscription subscription = subscriptionService.createSubscription(userId, tenantId, planId);

        // Then
        assertThat(subscription.getNextBillingDate()).isNotNull();
    }

    @Test
    void shouldEnforceTenantIsolation() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        UUID differentTenantId = UUID.randomUUID();

        when(subscriptionRepository.findByIdAndTenantId(subscriptionId, differentTenantId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> subscriptionService.getSubscriptionById(subscriptionId, differentTenantId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void shouldUpgradeSubscription() {
        // Given
        BillingPlan upgradePlan = new BillingPlan("Enterprise", "Enterprise tier", new BigDecimal("299.99"), BillingCycle.YEARLY);
        UUID upgradePlanId = UUID.randomUUID();
        upgradePlan.setId(upgradePlanId);
        
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());
        
        when(subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId)).thenReturn(Optional.of(subscription));
        when(planRepository.findByIdAndNotDeleted(upgradePlanId)).thenReturn(Optional.of(upgradePlan));
        
        // When
        subscriptionService.upgradeSubscription(subscription.getId(), tenantId, upgradePlanId);

        // Then
        verify(subscriptionRepository).save(subscription);
        verify(stateMachine).logUpgrade(eq(subscription), eq(proPlan), eq(upgradePlan), eq(userId));
        assertThat(subscription.getPlan()).isEqualTo(upgradePlan);
    }

    @Test
    void shouldThrowExceptionWhenUpgradingToSamePlan() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());

        when(subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId)).thenReturn(Optional.of(subscription));
        when(planRepository.findByIdAndNotDeleted(planId)).thenReturn(Optional.of(proPlan));

        // When/Then
        assertThatThrownBy(() -> subscriptionService.upgradeSubscription(subscription.getId(), tenantId, planId))
                .isInstanceOf(InvalidUpgradeException.class)
                .hasMessageContaining("Cannot upgrade to the same plan");
    }

    @Test
    void shouldCancelSubscription() {
        // Given
        Subscription subscription = new Subscription(userId, tenantId, proPlan);
        subscription.setId(UUID.randomUUID());
        
        when(subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        subscriptionService.cancelSubscription(subscription.getId(), tenantId, userId);

        // Then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionState.CANCELED);
        verify(subscriptionRepository).save(subscription);
        verify(stateMachine).transitionTo(eq(subscription), eq(SubscriptionState.CANCELED), anyString(), eq(userId));
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
        verify(stateMachine).transitionTo(eq(subscription), eq(SubscriptionState.PAST_DUE), anyString(), isNull());
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
        verify(stateMachine).transitionTo(eq(subscription), eq(SubscriptionState.ACTIVE), anyString(), isNull());
    }
}
