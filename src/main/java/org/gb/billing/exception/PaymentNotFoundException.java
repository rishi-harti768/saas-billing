package org.gb.billing.exception;

/**
 * Exception thrown when payment is not found.
 */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
