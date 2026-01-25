package org.gb.billing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Feature limit entity representing usage restrictions for a billing plan.
 * 
 * Examples:
 * - max_users: 10 (plan allows up to 10 users)
 * - max_projects: 5 (plan allows up to 5 projects)
 * - max_storage_gb: 100 (plan allows up to 100GB storage)
 * - api_calls_per_month: -1 (unlimited API calls)
 * 
 * Special Values:
 * - -1: Unlimited (no restriction)
 * - 0+: Specific limit value
 * 
 * @see BillingPlan for parent aggregate
 */
@Entity
@Table(name = "feature_limit",
       uniqueConstraints = @UniqueConstraint(
           name = "unique_plan_limit_type",
           columnNames = {"plan_id", "limit_type"}
       ))
public class FeatureLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_feature_limit_plan"))
    private BillingPlan plan;

    @Column(name = "limit_type", nullable = false, length = 50)
    private String limitType;

    @Column(name = "limit_value", nullable = false)
    private Integer limitValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public FeatureLimit() {
    }

    public FeatureLimit(String limitType, Integer limitValue) {
        this.limitType = limitType;
        this.limitValue = limitValue;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BillingPlan getPlan() {
        return plan;
    }

    public void setPlan(BillingPlan plan) {
        this.plan = plan;
    }

    public String getLimitType() {
        return limitType;
    }

    public void setLimitType(String limitType) {
        this.limitType = limitType;
    }

    public Integer getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Integer limitValue) {
        this.limitValue = limitValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Checks if this feature has unlimited access.
     * 
     * @return true if limitValue is -1 (unlimited), false otherwise
     */
    public boolean isUnlimited() {
        return limitValue == -1;
    }

    @Override
    public String toString() {
        return "FeatureLimit{" +
                "id=" + id +
                ", limitType='" + limitType + '\'' +
                ", limitValue=" + limitValue +
                '}';
    }
}
