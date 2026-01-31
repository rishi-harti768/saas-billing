package org.gb.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.gb.billing.dto.request.PaymentRequest;
import org.gb.billing.dto.response.PaymentResponse;
import org.gb.billing.entity.Payment;
import org.gb.billing.security.CustomUserDetails;
import org.gb.billing.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for payment operations.
 * Handles payment processing, webhook events, and payment history.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing and invoice management")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Process a payment for a subscription.
     */
    @PostMapping
    @Operation(
            summary = "Process Payment",
            description = "Process a payment for a subscription. Generates invoice and sends email notification.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logger.info("Processing payment for subscription: {}", request.getSubscriptionId());

        Payment payment = paymentService.processPayment(
                request.getSubscriptionId(),
                request.getPaymentMethod()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toPaymentResponse(payment));
    }

    /**
     * Get payment history for the authenticated user.
     */
    @GetMapping("/history")
    @Operation(
            summary = "Get Payment History",
            description = "Retrieve payment history for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long tenantId = userDetails.getTenantId();
        Long userId = userDetails.getUserId();

        List<Payment> payments = paymentService.getUserPayments(tenantId, userId);
        List<PaymentResponse> responses = payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get payments for a specific subscription.
     */
    @GetMapping("/subscription/{subscriptionId}")
    @Operation(
            summary = "Get Subscription Payments",
            description = "Retrieve all payments for a specific subscription",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<PaymentResponse>> getSubscriptionPayments(
            @PathVariable UUID subscriptionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Payment> payments = paymentService.getSubscriptionPayments(subscriptionId);
        List<PaymentResponse> responses = payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Stripe webhook endpoint for payment events.
     * Handles payment status updates from Stripe.
     */
    @PostMapping("/webhook/stripe")
    @Operation(
            summary = "Stripe Webhook",
            description = "Webhook endpoint for Stripe payment events"
    )
    public ResponseEntity<Map<String, String>> handleStripeWebhook(
            @RequestBody Map<String, Object> payload) {

        logger.info("Received Stripe webhook");

        try {
            String eventType = (String) payload.get("type");

            if ("payment_intent.succeeded".equals(eventType)) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                String paymentIntentId = (String) object.get("id");

                paymentService.handleStripeWebhook(paymentIntentId, "succeeded");
            } else if ("payment_intent.payment_failed".equals(eventType)) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                String paymentIntentId = (String) object.get("id");

                paymentService.handleStripeWebhook(paymentIntentId, "failed");
            }

            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Convert Payment entity to PaymentResponse DTO.
     */
    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getSubscriptionId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                payment.getExternalPaymentId(),
                payment.getInvoiceUrl(),
                payment.getCreatedAt(),
                payment.getPaidAt()
        );
    }
}
