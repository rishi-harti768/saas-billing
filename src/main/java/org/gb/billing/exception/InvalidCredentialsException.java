package org.gb.billing.exception;

/**
 * Exception thrown when login fails due to invalid email or password.
 * Results in HTTP 401 Unauthorized response.
 * 
 * Note: Error message should be generic to avoid revealing whether email or password was incorrect.
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
