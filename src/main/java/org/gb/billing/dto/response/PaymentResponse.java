package org.gb.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for payment information.
 */
public class PaymentResponse {

    private UUID id;
    private UUID subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String externalPaymentId;
    private String invoiceUrl;
    private Instant createdAt;
    private Instant paidAt;

    // Constructors
    public PaymentResponse() {}

    public PaymentResponse(UUID id, UUID subscriptionId, BigDecimal amount, String currency,
                          String status, String paymentMethod, String externalPaymentId,
                          String invoiceUrl, Instant createdAt, Instant paidAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.externalPaymentId = externalPaymentId;
        this.invoiceUrl = invoiceUrl;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}
