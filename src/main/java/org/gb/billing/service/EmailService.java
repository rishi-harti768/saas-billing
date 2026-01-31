package org.gb.billing.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.gb.billing.entity.Payment;
import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.User;
import org.gb.billing.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Service for sending email notifications.
 * Uses JavaMailSender for SMTP email delivery.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;

    @Value("${spring.mail.from:noreply@billing.com}")
    private String fromEmail;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender,
                       UserRepository userRepository,
                       InvoiceService invoiceService) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.invoiceService = invoiceService;
    }

    /**
     * Send payment success email with invoice attachment.
     * Executes asynchronously to avoid blocking payment processing.
     *
     * @param payment the successful payment
     * @param subscription the subscription details
     */
    @Async
    public void sendPaymentSuccessEmail(Payment payment, Subscription subscription) {
        if (!emailEnabled) {
            logger.info("Email is disabled. Skipping payment success email for payment {}", payment.getId());
            return;
        }

        try {
            // Fetch user details
            User user = userRepository.findById(payment.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Payment Successful - Invoice Attached");

            // Build email body
            String emailBody = buildPaymentSuccessEmailBody(payment, subscription, user);
            helper.setText(emailBody, true); // true = HTML content

            // Attach invoice PDF if available
            if (payment.getInvoiceUrl() != null) {
                File invoiceFile = invoiceService.getInvoiceFile(payment.getInvoiceUrl());
                if (invoiceFile.exists()) {
                    FileSystemResource file = new FileSystemResource(invoiceFile);
                    helper.addAttachment("invoice_" + payment.getId() + ".pdf", file);
                }
            }

            // Send email
            mailSender.send(message);
            logger.info("Payment success email sent to {} for payment {}", user.getEmail(), payment.getId());

        } catch (MessagingException e) {
            logger.error("Failed to send payment success email for payment {}", payment.getId(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    /**
     * Build HTML email body for payment success notification.
     */
    private String buildPaymentSuccessEmailBody(Payment payment, Subscription subscription, User user) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f9f9f9; }
                        .details { background: white; padding: 15px; margin: 10px 0; border-left: 4px solid #4CAF50; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Successful!</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>Your payment has been processed successfully. Thank you for your business!</p>
                            
                            <div class="details">
                                <h3>Payment Details</h3>
                                <p><strong>Amount:</strong> %s %s</p>
                                <p><strong>Payment ID:</strong> %s</p>
                                <p><strong>Payment Method:</strong> %s</p>
                                <p><strong>Status:</strong> %s</p>
                            </div>
                            
                            <div class="details">
                                <h3>Subscription Details</h3>
                                <p><strong>Plan:</strong> %s</p>
                                <p><strong>Billing Cycle:</strong> %s</p>
                                <p><strong>Subscription ID:</strong> %s</p>
                            </div>
                            
                            <p>Your invoice is attached to this email. You can also download it from your account dashboard.</p>
                            
                            <p>If you have any questions, please don't hesitate to contact our support team.</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply to this email.</p>
                            <p>&copy; 2026 Billing Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                user.getEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getId(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                subscription.getPlan().getName(),
                subscription.getPlan().getBillingCycle(),
                subscription.getId()
        );
    }

    /**
     * Send subscription cancellation email.
     */
    @Async
    public void sendSubscriptionCancelledEmail(User user, Subscription subscription) {
        if (!emailEnabled) {
            logger.info("Email is disabled. Skipping cancellation email");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Cancelled");

            String emailBody = String.format("""
                    <html>
                    <body>
                        <h2>Subscription Cancelled</h2>
                        <p>Dear %s,</p>
                        <p>Your subscription to %s has been cancelled.</p>
                        <p>We're sorry to see you go. If you change your mind, you can resubscribe anytime.</p>
                        <p>Best regards,<br>Billing Team</p>
                    </body>
                    </html>
                    """, user.getEmail(), subscription.getPlan().getName());

            helper.setText(emailBody, true);
            mailSender.send(message);
            logger.info("Cancellation email sent to {}", user.getEmail());

        } catch (MessagingException e) {
            logger.error("Failed to send cancellation email", e);
        }
    }
}
