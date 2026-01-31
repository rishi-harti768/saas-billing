package org.gb.billing.exception;

/**
 * Exception thrown when a user exceeds their API rate limit.
 * 
 * This exception is caught by GlobalExceptionHandler and converted to
 * an HTTP 429 (Too Many Requests) response with a Retry-After header.
 * 
 * @see org.gb.billing.exception.GlobalExceptionHandler#handleRateLimitExceeded
 * @see org.gb.billing.security.RateLimitingFilter
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfterSeconds;
    
    /**
     * Creates a new RateLimitExceededException.
     * 
     * @param retryAfterSeconds number of seconds until the rate limit resets
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        super("API rate limit exceeded. Please try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    /**
     * Gets the number of seconds until the rate limit resets.
     * 
     * @return seconds until retry is allowed
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
