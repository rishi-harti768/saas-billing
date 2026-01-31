package org.gb.billing.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.gb.billing.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across the API.
 * Handles all exceptions and converts them to standardized ErrorResponse DTOs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /**
     * Helper method to create ErrorResponse with all required fields.
     */
    private ErrorResponse createErrorResponse(HttpStatus status, String error, String errorCode, String message, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(status.value());
        response.setError(error);
        response.setErrorCode(errorCode);
        response.setMessage(message);
        response.setPath(request.getRequestURI());
        response.setTimestamp(java.time.Instant.now().toString());
        return response;
    }


    /**
     * Handle duplicate email registration attempts.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        logger.warn("Duplicate email registration attempt: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.CONFLICT, "Conflict", "DUPLICATE_EMAIL", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle invalid login credentials.
     * Returns HTTP 401 Unauthorized.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        logger.warn("Invalid credentials attempt for path: {}", request.getRequestURI());
        
        ErrorResponse error = createErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "INVALID_CREDENTIALS", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle expired JWT tokens.
     * Returns HTTP 401 Unauthorized.
     */
    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredToken(
            ExpiredTokenException ex, HttpServletRequest request) {
        logger.warn("Expired token for path: {}", request.getRequestURI());
        
        ErrorResponse error = createErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", "TOKEN_EXPIRED", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle validation errors from @Valid annotation.
     * Returns HTTP 400 Bad Request with field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue(),
                        getExampleForField(error.getField()) // Added example field
                ))
                .collect(Collectors.toList());
        
        logger.warn("Validation errors for path {}: {} field(s) failed validation", 
                request.getRequestURI(), fieldErrors.size());
        
        ErrorResponse error = createErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "VALIDATION_FAILED", "Validation failed for one or more fields", request);
        error.setErrors(fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // New helper method for validation examples
    private String getExampleForField(String field) {
        return switch (field) {
            case "email" -> "user@example.com";
            case "password" -> "StrongP@ssw0rd123";
            case "role" -> "ROLE_USER, ROLE_ADMIN";
            case "name" -> "Enterprise Plan";
            case "price" -> "99.99";
            default -> null;
        };
    }

    /**
     * Handle plan not found errors.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(PlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlanNotFound(
            PlanNotFoundException ex, HttpServletRequest request) {
        logger.warn("Plan not found: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.NOT_FOUND, "Not Found", "PLAN_NOT_FOUND", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle subscription not found errors.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFound(
            SubscriptionNotFoundException ex, HttpServletRequest request) {
        logger.warn("Subscription not found: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.NOT_FOUND, "Not Found", "SUBSCRIPTION_NOT_FOUND", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle invalid state transition errors.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
            InvalidStateTransitionException ex, HttpServletRequest request) {
        logger.warn("Invalid state transition: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "INVALID_STATE_TRANSITION", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle duplicate subscription errors.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateSubscriptionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSubscription(
            DuplicateSubscriptionException ex, HttpServletRequest request) {
        logger.warn("Duplicate subscription attempt: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.CONFLICT, "Conflict", "DUPLICATE_SUBSCRIPTION", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle plan deletion with active subscriptions errors.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(PlanHasActiveSubscriptionsException.class)
    public ResponseEntity<ErrorResponse> handlePlanHasActiveSubscriptions(
            PlanHasActiveSubscriptionsException ex, HttpServletRequest request) {
        logger.warn("Plan deletion blocked: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.CONFLICT, "Conflict", "PLAN_HAS_ACTIVE_SUBSCRIPTIONS", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle invalid upgrade errors.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(InvalidUpgradeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUpgrade(
            InvalidUpgradeException ex, HttpServletRequest request) {
        logger.warn("Invalid upgrade attempt: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "INVALID_UPGRADE", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            org.springframework.dao.OptimisticLockingFailureException ex, HttpServletRequest request) {
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.CONFLICT, "Conflict", "OPTIMISTIC_LOCK_FAILURE", "Resource was modified by another transaction. Please refresh and try again.", request);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle rate limit exceeded errors.
     * Returns HTTP 429 Too Many Requests with Retry-After header.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletRequest request) {
        logger.warn("Rate limit exceeded for path {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "RATE_LIMIT_EXCEEDED", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    /**
     * Handle payment failed errors.
     * Returns HTTP 402 Payment Required.
     */
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(
            PaymentFailedException ex, HttpServletRequest request) {
        logger.warn("Payment failed: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.PAYMENT_REQUIRED, "Payment Required", "PAYMENT_FAILED", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    /**
     * Handle payment not found errors.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException ex, HttpServletRequest request) {
        logger.warn("Payment not found: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.NOT_FOUND, "Not Found", "PAYMENT_NOT_FOUND", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle file storage errors (file upload failures).
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(
            FileStorageException ex, HttpServletRequest request) {
        logger.warn("File storage error: {}", ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", "FILE_STORAGE_ERROR", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle security access denied exceptions.
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Access denied for path {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = createErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", "ACCESS_DENIED", "You do not have permission to access this resource", request);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle all other unexpected exceptions.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString(); // Generate unique error ID
        logger.error("Unexpected error [ID: {}] for path {}: {}", errorId, request.getRequestURI(), ex.getMessage(), ex);
        
        ErrorResponse error = createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please reference error ID: " + errorId, request);
        error.setErrorId(errorId); // Set the error ID in the response DTO
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
