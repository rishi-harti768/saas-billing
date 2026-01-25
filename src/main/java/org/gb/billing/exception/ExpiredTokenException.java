package org.gb.billing.exception;

/**
 * Exception thrown when a JWT token has expired.
 * Results in HTTP 401 Unauthorized response.
 */
public class ExpiredTokenException extends RuntimeException {
    
    public ExpiredTokenException(String message) {
        super(message);
    }
    
    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
