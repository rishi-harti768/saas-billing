package org.gb.billing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Billing plan entity (aggregate root) representing subscription tiers.
 * 
 * Examples:
 * - Free Plan: $0/month, limited features
 * - Pro Plan: $29.99/month, enhanced features
 * - Enterprise Plan: $99.99/month, unlimited features
 * 
 * Domain Invariants:
 * - Plan name must be unique
 * - Price must be >= 0 (free plans allowed)
 * - Deleted plans cannot be restored (soft delete)
 * - Plans with active subscriptions cannot be hard deleted
 * 
 * @see FeatureLimit for plan feature restrictions
 * @see Subscription for customer subscriptions to plans
 */
@Entity
@Table(name = "billing_plan",
       uniqueConstraints = @UniqueConstraint(
           name = "unique_plan_name",
           columnNames = "name"
       ),
       indexes = {
           @Index(name = "idx_billing_plan_deleted", columnList = "deleted"),
           @Index(name = "idx_billing_plan_active", columnList = "is_active"),
           @Index(name = "idx_billing_plan_name", columnList = "name")
       })
public class BillingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FeatureLimit> featureLimits = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public BillingPlan() {
    }

    public BillingPlan(String name, String description, BigDecimal price, BillingCycle billingCycle) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.billingCycle = billingCycle;
        this.isActive = true;
        this.deleted = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public List<FeatureLimit> getFeatureLimits() {
        return featureLimits;
    }

    public void setFeatureLimits(List<FeatureLimit> featureLimits) {
        this.featureLimits = featureLimits;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Business Methods

    /**
     * Adds a feature limit to this plan.
     * Maintains bidirectional relationship.
     * 
     * @param featureLimit the feature limit to add
     */
    public void addFeatureLimit(FeatureLimit featureLimit) {
        featureLimits.add(featureLimit);
        featureLimit.setPlan(this);
    }

    /**
     * Removes a feature limit from this plan.
     * Maintains bidirectional relationship.
     * 
     * @param featureLimit the feature limit to remove
     */
    public void removeFeatureLimit(FeatureLimit featureLimit) {
        featureLimits.remove(featureLimit);
        featureLimit.setPlan(null);
    }

    /**
     * Soft deletes this plan.
     * Sets deleted flag to true without removing from database.
     */
    public void softDelete() {
        this.deleted = true;
        this.isActive = false;
    }

    /**
     * Checks if this plan is free (price = 0).
     * 
     * @return true if price is zero, false otherwise
     */
    public boolean isFree() {
        return price.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public String toString() {
        return "BillingPlan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", billingCycle=" + billingCycle +
                ", isActive=" + isActive +
                ", deleted=" + deleted +
                '}';
    }
}
