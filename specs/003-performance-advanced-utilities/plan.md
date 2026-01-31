# Implementation Plan: Performance & Advanced Utilities

**Branch**: `003-performance-advanced-utilities` | **Date**: 2026-01-26 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/003-performance-advanced-utilities/spec.md`

## Summary

This plan implements tiered rate limiting, caching enhancements, and global exception handling improvements for the Enterprise SaaS Billing Engine. The implementation uses Bucket4j for token-bucket-based rate limiting (Free: 10 req/min, Pro: 100 req/min, Enterprise: 1000 req/min), enhances existing Caffeine cache configuration (1-hour TTL for plans), and extends the GlobalExceptionHandler with error codes, timestamps, and unique error IDs. All changes leverage existing Spring Boot 3.2.2 infrastructure with minimal new dependencies (Bucket4j only).

**Key Technical Decisions**:
- **Rate Limiting**: Bucket4j with Caffeine backend (in-memory, no Redis required for MVP)
- **Caching**: Enhance existing Caffeine configuration (change TTL from 10min to 1 hour)
- **Exception Handling**: Extend existing GlobalExceptionHandler and ErrorResponse DTO
- **No Database Changes**: All functionality uses in-memory structures and existing entities

---

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.2, Bucket4j 8.7.0, Caffeine (existing), Spring Security 6.x  
**Storage**: PostgreSQL (existing, no schema changes required)  
**Testing**: JUnit 5, Mockito, MockMvc, AssertJ  
**Target Platform**: JVM (Linux/Windows server)  
**Project Type**: Single monolith (Spring Boot application)  
**Performance Goals**: <1ms rate limit overhead, <5ms cache hit latency, >90% cache hit rate  
**Constraints**: <5ms total added latency, zero breaking changes to existing APIs  
**Scale/Scope**: 10,000 concurrent users, 100 req/sec per user (Pro plan)

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Compliance Status: PASSED

| Principle | Requirement | Status | Notes |
|-----------|-------------|--------|-------|
| **I. Layered Architecture** | Strict layer separation | ✅ PASS | RateLimitingFilter in security/, enhanced ErrorResponse in dto/, CacheConfig in config/ |
| **II. Domain-Driven Design** | Business concepts modeled | ✅ PASS | Rate limits derived from BillingPlan entity (domain concept) |
| **III. Security-First** | Security from day one | ✅ PASS | Rate limiting enforces tenant isolation via tenantId |
| **IV. Transaction Management** | State changes transactional | ✅ PASS | No new transactional operations (read-only rate limiting/caching) |
| **V. API Design Excellence** | RESTful, versioned, documented | ✅ PASS | All endpoints retain /api/v1/ prefix, OpenAPI contract provided |
| **VI. Test-First Development** | Tests before implementation | ✅ PASS | Test strategy defined in verification plan below |
| **VII. Exception Handling** | Global, consistent errors | ✅ PASS | Enhances existing GlobalExceptionHandler with error codes/timestamps |
| **VIII. Performance & Caching** | Performance from start | ✅ PASS | Core feature - implements caching and rate limiting |
| **IX. Data Integrity** | Multi-layer validation | ✅ PASS | No new data entities, existing validation remains |
| **X. External Integration Resilience** | Fault-tolerant integrations | ✅ PASS | Cache graceful degradation if Caffeine unavailable |

**Post-Design Re-Check**: ✅ PASSED - All principles maintained, no violations.

---

## Project Structure

### Documentation (this feature)

```text
specs/003-performance-advanced-utilities/
├── plan.md              # This file
├── research.md          # Technology research and decisions
├── data-model.md        # Data structures and entities
├── quickstart.md        # Developer guide
├── contracts/           # API contracts
│   └── error-handling-api.yaml  # OpenAPI spec for error responses
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
src/main/java/org/gb/billing/
├── config/
│   ├── CacheConfig.java              # [MODIFY] Update TTL to 1 hour
│   ├── RateLimitConfig.java          # [NEW] Bucket4j configuration
│   └── SecurityConfig.java           # [MODIFY] Register RateLimitingFilter
├── security/
│   └── RateLimitingFilter.java       # [NEW] Rate limiting filter
├── dto/
│   └── response/
│       └── ErrorResponse.java        # [MODIFY] Add errorCode, timestamp, errorId fields
├── exception/
│   ├── GlobalExceptionHandler.java   # [MODIFY] Add error codes, timestamps, 429 handler
│   └── RateLimitExceededException.java  # [NEW] Rate limit exception
└── service/
    └── PlanService.java              # [MODIFY] Add @CacheEvict to update/delete methods

