package org.gb.billing.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.gb.billing.entity.*;
import org.gb.billing.exception.PaymentFailedException;
import org.gb.billing.exception.SubscriptionNotFoundException;
import org.gb.billing.repository.PaymentRepository;
import org.gb.billing.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for processing payments using Stripe API (MVP implementation).
 * For academic purposes, this uses Stripe test mode with mock data.
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceService invoiceService;
    private final EmailService emailService;

    @Value("${stripe.api.key:sk_test_mock_key}")
    private String stripeApiKey;

    @Value("${stripe.enabled:false}")
    private boolean stripeEnabled;

    public PaymentService(PaymentRepository paymentRepository,
                         SubscriptionRepository subscriptionRepository,
                         InvoiceService invoiceService,
                         EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceService = invoiceService;
        this.emailService = emailService;
    }

    /**
     * Process a payment for a subscription.
     * Creates payment record, generates invoice, and sends email notification.
     *
     * @param subscriptionId the subscription to pay for
     * @param paymentMethod the payment method (STRIPE, PAYPAL, CARD)
     * @return the created payment
     */
    public Payment processPayment(UUID subscriptionId, String paymentMethod) {
        logger.info("Processing payment for subscription {}", subscriptionId);

        // Fetch subscription
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        // Calculate amount from plan
        BigDecimal amount = subscription.getPlan().getPrice();
        String currency = "USD";

        // Create payment record
        Payment payment = new Payment(
                subscriptionId,
                subscription.getTenantId(),
                subscription.getUserId(),
                amount,
                currency,
                PaymentStatus.PENDING,
                paymentMethod
        );

        // Process with external payment gateway (mock for MVP)
        String externalPaymentId = processExternalPayment(amount, currency, paymentMethod);
        payment.setExternalPaymentId(externalPaymentId);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.now());

        // Save payment
        payment = paymentRepository.save(payment);
        logger.info("Payment created: {}", payment.getId());

        // Generate invoice PDF
        try {
            String invoiceUrl = invoiceService.generateInvoice(payment, subscription);
            payment.setInvoiceUrl(invoiceUrl);
            payment = paymentRepository.save(payment);
            logger.info("Invoice generated: {}", invoiceUrl);
        } catch (Exception e) {
            logger.error("Failed to generate invoice", e);
            // Continue even if invoice generation fails
        }

        // Send email notification asynchronously
        try {
            emailService.sendPaymentSuccessEmail(payment, subscription);
            logger.info("Payment success email sent to user {}", subscription.getUserId());
        } catch (Exception e) {
            logger.error("Failed to send payment email", e);
            // Continue even if email fails
        }

        return payment;
    }

    /**
     * Process payment with external gateway (Stripe/PayPal).
     * For MVP, this is a mock implementation.
     */
    private String processExternalPayment(BigDecimal amount, String currency, String paymentMethod) {
        if (stripeEnabled && "STRIPE".equalsIgnoreCase(paymentMethod)) {
            return processStripePayment(amount, currency);
        } else {
            // Mock payment for MVP
            return "mock_payment_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    /**
     * Process payment through Stripe API.
     * Uses Stripe test mode for academic purposes.
     */
    private String processStripePayment(BigDecimal amount, String currency) {
        try {
            Stripe.apiKey = stripeApiKey;

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(new BigDecimal(100)).longValue()) // Convert to cents
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            logger.info("Stripe payment intent created: {}", paymentIntent.getId());
            return paymentIntent.getId();

        } catch (StripeException e) {
            logger.error("Stripe payment failed", e);
            throw new PaymentFailedException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment history for a user.
     */
    @Transactional(readOnly = true)
    public List<Payment> getUserPayments(Long tenantId, Long userId) {
        return paymentRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
    }

    /**
     * Get payment history for a subscription.
     */
    @Transactional(readOnly = true)
    public List<Payment> getSubscriptionPayments(UUID subscriptionId) {
        return paymentRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
    }

    /**
     * Webhook handler for Stripe payment events.
     * Updates payment status when webhook is received from Stripe.
     */
    public void handleStripeWebhook(String paymentIntentId, String status) {
        logger.info("Processing Stripe webhook for payment intent: {}", paymentIntentId);

        paymentRepository.findByExternalPaymentId(paymentIntentId)
                .ifPresent(payment -> {
                    if ("succeeded".equalsIgnoreCase(status)) {
                        payment.setStatus(PaymentStatus.SUCCEEDED);
                        payment.setPaidAt(Instant.now());
                    } else if ("failed".equalsIgnoreCase(status)) {
                        payment.setStatus(PaymentStatus.FAILED);
                    }
                    paymentRepository.save(payment);
                    logger.info("Payment status updated: {}", payment.getId());
                });
    }
}
