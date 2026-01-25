package org.gb.billing.repository;

import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for PlanRepository.
 * Uses @DataJpaTest for lightweight JPA testing with in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
class PlanRepositoryTest {

    @Autowired
    private PlanRepository planRepository;

    private BillingPlan freePlan;
    private BillingPlan proPlan;
    private BillingPlan enterprisePlan;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();

        // Create test plans
        freePlan = new BillingPlan("Free", "Free tier", BigDecimal.ZERO, BillingCycle.MONTHLY);
        proPlan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        enterprisePlan = new BillingPlan("Enterprise", "Enterprise tier", new BigDecimal("99.99"), BillingCycle.YEARLY);

        planRepository.saveAll(List.of(freePlan, proPlan, enterprisePlan));
    }

    @Test
    void shouldFindPlanByName() {
        // When
        Optional<BillingPlan> found = planRepository.findByName("Pro");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Pro");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    void shouldReturnEmptyWhenPlanNameNotFound() {
        // When
        Optional<BillingPlan> found = planRepository.findByName("NonExistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllNonDeletedPlans() {
        // Given - soft delete one plan
        proPlan.softDelete();
        planRepository.save(proPlan);

        // When
        List<BillingPlan> plans = planRepository.findAllNonDeleted();

        // Then
        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(BillingPlan::getName)
                .containsExactlyInAnyOrder("Free", "Enterprise");
    }

    @Test
    void shouldFindAllActivePlans() {
        // Given - deactivate one plan
        proPlan.setIsActive(false);
        planRepository.save(proPlan);

        // When
        List<BillingPlan> plans = planRepository.findAllActive();

        // Then
        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(BillingPlan::getName)
                .containsExactlyInAnyOrder("Free", "Enterprise");
    }

    @Test
    void shouldCheckIfPlanExistsByName() {
        // When
        boolean exists = planRepository.existsByNameAndNotDeleted("Pro");
        boolean notExists = planRepository.existsByNameAndNotDeleted("NonExistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldNotFindDeletedPlanByName() {
        // Given
        proPlan.softDelete();
        planRepository.save(proPlan);

        // When
        boolean exists = planRepository.existsByNameAndNotDeleted("Pro");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldFindNonDeletedPlanById() {
        // When
        Optional<BillingPlan> found = planRepository.findByIdAndNotDeleted(proPlan.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Pro");
    }

    @Test
    void shouldNotFindDeletedPlanById() {
        // Given
        proPlan.softDelete();
        planRepository.save(proPlan);

        // When
        Optional<BillingPlan> found = planRepository.findByIdAndNotDeleted(proPlan.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldEnforceUniquePlanName() {
        // Given
        BillingPlan duplicatePlan = new BillingPlan("Pro", "Duplicate", BigDecimal.TEN, BillingCycle.MONTHLY);

        // When/Then
        try {
            planRepository.saveAndFlush(duplicatePlan);
            assertThat(false).as("Should throw constraint violation").isTrue();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("unique_plan_name");
        }
    }

    @Test
    void shouldSavePlanWithFeatureLimits() {
        // Given
        BillingPlan customPlan = new BillingPlan("Custom", "Custom tier", new BigDecimal("49.99"), BillingCycle.MONTHLY);
        
        // When
        BillingPlan saved = planRepository.save(customPlan);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