src/test/java/org/gb/billing/
├── security/
│   └── RateLimitingFilterTest.java   # [NEW] Unit tests for rate limiting
├── exception/
│   └── GlobalExceptionHandlerTest.java  # [MODIFY] Add tests for new error fields
└── integration/
    └── RateLimitIntegrationTest.java # [NEW] End-to-end rate limiting tests

pom.xml                               # [MODIFY] Add Bucket4j dependencies
```

**Structure Decision**: Single project structure maintained. All new components follow existing layered architecture (config/, security/, dto/, exception/, service/). No new modules or packages required.

---

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**No violations** - Constitution check passed. No complexity justification required.

---

## Proposed Changes

Changes are grouped by component and ordered by dependency (foundational changes first).

---

### Component 1: Dependencies & Configuration

#### [MODIFY] [pom.xml](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/pom.xml)

**Purpose**: Add Bucket4j dependencies for rate limiting.

**Changes**:
```xml
<!-- Add after existing dependencies, before </dependencies> -->
<!-- Bucket4j for rate limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>

<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-caffeine</artifactId>
    <version>8.7.0</version>
</dependency>
```

**Rationale**: Bucket4j provides token-bucket rate limiting with Caffeine integration. Version 8.7.0 is latest stable, compatible with Java 17.

---

#### [MODIFY] [CacheConfig.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/config/CacheConfig.java)

**Purpose**: Update plan cache TTL from 10 minutes to 1 hour per specification.

**Changes**:
```java
// Line 43: Change expireAfterWrite from 10 minutes to 1 hour
cacheManager.registerCustomCache("plans", 
    Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)  // Changed from 10 MINUTES
        .maximumSize(1000)
        .recordStats()
        .build());
```

**Rationale**: Plans change infrequently (maybe weekly), so 1-hour TTL balances freshness with performance. Current 10-minute TTL is too aggressive.

---

#### [NEW] [RateLimitConfig.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/config/RateLimitConfig.java)

**Purpose**: Configure Bucket4j with Caffeine backend for rate limiting.

**Implementation**:
```java
package org.gb.billing.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitConfig {
    
    @Bean
    public ProxyManager<String> rateLimitBuckets() {
        return new CaffeineProxyManager<>(
            Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build()
        );
    }
}
```

**Rationale**: ProxyManager stores rate limit buckets keyed by tenantId. 10-minute expiration removes inactive users, 10,000 max size supports large user base.

---

### Component 2: Rate Limiting Implementation

#### [NEW] [RateLimitExceededException.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/exception/RateLimitExceededException.java)

**Purpose**: Exception thrown when user exceeds rate limit.

**Implementation**:
```java
package org.gb.billing.exception;

