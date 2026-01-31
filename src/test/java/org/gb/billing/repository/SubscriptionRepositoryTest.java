package org.gb.billing.repository;

import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.SubscriptionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for SubscriptionRepository.
 * Tests tenant isolation, state filtering, and custom queries.
 */
@DataJpaTest
@ActiveProfiles("test")
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    private BillingPlan proPlan;
    private Long tenantId1;
    private Long tenantId2;
    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        planRepository.deleteAll();

        // Create test plan
        proPlan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        proPlan = planRepository.save(proPlan);

        // Test data
        tenantId1 = 1L;
        tenantId2 = 2L;
        userId1 = 101L;
        userId2 = 102L;
    }

    @Test
    void shouldFindSubscriptionByIdAndTenantId() {
        // Given
        Subscription subscription = new Subscription(userId1, tenantId1, proPlan);
        subscription = subscriptionRepository.save(subscription);

        // When
        Optional<Subscription> found = subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId1);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId1);
    }

    @Test
    void shouldNotFindSubscriptionFromDifferentTenant() {
        // Given
        Subscription subscription = new Subscription(userId1, tenantId1, proPlan);
        subscription = subscriptionRepository.save(subscription);

        // When - try to access with different tenant ID
        Optional<Subscription> found = subscriptionRepository.findByIdAndTenantId(subscription.getId(), tenantId2);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindActiveSubscriptionForUser() {
        // Given
        Subscription subscription = new Subscription(userId1, tenantId1, proPlan);
        subscription = subscriptionRepository.save(subscription);

        // When
        Optional<Subscription> found = subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId1, tenantId1);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(SubscriptionState.ACTIVE);
    }

    @Test
    void shouldFindPastDueSubscriptionForUser() {
        // Given
        Subscription subscription = new Subscription(userId1, tenantId1, proPlan);
        subscription.transitionToPastDue();
        subscription = subscriptionRepository.save(subscription);

        // When
        Optional<Subscription> found = subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId1, tenantId1);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(SubscriptionState.PAST_DUE);
    }

    @Test
    void shouldNotFindCanceledSubscriptionForUser() {
        // Given
        Subscription subscription = new Subscription(userId1, tenantId1, proPlan);
        subscription.cancel();
        subscription = subscriptionRepository.save(subscription);

        // When
        Optional<Subscription> found = subscriptionRepository.findActiveOrPastDueByUserIdAndTenantId(userId1, tenantId1);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllSubscriptionsForUser() {
        // Given
        Subscription active = new Subscription(userId1, tenantId1, proPlan);
        Subscription canceled = new Subscription(userId1, tenantId1, proPlan);
        canceled.cancel();
        
        subscriptionRepository.saveAll(List.of(active, canceled));

        // When
        List<Subscription> subscriptions = subscriptionRepository.findByUserIdAndTenantId(userId1, tenantId1);

        // Then
        assertThat(subscriptions).hasSize(2);
    }

    @Test
    void shouldCountActiveSubscriptionsByPlan() {
        // Given
        Subscription active1 = new Subscription(userId1, tenantId1, proPlan);
        Subscription active2 = new Subscription(userId2, tenantId1, proPlan);
        Subscription canceled = new Subscription(103L, tenantId1, proPlan);
        canceled.cancel();

        subscriptionRepository.saveAll(List.of(active1, active2, canceled));

        // When
        long count = subscriptionRepository.countActiveSubscriptionsByPlanId(proPlan.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindSubscriptionsByStatus() {
        // Given
        Subscription active = new Subscription(userId1, tenantId1, proPlan);
        Subscription pastDue = new Subscription(userId2, tenantId1, proPlan);
        pastDue.transitionToPastDue();

        subscriptionRepository.saveAll(List.of(active, pastDue));

        // When
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatusAndTenantId(SubscriptionState.ACTIVE, tenantId1);
        List<Subscription> pastDueSubscriptions = subscriptionRepository.findByStatusAndTenantId(SubscriptionState.PAST_DUE, tenantId1);

        // Then
        assertThat(activeSubscriptions).hasSize(1);
        assertThat(pastDueSubscriptions).hasSize(1);
    }

    @Test
    void shouldEnforceTenantIsolation() {
        // Given
        Subscription tenant1Sub = new Subscription(userId1, tenantId1, proPlan);
        Subscription tenant2Sub = new Subscription(userId2, tenantId2, proPlan);

        subscriptionRepository.saveAll(List.of(tenant1Sub, tenant2Sub));

        // When
        List<Subscription> tenant1Subscriptions = subscriptionRepository.findByStatusAndTenantId(SubscriptionState.ACTIVE, tenantId1);
        List<Subscription> tenant2Subscriptions = subscriptionRepository.findByStatusAndTenantId(SubscriptionState.ACTIVE, tenantId2);

        // Then
        assertThat(tenant1Subscriptions).hasSize(1);
        assertThat(tenant2Subscriptions).hasSize(1);
        assertThat(tenant1Subscriptions.get(0).getTenantId()).isEqualTo(tenantId1);
        assertThat(tenant2Subscriptions.get(0).getTenantId()).isEqualTo(tenantId2);
    }

    @Test
    void shouldCountSubscriptionsByStatus() {
        // Given
        Subscription active = new Subscription(userId1, tenantId1, proPlan);
        Subscription pastDue = new Subscription(userId2, tenantId1, proPlan);
        pastDue.transitionToPastDue();

        subscriptionRepository.saveAll(List.of(active, pastDue));

        // When
        List<Object[]> counts = subscriptionRepository.countSubscriptionsByStatus(tenantId1);

        // Then
        assertThat(counts).hasSize(2);
    }

    @Test
    void shouldCountActiveSubscriptionsGroupedByPlan() {
        // Given
        Subscription sub1 = new Subscription(userId1, tenantId1, proPlan);
        Subscription sub2 = new Subscription(userId2, tenantId1, proPlan);

        subscriptionRepository.saveAll(List.of(sub1, sub2));

        // When
        List<Object[]> counts = subscriptionRepository.countActiveSubscriptionsByPlan(tenantId1);

        // Then
        assertThat(counts).hasSize(1);
        assertThat(counts.get(0)[2]).isEqualTo(2L); // Count should be 2
    }
}
