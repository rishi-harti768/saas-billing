package org.gb.billing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription transition log entity for audit trail of subscription state changes.
 * 
 * Captures all state transitions for compliance, debugging, and analytics:
 * - Initial subscription creation (fromStatus = null, toStatus = ACTIVE)
 * - Payment failures (ACTIVE → PAST_DUE)
 * - Payment recoveries (PAST_DUE → ACTIVE)
 * - Cancellations (ACTIVE/PAST_DUE → CANCELED)
 * - Upgrades (state remains ACTIVE, but plan changes)
 * 
 * @see Subscription for subscription lifecycle management
 * @see SubscriptionState for valid state values
 */
@Entity
@Table(name = "subscription_transition_log",
       indexes = {
           @Index(name = "idx_transition_log_subscription_id", columnList = "subscription_id"),
           @Index(name = "idx_transition_log_transitioned_at", columnList = "transitioned_at"),
           @Index(name = "idx_transition_log_to_status", columnList = "to_status")
       })
@EntityListeners(AuditingEntityListener.class)
public class SubscriptionTransitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "transitioned_by")
    private Long transitionedBy;

    @CreationTimestamp
    @Column(name = "transitioned_at", nullable = false, updatable = false)
    private Instant transitionedAt;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    private Instant lastModifiedDate;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    // Constructors
    public SubscriptionTransitionLog() {
    }

    public SubscriptionTransitionLog(UUID subscriptionId, String fromStatus, String toStatus, String reason, Long transitionedBy) {
        this.subscriptionId = subscriptionId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.transitionedBy = transitionedBy;
    }

    public SubscriptionTransitionLog(UUID subscriptionId, SubscriptionState fromState, SubscriptionState toState, String reason, Long transitionedBy) {
        this.subscriptionId = subscriptionId;
        this.fromStatus = fromState != null ? fromState.name() : null;
        this.toStatus = toState.name();
        this.reason = reason;
        this.transitionedBy = transitionedBy;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getTransitionedBy() {
        return transitionedBy;
    }

    public void setTransitionedBy(Long transitionedBy) {
        this.transitionedBy = transitionedBy;
    }

    public Instant getTransitionedAt() {
        return transitionedAt;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Checks if this log entry represents the initial subscription creation.
     * 
     * @return true if fromStatus is null (initial creation), false otherwise
     */
    public boolean isInitialCreation() {
        return fromStatus == null;
    }

    /**
     * Checks if this transition was triggered by the system (not a user).
     * 
     * @return true if transitionedBy is null (system transition), false otherwise
     */
    public boolean isSystemTransition() {
        return transitionedBy == null;
    }

    @Override
    public String toString() {
        return "SubscriptionTransitionLog{" +
                "id=" + id +
                ", subscriptionId=" + subscriptionId +
                ", fromStatus='" + fromStatus + '\'' +
                ", toStatus='" + toStatus + '\'' +
                ", reason='" + reason + '\'' +
                ", transitionedAt=" + transitionedAt +
                '}';
    }
}
