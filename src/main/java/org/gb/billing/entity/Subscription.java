package org.gb.billing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Subscription entity (aggregate root) representing a customer's subscription to a billing plan.
 * 
 * Lifecycle States:
 * - ACTIVE: Subscription is active with successful payments
 * - PAST_DUE: Payment failed, in grace period
 * - CANCELED: Subscription canceled (terminal state)
 * 
 * Domain Invariants:
 * - User can have only one active/past_due subscription at a time (enforced by DB constraint)
 * - State transitions must follow valid paths (enforced by SubscriptionStateMachine)
 * - Next billing date is null for canceled subscriptions
 * - Optimistic locking prevents concurrent state modifications
 * 
 * @see BillingPlan for subscription plan details
 * @see SubscriptionTransitionLog for state transition audit trail
 * @see org.gb.billing.service.SubscriptionStateMachine for state transition rules
 */
@Entity
@Table(name = "subscription",
       indexes = {
           @Index(name = "idx_subscription_user_id", columnList = "user_id"),
           @Index(name = "idx_subscription_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_subscription_status", columnList = "status"),
           @Index(name = "idx_subscription_tenant_status", columnList = "tenant_id,status"),
           @Index(name = "idx_subscription_plan_id", columnList = "plan_id"),
           @Index(name = "idx_subscription_next_billing", columnList = "next_billing_date")
       })
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_subscription_plan"))
    private BillingPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionState status;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "next_billing_date")
    private Instant nextBillingDate;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public Subscription() {
    }

    public Subscription(UUID userId, UUID tenantId, BillingPlan plan) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.plan = plan;
        this.status = SubscriptionState.ACTIVE;
        this.startDate = Instant.now();
        this.nextBillingDate = calculateNextBillingDate();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public BillingPlan getPlan() {
        return plan;
    }

    public void setPlan(BillingPlan plan) {
        this.plan = plan;
    }

    public SubscriptionState getStatus() {
        return status;
    }

    public void setStatus(SubscriptionState status) {
        this.status = status;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(Instant nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Business Methods

    /**
     * Calculates the next billing date based on the plan's billing cycle.
     * 
     * @return next billing date instant
     */
    public Instant calculateNextBillingDate() {
        if (plan == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate nextBilling;

        if (plan.getBillingCycle() == BillingCycle.MONTHLY) {
            nextBilling = today.plusMonths(1);
        } else { // YEARLY
            nextBilling = today.plusYears(1);
        }

        return nextBilling.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Upgrades this subscription to a new plan.
     * Recalculates next billing date based on new plan's cycle.
     * 
     * @param newPlan the plan to upgrade to
     */
    public void upgradeToPlan(BillingPlan newPlan) {
        this.plan = newPlan;
        this.nextBillingDate = calculateNextBillingDate();
    }

    /**
     * Cancels this subscription.
     * Sets status to CANCELED, clears next billing date, records cancellation time.
     * Customer retains access until current billing period ends.
     */
    public void cancel() {
        this.status = SubscriptionState.CANCELED;
        this.canceledAt = Instant.now();
        this.nextBillingDate = null;
    }

    /**
     * Transitions subscription to PAST_DUE state.
     * Called when payment fails.
     */
    public void transitionToPastDue() {
        this.status = SubscriptionState.PAST_DUE;
    }

    /**
     * Transitions subscription back to ACTIVE state.
     * Called when payment succeeds after being PAST_DUE.
     */
    public void transitionToActive() {
        this.status = SubscriptionState.ACTIVE;
        this.nextBillingDate = calculateNextBillingDate();
    }

    /**
     * Checks if subscription is currently active.
     * 
     * @return true if status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return status == SubscriptionState.ACTIVE;
    }

    /**
     * Checks if subscription is past due.
     * 
     * @return true if status is PAST_DUE, false otherwise
     */
    public boolean isPastDue() {
        return status == SubscriptionState.PAST_DUE;
    }

    /**
     * Checks if subscription is canceled.
     * 
     * @return true if status is CANCELED, false otherwise
     */
    public boolean isCanceled() {
        return status == SubscriptionState.CANCELED;
    }

    /**
     * Calculates days until next billing date.
     * 
     * @return days until next billing, or -1 if no next billing date
     */
    public long daysUntilNextBilling() {
        if (nextBillingDate == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(Instant.now(), nextBillingDate);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", userId=" + userId +
                ", tenantId=" + tenantId +
                ", status=" + status +
                ", startDate=" + startDate +
                ", nextBillingDate=" + nextBillingDate +
                '}';
    }
}