public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(long retryAfterSeconds) {
        super("API rate limit exceeded. Please try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
```

**Rationale**: Encapsulates retry-after information for HTTP 429 response.

---

#### [NEW] [RateLimitingFilter.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/security/RateLimitingFilter.java)

**Purpose**: Servlet filter that enforces rate limits on all authenticated requests.

**Implementation**:
```java
package org.gb.billing.security;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.exception.RateLimitExceededException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final ProxyManager<String> buckets;
    
    public RateLimitingFilter(ProxyManager<String> buckets) {
        this.buckets = buckets;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Skip rate limiting for auth endpoints
        if (request.getRequestURI().startsWith("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String tenantId = userDetails.getTenantId().toString();
        
        // Get plan-based rate limit
        BillingPlan plan = userDetails.getSubscription().getPlan();
        Bandwidth limit = getRateLimitForPlan(plan.getName());
        
        // Get or create bucket
        Bucket bucket = buckets.builder().build(tenantId, () -> 
            BucketConfiguration.builder()
                .addLimit(limit)
                .build()
        );
        
        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(
            System.currentTimeMillis() / 1000 + 60  // Reset in 60 seconds
        ));
        
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long retryAfter = probe.getNanosToWaitForRefill() / 1_000_000_000;
            throw new RateLimitExceededException(retryAfter);
        }
    }
    
    private Bandwidth getRateLimitForPlan(String planName) {
        return switch (planName.toLowerCase()) {
            case "free" -> Bandwidth.simple(10, Duration.ofMinutes(1));
            case "pro" -> Bandwidth.simple(100, Duration.ofMinutes(1));
            case "enterprise" -> Bandwidth.simple(1000, Duration.ofMinutes(1));
            default -> Bandwidth.simple(10, Duration.ofMinutes(1));  // Default to Free
        };
    }
}
```

**Rationale**: OncePerRequestFilter ensures rate limiting runs once per request. Uses tenantId as bucket key for multi-tenant isolation. Skips /auth endpoints (login/register don't need rate limiting).

---

#### [MODIFY] [SecurityConfig.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/config/SecurityConfig.java)

**Purpose**: Register RateLimitingFilter in security filter chain.

**Changes**:
```java
// Add to SecurityFilterChain bean
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .anyRequest().authenticated()
    )
    .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)  // Add this line
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
    // ... rest of configuration
```

**Rationale**: Rate limiting must run before JWT authentication to include headers in all responses.

---

### Component 3: Exception Handling Enhancements

#### [MODIFY] [ErrorResponse.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/dto/response/ErrorResponse.java)

**Purpose**: Add error codes, timestamps, and error IDs to standardized error response.

**Changes**:
```java
// Add new fields
private String errorCode;        // Machine-readable error code
private String timestamp;        // ISO 8601 timestamp
private String errorId;          // UUID for 500 errors (nullable)
private Map<String, String> details;  // Additional context (nullable)

// Update constructor
public ErrorResponse(int status, String error, String errorCode, 
                     String message, String timestamp, String path) {
    this.status = status;
    this.error = error;
    this.errorCode = errorCode;
    this.message = message;
    this.timestamp = timestamp;
    this.path = path;
}

// Add getters/setters for new fields
```

**Rationale**: Error codes enable client-side error handling, timestamps help debugging, error IDs correlate user reports with logs.

---

#### [MODIFY] [GlobalExceptionHandler.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/exception/GlobalExceptionHandler.java)

**Purpose**: Update all exception handlers to include error codes and timestamps.

**Changes**:

1. **Update existing handlers** (10 handlers total):
```java
// Example: SubscriptionNotFoundException handler
@ExceptionHandler(SubscriptionNotFoundException.class)
public ResponseEntity<ErrorResponse> handleSubscriptionNotFound(
        SubscriptionNotFoundException ex, HttpServletRequest request) {
    logger.warn("Subscription not found: {}", ex.getMessage());
    
    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        "SUBSCRIPTION_NOT_FOUND",  // NEW: Error code
        ex.getMessage(),
        Instant.now().toString(),  // NEW: Timestamp
        request.getRequestURI()
    );
    
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}

// Repeat for all 10 existing handlers:
// - DuplicateEmailException → DUPLICATE_EMAIL
// - InvalidCredentialsException → INVALID_CREDENTIALS
// - ExpiredTokenException → TOKEN_EXPIRED
// - PlanNotFoundException → PLAN_NOT_FOUND
// - InvalidStateTransitionException → INVALID_STATE_TRANSITION
// - DuplicateSubscriptionException → DUPLICATE_SUBSCRIPTION
// - PlanHasActiveSubscriptionsException → PLAN_HAS_ACTIVE_SUBSCRIPTIONS
// - InvalidUpgradeException → INVALID_UPGRADE
// - OptimisticLockingFailureException → OPTIMISTIC_LOCK_FAILURE
// - MethodArgumentNotValidException → VALIDATION_ERROR
```

2. **Add HTTP 429 handler**:
```java
@ExceptionHandler(RateLimitExceededException.class)
public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
        RateLimitExceededException ex, HttpServletRequest request) {
    logger.warn("Rate limit exceeded for path: {}", request.getRequestURI());
    
    ErrorResponse error = new ErrorResponse(
        HttpStatus.TOO_MANY_REQUESTS.value(),
        "Too Many Requests",
        "RATE_LIMIT_EXCEEDED",
        ex.getMessage(),
        Instant.now().toString(),
        request.getRequestURI()
    );
    
    error.setDetails(Map.of("retryAfter", String.valueOf(ex.getRetryAfterSeconds())));
    
    HttpHeaders headers = new HttpHeaders();
    headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
    
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .headers(headers)
        .body(error);
}
```

3. **Update 500 handler with error ID**:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex, HttpServletRequest request) {
    String errorId = UUID.randomUUID().toString();  // NEW: Generate error ID
    logger.error("Unexpected error [{}] for path {}: {}", 
        errorId, request.getRequestURI(), ex.getMessage(), ex);
    
    ErrorResponse error = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please try again later.",
        Instant.now().toString(),
        request.getRequestURI()
    );
    error.setErrorId(errorId);  // NEW: Include error ID
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
```

