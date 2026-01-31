package org.gb.billing.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.Subscription;
import org.gb.billing.exception.RateLimitExceededException;
import org.gb.billing.service.SubscriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Servlet filter that enforces API rate limiting based on user subscription plans.
 * 
 * Rate Limits:
 * - Free plan: 10 requests per minute
 * - Pro plan: 100 requests per minute
 * - Enterprise plan: 1000 requests per minute
 * 
 * Rate limit information is included in response headers:
 * - X-RateLimit-Limit: Maximum requests allowed
 * - X-RateLimit-Remaining: Requests remaining in current window
 * - X-RateLimit-Reset: Unix timestamp when limit resets
 * 
 * When rate limit is exceeded, throws RateLimitExceededException which is
 * handled by GlobalExceptionHandler to return HTTP 429 with Retry-After header.
 * 
 * @see org.gb.billing.config.RateLimitConfig for bucket configuration
 * @see org.gb.billing.exception.GlobalExceptionHandler for 429 handling
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final ProxyManager<String> buckets;
    private final SubscriptionService subscriptionService;
    
    public RateLimitingFilter(ProxyManager<String> buckets, SubscriptionService subscriptionService) {
        this.buckets = buckets;
        this.subscriptionService = subscriptionService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Skip rate limiting for auth endpoints (login/register don't need rate limiting)
        if (request.getRequestURI().startsWith("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip rate limiting for unauthenticated requests (will be handled by security)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getUserId();
        Long tenantId = userDetails.getTenantId();
        
        // Get user's active subscription to determine plan (Cached)
        Optional<Subscription> subscriptionOpt = subscriptionService.getMySubscription(userId, tenantId);
        
        // Default to Free plan if no subscription found
        String planName = subscriptionOpt
            .map(Subscription::getPlan)
            .map(BillingPlan::getName)
            .orElse("Free");
        
        // Get plan-based rate limit
        Bandwidth limit = getRateLimitForPlan(planName);
        
        // Get or create bucket for this tenant (keyed by tenantId for multi-tenant isolation)
        Bucket bucket = buckets.builder().build(tenantId.toString(), () -> 
            BucketConfiguration.builder()
                .addLimit(limit)
                .build()
        );
        
        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        // Add rate limit headers to response
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(
            System.currentTimeMillis() / 1000 + 60  // Reset in 60 seconds (1 minute window)
        ));
        
        if (probe.isConsumed()) {
            // Token consumed successfully, proceed with request
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded, throw exception
            long retryAfter = probe.getNanosToWaitForRefill() / 1_000_000_000;
            throw new RateLimitExceededException(retryAfter);
        }
    }
    
    /**
     * Maps plan name to rate limit configuration.
     * 
     * @param planName name of the billing plan (case-insensitive)
     * @return Bandwidth configuration for the plan
     */
    private Bandwidth getRateLimitForPlan(String planName) {
        return switch (planName.toLowerCase()) {
            case "free" -> Bandwidth.simple(10, Duration.ofMinutes(1));
            case "pro" -> Bandwidth.simple(100, Duration.ofMinutes(1));
            case "enterprise" -> Bandwidth.simple(1000, Duration.ofMinutes(1));
            default -> Bandwidth.simple(10, Duration.ofMinutes(1));  // Default to Free tier
        };
    }
}
