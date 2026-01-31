# Research: Performance & Advanced Utilities

**Feature**: Performance & Advanced Utilities  
**Branch**: `003-performance-advanced-utilities`  
**Date**: 2026-01-26

## Executive Summary

This document consolidates research findings for implementing tiered rate limiting, caching enhancements, and global exception handling improvements in the Enterprise SaaS Billing Engine. All decisions align with the project constitution and leverage existing Spring Boot 3.2.2 infrastructure.

---

## 1. Rate Limiting Implementation

### Decision: Bucket4j with Caffeine Backend

**Rationale**:
- **Bucket4j** is the industry-standard Java rate limiting library implementing token bucket algorithm
- Integrates seamlessly with existing Caffeine cache (already in pom.xml)
- Supports distributed rate limiting (future Redis migration path)
- Provides flexible configuration for tiered limits based on subscription plans
- Zero additional infrastructure required (uses existing Caffeine cache)

**Alternatives Considered**:

| Alternative | Pros | Cons | Verdict |
|-------------|------|------|---------|
| **Spring Cloud Gateway** | Built-in rate limiting, production-ready | Requires separate gateway service, overkill for monolith | ❌ Rejected - adds unnecessary complexity |
| **Resilience4j RateLimiter** | Already familiar (used for circuit breakers) | Less flexible for multi-tier scenarios, no distributed support | ❌ Rejected - limited tiered support |
| **Custom Implementation** | Full control, no dependencies | High maintenance, reinventing the wheel, error-prone | ❌ Rejected - violates DRY principle |
| **Bucket4j** | Token bucket algorithm, tiered support, Caffeine integration | Requires new dependency | ✅ **Selected** |

**Implementation Approach**:
- Add `bucket4j-core` dependency to pom.xml
- Create `RateLimitingFilter` extending `OncePerRequestFilter`
- Store rate limit buckets in Caffeine cache keyed by `tenantId`
- Retrieve plan limits from `CustomUserDetails` (already contains tenant context)
- Return HTTP 429 with `Retry-After` header when limit exceeded
- Add rate limit headers (`X-RateLimit-*`) to all responses

**Key Configuration**:
```java
// Free: 10 req/min = 1 token every 6 seconds
Bandwidth.simple(10, Duration.ofMinutes(1))

// Pro: 100 req/min = 1 token every 0.6 seconds  
Bandwidth.simple(100, Duration.ofMinutes(1))

// Enterprise: 1000 req/min = 1 token every 0.06 seconds
Bandwidth.simple(1000, Duration.ofMinutes(1))
```

**Performance Impact**: <1ms overhead per request (in-memory bucket lookup)

---

## 2. Caching Strategy Enhancements

### Decision: Enhance Existing Caffeine Configuration

**Current State**:
- Caffeine cache already configured in `CacheConfig.java`
- Plans cache: 10-minute TTL, 1000 max entries
- `@Cacheable("plans")` annotation on `PlanService.getAllActivePlans()`

**Required Enhancements**:

#### 2.1 Reduce Plan Cache TTL to 1 Hour (Per Spec)
**Rationale**: Current 10-minute TTL is too aggressive. Plans change infrequently (maybe once per week), so 1-hour TTL balances freshness with performance.

**Change**: Update `CacheConfig.java` to use 1-hour expiration:
```java
.expireAfterWrite(1, TimeUnit.HOURS)  // Changed from 10 minutes
```

#### 2.2 Add Cache Eviction on Plan Modifications
**Rationale**: Ensure cache consistency when admins update plans.

**Implementation**:
- Add `@CacheEvict(value = "plans", allEntries = true)` to:
  - `PlanService.createPlan()`
  - `PlanService.updatePlan()`
  - `PlanService.deletePlan()`

#### 2.3 Prevent Cache Stampede
**Rationale**: When cache expires, multiple concurrent requests could trigger multiple DB queries.

**Implementation**:
- Caffeine's `recordStats()` already enabled for monitoring
- Use `@Cacheable` with `sync = true` to ensure only one thread loads cache on miss
- Add cache warming on application startup for critical data

**Alternatives Considered**:

| Alternative | Pros | Cons | Verdict |
|-------------|------|------|---------|
| **Redis** | Distributed, shared across instances | Requires Redis infrastructure, network latency | ❌ Rejected - overkill for current scale |
| **Caffeine (current)** | In-memory, ultra-fast, no infrastructure | Not shared across instances | ✅ **Selected** - sufficient for MVP |
| **Spring Cache Abstraction** | Vendor-neutral | Already using it | ✅ **Keep** |

