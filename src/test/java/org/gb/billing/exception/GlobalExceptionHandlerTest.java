package org.gb.billing.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.gb.billing.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void testHandlePlanNotFound() {
        PlanNotFoundException ex = new PlanNotFoundException(UUID.randomUUID());
        ResponseEntity<ErrorResponse> response = handler.handlePlanNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("PLAN_NOT_FOUND", response.getBody().getErrorCode());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void testHandleRateLimitExceeded() {
        RateLimitExceededException ex = new RateLimitExceededException(30);
        ResponseEntity<ErrorResponse> response = handler.handleRateLimitExceeded(ex, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("RATE_LIMIT_EXCEEDED", response.getBody().getErrorCode());
        assertEquals("30", response.getHeaders().getFirst("Retry-After"));
    }

    @Test
    void testHandleGenericException() {
        Exception ex = new Exception("Something went wrong");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
        assertNotNull(response.getBody().getErrorId());
        assertTrue(response.getBody().getMessage().contains(response.getBody().getErrorId()));
    }
}