**Rationale**: Consistent error codes across all handlers enable client-side error handling. Error IDs for 500 errors help support teams correlate user reports with server logs.

---

### Component 4: Caching Enhancements

#### [MODIFY] [PlanService.java](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/src/main/java/org/gb/billing/service/PlanService.java)

**Purpose**: Add cache eviction to plan modification methods.

**Changes**:
```java
// Existing method already has @Cacheable
@Cacheable(value = "plans", sync = true)  // Add sync = true to prevent cache stampede
public List<PlanResponseDTO> getAllActivePlans() {
    // Existing implementation
}

// Add @CacheEvict to modification methods
@CacheEvict(value = "plans", allEntries = true)
public PlanResponseDTO createPlan(CreatePlanRequest request) {
    // Existing implementation
}

@CacheEvict(value = "plans", allEntries = true)
public PlanResponseDTO updatePlan(UUID planId, UpdatePlanRequest request) {
    // Existing implementation
}

@CacheEvict(value = "plans", allEntries = true)
public void deletePlan(UUID planId) {
    // Existing implementation
}
```

**Rationale**: `@CacheEvict` ensures cache is cleared when plans are modified, preventing stale data. `sync = true` prevents multiple threads from loading cache simultaneously (cache stampede protection).

---

## Verification Plan

### Automated Tests

#### 1. Rate Limiting Unit Tests

**File**: `src/test/java/org/gb/billing/security/RateLimitingFilterTest.java` (NEW)

**Test Cases**:
- `shouldAllowRequestsWithinRateLimit_FreePlan()` - Verify 10 requests succeed
- `shouldBlockRequestsExceedingRateLimit_FreePlan()` - Verify 11th request returns 429
- `shouldAllowRequestsWithinRateLimit_ProPlan()` - Verify 100 requests succeed
- `shouldIncludeRateLimitHeaders()` - Verify X-RateLimit-* headers present
- `shouldIncludeRetryAfterHeader_WhenRateLimited()` - Verify Retry-After header
- `shouldResetRateLimitAfterTimeWindow()` - Verify limits reset after 1 minute

**Run Command**:
```bash
./mvnw test -Dtest=RateLimitingFilterTest
```

---

#### 2. Exception Handling Unit Tests

**File**: `src/test/java/org/gb/billing/exception/GlobalExceptionHandlerTest.java` (MODIFY)

**New Test Cases**:
- `shouldIncludeErrorCodeInResponse()` - Verify errorCode field present
- `shouldIncludeTimestampInResponse()` - Verify timestamp field present
- `shouldIncludeErrorIdFor500Errors()` - Verify errorId in 500 responses
- `shouldHandleRateLimitExceeded()` - Verify 429 response format
- `shouldIncludeRetryAfterInRateLimitResponse()` - Verify Retry-After header

**Run Command**:
```bash
./mvnw test -Dtest=GlobalExceptionHandlerTest
```

---

#### 3. Rate Limiting Integration Tests

**File**: `src/test/java/org/gb/billing/integration/RateLimitIntegrationTest.java` (NEW)