**Performance Targets**:
- Cache hit latency: <5ms (Caffeine in-memory lookup)
- Cache miss latency: <100ms (DB query + cache population)
- Cache hit rate: >90% (plans rarely change)

---

## 3. Global Exception Handling Enhancements

### Decision: Enhance Existing GlobalExceptionHandler

**Current State**:
- `GlobalExceptionHandler.java` already implements `@RestControllerAdvice`
- Returns standardized `ErrorResponse` DTO with:
  - `status` (HTTP code)
  - `error` (HTTP status text)
  - `message` (user-friendly message)
  - `path` (request URI)
- Handles 10+ domain exceptions (DuplicateEmailException, PlanNotFoundException, etc.)
- Logs errors appropriately (WARN for client errors, ERROR for server errors)

**Required Enhancements**:

#### 3.1 Add Error Codes to ErrorResponse
**Rationale**: Machine-readable error codes enable better client-side error handling.

**Change**: Update `ErrorResponse` DTO to include `errorCode` field:
```java
private String errorCode;  // e.g., "SUBSCRIPTION_NOT_FOUND", "PAYMENT_FAILED"
```

**Mapping Strategy**:
- Use exception class name as base (e.g., `SubscriptionNotFoundException` → `SUBSCRIPTION_NOT_FOUND`)
- Add `getErrorCode()` method to custom exceptions for override capability

#### 3.2 Add Unique Error IDs for 500 Errors
**Rationale**: Correlate user reports with server logs for debugging.

**Implementation**:
- Generate UUID for each 500 error
- Include in ErrorResponse and log message
- Example: `"errorId": "550e8400-e29b-41d4-a716-446655440000"`

#### 3.3 Add Timestamp to ErrorResponse
**Rationale**: Helps with debugging and log correlation.

**Change**: Add `timestamp` field (ISO 8601 format):
```java
private String timestamp = Instant.now().toString();
```

#### 3.4 Enhance Validation Error Details
**Current**: Returns list of `FieldError` objects  
**Enhancement**: Add example values for common validation failures

**Example**:
```json
{
  "field": "email",
  "message": "Email format is invalid",
  "rejectedValue": "notanemail",
  "example": "user@example.com"
}
```

#### 3.5 Add HTTP 429 Handler for Rate Limiting
**New Exception**: `RateLimitExceededException`

**Handler**:
```java
@ExceptionHandler(RateLimitExceededException.class)
public ResponseEntity<ErrorResponse> handleRateLimitExceeded(...)
```

**Response**:
- HTTP 429 (Too Many Requests)
- `Retry-After` header (seconds until reset)
- Error code: `RATE_LIMIT_EXCEEDED`
- Message: "API rate limit exceeded. Please try again in {seconds} seconds."

**Alternatives Considered**:

| Alternative | Pros | Cons | Verdict |
|-------------|------|------|---------|
| **RFC 7807 Problem Details** | Industry standard, rich metadata | More complex, requires new DTO | ❌ Rejected - current ErrorResponse sufficient |
| **Custom ErrorResponse (current)** | Simple, already implemented | Not a standard | ✅ **Enhance** - add missing fields |
| **Spring Boot Default** | Zero code | Inconsistent, exposes stack traces | ❌ Rejected - poor UX |

---

## 4. Technology Stack Summary

### Core Dependencies (Existing)
- **Spring Boot**: 3.2.2
- **Java**: 17
- **Caffeine Cache**: Already configured
- **Spring Security**: 6.x (JWT authentication)
- **PostgreSQL**: Production database
- **Lombok**: Code generation

### New Dependencies Required
```xml
<!-- Bucket4j for rate limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>

<!-- Bucket4j Caffeine integration -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-caffeine</artifactId>
    <version>8.7.0</version>
</dependency>
```

**Version Justification**:
- Bucket4j 8.7.0: Latest stable, compatible with Java 17
- No breaking changes from 8.x series

---

## 5. Performance Considerations

### Rate Limiting Performance
- **Bucket lookup**: O(1) from Caffeine cache (~0.5ms)
- **Token consumption**: O(1) atomic operation (~0.1ms)
- **Total overhead**: <1ms per request
- **Memory**: ~200 bytes per bucket × 10,000 users = ~2MB

### Caching Performance
- **Cache hit**: <5ms (Caffeine in-memory)
- **Cache miss**: ~50-100ms (PostgreSQL query)
- **Memory**: ~1KB per plan × 1000 max = ~1MB
- **Hit rate target**: >90%

