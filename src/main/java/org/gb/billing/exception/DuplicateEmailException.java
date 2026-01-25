package org.gb.billing.exception;

/**
 * Exception thrown when attempting to register a user with an email address that already exists.
 * Results in HTTP 409 Conflict response.
 */
public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String message) {
        super(message);
    }
    
    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