**Test Cases**:
- `shouldEnforceRateLimitForFreePlan()` - End-to-end test with real HTTP requests
- `shouldEnforceRateLimitForProPlan()` - End-to-end test with Pro plan user
- `shouldNotRateLimitAuthEndpoints()` - Verify /auth/** endpoints bypass rate limiting
- `shouldIncludeRateLimitHeadersInAllResponses()` - Verify headers in 200, 404, 500 responses

**Run Command**:
```bash
./mvnw test -Dtest=RateLimitIntegrationTest
```

---

#### 4. Caching Tests

**File**: `src/test/java/org/gb/billing/service/PlanServiceTest.java` (MODIFY)

**New Test Cases**:
- `shouldCachePlanList()` - Verify second call doesn't hit database
- `shouldEvictCacheOnPlanUpdate()` - Verify cache cleared after update
- `shouldEvictCacheOnPlanDelete()` - Verify cache cleared after delete
- `shouldPreventCacheStampede()` - Verify only one DB query on concurrent cache misses

**Run Command**:
```bash
./mvnw test -Dtest=PlanServiceTest
```

---

#### 5. Full Test Suite

**Run All Tests**:
```bash
./mvnw clean test
```

**Expected Output**:
- All tests pass
- Coverage >80% for new classes (RateLimitingFilter, RateLimitConfig)
- No regressions in existing tests

---

### Manual Verification

#### 1. Rate Limiting Manual Test

**Prerequisites**:
- Application running locally (`./mvnw spring-boot:run`)
- User account with Free plan (10 req/min limit)

**Steps**:
```bash
# 1. Login and get JWT token
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"freeuser@example.com","password":"password"}' \
  | jq -r '.token')

# 2. Make 10 requests - all should succeed
for i in {1..10}; do
  echo "Request $i:"
  curl -i http://localhost:8080/api/v1/plans \
    -H "Authorization: Bearer $TOKEN" \
    | grep -E "HTTP|X-RateLimit"
done

# 3. Make 11th request - should return 429
echo "Request 11 (should be rate limited):"
curl -i http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Results**:
- Requests 1-10: HTTP 200, X-RateLimit-Remaining decreases (10, 9, 8, ...)
- Request 11: HTTP 429, Retry-After header present, error code "RATE_LIMIT_EXCEEDED"

---

#### 2. Caching Manual Test

**Steps**:
```bash
# 1. Clear cache (restart application)
./mvnw spring-boot:run

# 2. First request (cache miss) - measure time
time curl http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $TOKEN"
# Expected: ~100ms (database query)

# 3. Second request (cache hit) - measure time
time curl http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $TOKEN"
# Expected: <10ms (from cache)

# 4. Update a plan (triggers cache eviction)
curl -X PUT http://localhost:8080/api/v1/plans/{planId} \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"price": 39.99}'

# 5. Next request should be cache miss again
time curl http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $TOKEN"
# Expected: ~100ms (cache was evicted)
```

**Expected Results**:
- First request: Slow (~100ms)
- Second request: Fast (<10ms)
- After plan update: Slow again (cache evicted)

---

#### 3. Error Response Manual Test

**Steps**:
```bash
# 1. Test 404 error
curl -i http://localhost:8080/api/v1/subscriptions/invalid-id \
  -H "Authorization: Bearer $TOKEN"

# Expected response:
# HTTP/1.1 404 Not Found
# {
#   "status": 404,
#   "error": "Not Found",
#   "errorCode": "SUBSCRIPTION_NOT_FOUND",
#   "message": "Subscription with ID invalid-id not found",
#   "timestamp": "2026-01-26T10:00:00Z",
#   "path": "/api/v1/subscriptions/invalid-id"
# }

# 2. Test validation error
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"notanemail","password":"123"}'

# Expected: HTTP 400, errorCode "VALIDATION_ERROR", errors array with field details

# 3. Test rate limit error (make 11 requests as shown in test 1)
# Expected: HTTP 429, errorCode "RATE_LIMIT_EXCEEDED", Retry-After header
```

**Expected Results**:
- All error responses include errorCode, timestamp, and path
- Validation errors include errors array with field-level details
- 500 errors include errorId (trigger by causing an unexpected error)

---

### Performance Verification

**Monitor Cache Hit Rate**:
```bash
curl http://localhost:8080/actuator/metrics/cache.gets?tag=cache:plans&tag=result:hit
curl http://localhost:8080/actuator/metrics/cache.gets?tag=cache:plans&tag=result:miss

# Calculate hit rate = hits / (hits + misses)
# Target: >90% hit rate after warm-up
```

**Monitor Rate Limiting Performance**:
```bash
# Use JMeter or Apache Bench to send 1000 requests
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/plans

# Verify:
# - Average response time increase <5ms (rate limiting overhead)
# - 429 responses for requests exceeding limit
```

---

## Success Criteria Verification

| Success Criterion | Verification Method | Target | How to Verify |
|-------------------|---------------------|--------|---------------|
| SC-001: Free users blocked after 10 req/min | Integration test | 100% accuracy | Run RateLimitIntegrationTest |
| SC-002: Pro users blocked after 100 req/min | Integration test | 100% accuracy | Run RateLimitIntegrationTest |
| SC-003: Rate limit overhead <5ms | Performance test | <5ms | Apache Bench comparison |
| SC-004: Cached responses <50ms | Manual test | 95% <50ms | `time curl` command |
| SC-005: Cache hit rate >90% | Actuator metrics | >90% | Check /actuator/metrics |
| SC-006: DB load reduced 85% | Actuator metrics | 85% reduction | Compare cache hits vs misses |
| SC-007: Standardized error format | Integration test | 100% consistency | GlobalExceptionHandlerTest |
| SC-008: Error self-service 90% | Manual review | 90% | Review error messages |
| SC-009: Support tickets reduced 40% | Post-deployment metric | 40% reduction | Monitor support system |
| SC-010: 99.9% uptime with cache down | Manual test | 99.9% | Disable cache, verify degradation |
| SC-011: 10,000 concurrent users | Load test | No counter drift | JMeter with 10k threads |
| SC-012: Error responses <100ms | Performance test | <100ms | Measure error response time |

---

## Deployment Notes

### Pre-Deployment Checklist
- [ ] All tests pass (`./mvnw clean test`)
- [ ] Code coverage >80% for new classes
- [ ] OpenAPI documentation updated
- [ ] No breaking changes to existing APIs
- [ ] Performance benchmarks meet targets

### Deployment Steps
1. Merge feature branch to main
2. Deploy to staging environment
3. Run smoke tests (rate limiting, caching, error handling)
4. Monitor metrics for 24 hours
5. Deploy to production
6. Monitor error rates and cache hit rates

### Rollback Plan
- No database migrations required - safe to rollback
- Remove Bucket4j dependencies and RateLimitingFilter
- Revert CacheConfig TTL to 10 minutes
- Revert ErrorResponse DTO changes

---

## Post-Implementation Tasks

1. **Monitor Metrics** (first week):
   - Cache hit rate (target >90%)
   - Rate limit violations per plan tier
   - Error response times
   - 500 error rate (should remain <0.1%)

2. **Tune Configuration** (if needed):
   - Adjust cache TTL based on plan update frequency
   - Adjust rate limits based on actual usage patterns
   - Optimize bucket expiration time

3. **Documentation Updates**:
   - Update API documentation with rate limit headers
   - Add error code reference to developer docs
   - Create runbook for rate limit monitoring

4. **Future Enhancements** (out of scope):
   - Migrate to Redis for distributed rate limiting (multi-instance deployment)
   - Add per-endpoint rate limits (e.g., stricter limits on expensive operations)
   - Implement rate limit exemptions for admin users
   - Add configurable rate limits per plan in database

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Rate limiting blocks legitimate users | Low | High | Thorough testing, gradual rollout, monitoring |
| Cache stampede on expiration | Low | Medium | `sync = true` prevents concurrent loads |
| Bucket4j performance issues | Low | Medium | Caffeine backend is in-memory (fast) |
| Breaking changes to error responses | Low | High | Additive changes only (new fields) |
| Memory exhaustion from buckets | Low | Medium | 10k max size, 10min expiration |

---

## Conclusion

This implementation plan delivers tiered rate limiting, caching enhancements, and global exception handling improvements with minimal risk and zero breaking changes. All functionality leverages existing Spring Boot infrastructure (Caffeine, Spring Security, GlobalExceptionHandler) with only one new dependency (Bucket4j). The plan includes comprehensive testing strategy (unit, integration, manual) and clear verification criteria for all 12 success criteria from the specification.

**Next Steps**: Proceed to `/speckit.tasks` to generate actionable task breakdown for implementation.
