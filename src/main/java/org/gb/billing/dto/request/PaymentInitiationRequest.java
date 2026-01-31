package org.gb.billing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO for payment initiation requests.
 * Used by POST /api/v1/payments/initiate endpoint.
 */
public class PaymentInitiationRequest {

    @NotNull(message = "Subscription ID is required")
    private UUID subscriptionId;

    private String returnUrl;
    private String cancelUrl;

    // Constructors
    public PaymentInitiationRequest() {
    }

    public PaymentInitiationRequest(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    // Getters and Setters
    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}
