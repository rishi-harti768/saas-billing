package org.gb.billing.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment entity representing a payment transaction.
 * Stores payment details from external payment processors (Stripe/PayPal).
 */
@Entity
@Table(name = "payment",
       indexes = {
           @Index(name = "idx_payment_subscription", columnList = "subscription_id"),
           @Index(name = "idx_payment_tenant", columnList = "tenant_id"),
           @Index(name = "idx_payment_status", columnList = "status"),
           @Index(name = "idx_payment_external_id", columnList = "external_payment_id")
       })
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // CARD, STRIPE, PAYPAL

    @Column(name = "external_payment_id", length = 255)
    private String externalPaymentId; // Stripe payment intent ID

    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    private Instant lastModifiedDate;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    // Constructors
    public Payment() {}

    public Payment(UUID subscriptionId, Long tenantId, Long userId, BigDecimal amount, 
                   String currency, PaymentStatus status, String paymentMethod) {
        this.subscriptionId = subscriptionId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
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

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public void setExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
    }

    public String getInvoiceUrl() {
        return invoiceUrl;
    }

    public void setInvoiceUrl(String invoiceUrl) {
        this.invoiceUrl = invoiceUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
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
}