### Exception Handling Performance
- **Error response generation**: <1ms
- **Logging**: Async (non-blocking)
- **Total overhead**: <2ms per error

**Total Performance Budget**: <5ms added latency for rate limiting + caching + error handling

---

## 6. Security Considerations

### Rate Limiting Security
- **Tenant Isolation**: Rate limits enforced per `tenantId` (from JWT)
- **Bypass Protection**: Unauthenticated requests get strictest limits (10 req/min)
- **DDoS Mitigation**: Rate limiting provides basic DDoS protection

### Cache Security
- **Data Isolation**: Plans are public data (no tenant-specific caching)
- **Cache Poisoning**: Caffeine is in-memory, no external cache poisoning risk

### Error Handling Security
- **Information Disclosure**: Never expose stack traces in production
- **Sensitive Data**: Never log passwords, tokens, or payment details
- **Error IDs**: UUIDs are non-sequential (no enumeration attacks)

---

## 7. Testing Strategy

### Rate Limiting Tests
- **Unit Tests**: Bucket configuration, token consumption logic
- **Integration Tests**: 
  - Verify 10 requests succeed, 11th fails (Free plan)
  - Verify 100 requests succeed, 101st fails (Pro plan)
  - Verify rate limit headers in responses
  - Verify `Retry-After` header accuracy

### Caching Tests
- **Unit Tests**: Cache configuration, eviction logic
- **Integration Tests**:
  - Verify cache hit on second request
  - Verify cache eviction on plan update
  - Measure cache hit rate
  - Verify graceful degradation if cache unavailable

### Exception Handling Tests
- **Unit Tests**: ErrorResponse DTO construction
- **Integration Tests**:
  - Trigger each exception type, verify HTTP status
  - Verify error code consistency
  - Verify validation error details
  - Verify 500 errors include error ID

---

## 8. Monitoring & Observability

### Metrics to Track
- **Rate Limiting**:
  - Requests allowed vs. rejected per plan tier
  - Average tokens remaining per user
  - Rate limit violations per endpoint

- **Caching**:
  - Cache hit/miss ratio
  - Cache eviction count
  - Average cache lookup time

- **Error Handling**:
  - Error count by HTTP status code
  - Error count by error code
  - 500 error rate (should be <0.1%)

### Implementation
- Use Spring Boot Actuator metrics (already configured)
- Caffeine `recordStats()` already enabled
- Add custom Micrometer counters for rate limiting

---

## 9. Migration Path

### Phase 1: Rate Limiting (P1)
1. Add Bucket4j dependencies
2. Create `RateLimitingFilter`
3. Configure buckets per plan tier
4. Add rate limit headers
5. Create `RateLimitExceededException` and handler

### Phase 2: Caching Enhancements (P2)
1. Update cache TTL to 1 hour
2. Add `@CacheEvict` to plan modification methods
3. Enable cache synchronization (`sync = true`)
4. Add cache warming on startup

### Phase 3: Exception Handling (P3)
1. Add `errorCode` field to `ErrorResponse`
2. Add `timestamp` field to `ErrorResponse`
3. Add `errorId` for 500 errors
4. Enhance validation error details
5. Update all exception handlers

---

## 10. Open Questions & Assumptions

### Assumptions Made
1. **Single Instance Deployment**: Using Caffeine (in-memory) assumes single instance. If multi-instance, need Redis.
2. **Plan Tier Mapping**: Assuming plan names are "Free", "Pro", "Enterprise" (case-insensitive).
3. **Rate Limit Scope**: Applied to all `/api/v1/**` endpoints except `/auth/**` (login/register).
4. **Cache Warming**: Plans loaded on startup to avoid cold start penalty.

### Questions for Clarification
1. **Multi-Instance Deployment**: Is the application deployed across multiple instances? (Affects cache/rate limit strategy)
2. **Rate Limit Exemptions**: Should admin users bypass rate limits?
3. **Custom Plan Limits**: Should rate limits be configurable per plan in database vs. hardcoded?

**Recommendation**: Proceed with assumptions above. Can refactor to Redis-backed rate limiting if multi-instance deployment confirmed.

---

## 11. References

- [Bucket4j Documentation](https://bucket4j.com/)
- [Caffeine Cache Guide](https://github.com/ben-manes/caffeine/wiki)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [RFC 6585 - HTTP 429 Too Many Requests](https://tools.ietf.org/html/rfc6585#section-4)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
