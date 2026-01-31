package org.gb.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for initiating a payment.
 */
public class PaymentRequest {

    @NotNull(message = "Subscription ID is required")
    private UUID subscriptionId;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // STRIPE, PAYPAL, CARD

    // Constructors
    public PaymentRequest() {}

    public PaymentRequest(UUID subscriptionId, String paymentMethod) {
        this.subscriptionId = subscriptionId;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
