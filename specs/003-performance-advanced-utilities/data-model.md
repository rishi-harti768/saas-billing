# Data Model: Performance & Advanced Utilities

**Feature**: Performance & Advanced Utilities  
**Branch**: `003-performance-advanced-utilities`  
**Date**: 2026-01-26

## Overview

This document defines the data structures and entities required for implementing tiered rate limiting, caching enhancements, and global exception handling. Most functionality leverages existing entities and in-memory structures rather than new database tables.

---

## 1. Rate Limiting Data Structures

### 1.1 RateLimitBucket (In-Memory Only)

**Purpose**: Represents a user's API usage quota for a specific time window using the token bucket algorithm.

**Storage**: Caffeine cache (in-memory), keyed by `tenantId`

**Structure** (Bucket4j `Bucket` object):
```java
// Managed by Bucket4j library
Bucket {
    - tokens: long                    // Current available tokens
    - capacity: long                  // Maximum tokens (e.g., 10 for Free, 100 for Pro)
    - refillRate: long                // Tokens added per time unit
    - refillPeriod: Duration          // Time window (1 minute)
    - lastRefillTime: Instant         // Last token refill timestamp
}
```

**Key**: `String tenantId` (from JWT `CustomUserDetails`)

**Lifecycle**:
- Created on first API request from a user
- Automatically evicted after 10 minutes of inactivity (Caffeine TTL)
- Recreated on next request with full token capacity

**No Database Table Required**: Rate limit state is ephemeral and doesn't need persistence.

---

### 1.2 RateLimitConfig (Configuration Class)

**Purpose**: Maps subscription plan names to rate limit configurations.

**Storage**: Java enum or configuration class (hardcoded)

**Structure**:
```java
public enum PlanRateLimit {
    FREE(10, Duration.ofMinutes(1)),
    PRO(100, Duration.ofMinutes(1)),
    ENTERPRISE(1000, Duration.ofMinutes(1));
    
    private final long capacity;
    private final Duration refillPeriod;
}
```

**Mapping Logic**:
- Extract plan name from `Subscription` entity via `CustomUserDetails`
- Look up rate limit configuration from enum
- Create Bucket4j `Bucket` with corresponding capacity

---

## 2. Caching Data Structures

### 2.1 CachedPlanList (Caffeine Cache Entry)

**Purpose**: Cached response for "List Plans" API endpoint.

**Storage**: Caffeine cache with key `"plans::all"`

**Structure**:
```java
// Cached value is List<PlanResponseDTO>
List<PlanResponseDTO> {
    - id: UUID
    - name: String
    - description: String
    - price: BigDecimal
    - billingCycle: String
    - featureLimits: List<FeatureLimitDTO>
}
```

**Cache Configuration**:
- **Key**: `"plans::all"` (constant)
- **TTL**: 1 hour (3600 seconds)
- **Max Size**: 1000 entries (shared with other caches)
- **Eviction**: Time-based + manual eviction on plan updates

**Eviction Triggers**:
- Automatic: After 1 hour (TTL expiration)
- Manual: When any plan is created, updated, or deleted (`@CacheEvict`)

**No Database Changes Required**: Uses existing `BillingPlan` entity.

---

### 2.2 Cache Statistics (Micrometer Metrics)

**Purpose**: Track cache performance for monitoring.

**Storage**: Micrometer registry (exposed via Spring Boot Actuator)

**Metrics**:
```java
cache.gets{cache="plans", result="hit"}     // Cache hits
cache.gets{cache="plans", result="miss"}    // Cache misses
cache.evictions{cache="plans"}              // Eviction count
cache.puts{cache="plans"}                   // Cache writes
```

**Access**: `/actuator/metrics/cache.gets?tag=cache:plans`

---

## 3. Exception Handling Data Structures

### 3.1 ErrorResponse (Enhanced DTO)

**Purpose**: Standardized error response returned to API clients.

**Storage**: Response body (not persisted)

**Current Structure** (from existing `ErrorResponse.java`):
```java
public class ErrorResponse {
    private int status;              // HTTP status code (e.g., 404)
    private String error;            // HTTP status text (e.g., "Not Found")
    private String message;          // User-friendly error message
    private String path;             // Request URI
    private List<FieldError> errors; // Validation errors (optional)
}
```

**Enhanced Structure** (additions marked with `// NEW`):
```java
public class ErrorResponse {
    private int status;
    private String error;
    private String errorCode;        // NEW: Machine-readable code (e.g., "SUBSCRIPTION_NOT_FOUND")
    private String message;
    private String timestamp;        // NEW: ISO 8601 timestamp (e.g., "2026-01-26T10:00:00Z")
    private String path;
    private String errorId;          // NEW: UUID for 500 errors (e.g., "550e8400-...")
    private List<FieldError> errors;
    private Map<String, String> details; // NEW: Additional context (optional)
}
```

**Field Descriptions**:

| Field | Type | Required | Example | Purpose |
|-------|------|----------|---------|---------|
| `status` | int | Yes | `404` | HTTP status code |
| `error` | String | Yes | `"Not Found"` | HTTP status text |
| `errorCode` | String | Yes | `"SUBSCRIPTION_NOT_FOUND"` | Machine-readable error identifier |
| `message` | String | Yes | `"Subscription with ID abc123 not found"` | Human-readable description |
| `timestamp` | String | Yes | `"2026-01-26T10:00:00Z"` | When error occurred (ISO 8601) |
| `path` | String | Yes | `"/api/v1/subscriptions/abc123"` | Request URI |
| `errorId` | String | No | `"550e8400-e29b-41d4-a716-446655440000"` | Unique ID for 500 errors |
| `errors` | List | No | `[{field: "email", message: "Invalid format"}]` | Validation errors |
| `details` | Map | No | `{"retryAfter": "60"}` | Additional context |

**Error Code Mapping**:

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `SubscriptionNotFoundException` | 404 | `SUBSCRIPTION_NOT_FOUND` |
| `PlanNotFoundException` | 404 | `PLAN_NOT_FOUND` |
| `InvalidCredentialsException` | 401 | `INVALID_CREDENTIALS` |
| `ExpiredTokenException` | 401 | `TOKEN_EXPIRED` |
| `DuplicateEmailException` | 409 | `DUPLICATE_EMAIL` |
| `InvalidStateTransitionException` | 400 | `INVALID_STATE_TRANSITION` |
| `RateLimitExceededException` | 429 | `RATE_LIMIT_EXCEEDED` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `Exception` (catch-all) | 500 | `INTERNAL_SERVER_ERROR` |

---

### 3.2 FieldError (Enhanced DTO)

**Purpose**: Detailed validation error for specific fields.

**Current Structure**:
```java
public static class FieldError {
    private String field;            // Field name (e.g., "email")
    private String message;          // Error message (e.g., "Email format is invalid")
    private Object rejectedValue;    // Value that failed validation
}
```

**Enhanced Structure**:
```java
public static class FieldError {
    private String field;
    private String message;
    private Object rejectedValue;
    private String example;          // NEW: Example of valid value (e.g., "user@example.com")
}
```

**Example Usage**:
```json
{
  "field": "email",
  "message": "Email format is invalid",
  "rejectedValue": "notanemail",
  "example": "user@example.com"
}
```

---

### 3.3 RateLimitExceededException (New Exception)

**Purpose**: Thrown when user exceeds their API rate limit.

**Structure**:
```java
public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;  // Seconds until rate limit resets
    
    public RateLimitExceededException(long retryAfterSeconds) {
        super("API rate limit exceeded. Please try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
```

**Handler Response**:
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "API rate limit exceeded. Please try again in 45 seconds.",
  "timestamp": "2026-01-26T10:00:00Z",
  "path": "/api/v1/subscriptions",
  "details": {
    "retryAfter": "45"
  }
}
```

**HTTP Headers**:
```
HTTP/1.1 429 Too Many Requests
Retry-After: 45
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706270445
```

---

## 4. Existing Entities (No Changes Required)

### 4.1 BillingPlan Entity

**Relevance**: Used to determine rate limits based on plan name.

**Key Fields**:
- `name`: String (e.g., "Free", "Pro", "Enterprise")
- `price`: BigDecimal
- `billingCycle`: Enum (MONTHLY, YEARLY)

**Usage in Rate Limiting**:
1. User authenticates → JWT contains `tenantId`
2. Load `Subscription` for tenant → get `BillingPlan`
3. Map `plan.name` to `PlanRateLimit` enum → create `Bucket`

**No Schema Changes Required**.

---

### 4.2 Subscription Entity

**Relevance**: Links users to billing plans (determines rate limits).

**Key Fields**:
- `user`: User (tenant)
- `plan`: BillingPlan
- `state`: SubscriptionState (ACTIVE, CANCELED, etc.)

**Usage**: Already loaded in `CustomUserDetails` via `TenantContext`.

**No Schema Changes Required**.

---

### 4.3 CustomUserDetails (Security)

**Relevance**: Contains tenant context for rate limiting.

**Key Fields**:
- `tenantId`: UUID (used as rate limit bucket key)
- `subscription`: Subscription (contains plan information)

**Usage**: Retrieved from `SecurityContextHolder` in `RateLimitingFilter`.

**No Changes Required**.

---

## 5. Configuration Classes

### 5.1 CacheConfig (Enhanced)

**Changes**:
- Update `plans` cache TTL from 10 minutes to 1 hour
- Add `sync = true` to prevent cache stampede

**Before**:
```java
.expireAfterWrite(10, TimeUnit.MINUTES)
```

**After**:
```java
.expireAfterWrite(1, TimeUnit.HOURS)
```

---

### 5.2 RateLimitConfig (New)

**Purpose**: Configure Bucket4j rate limiting.

**Structure**:
```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public ProxyManager<String> buckets() {
        return Bucket4j.builder()
            .build(Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build());
    }
}
```

---

## 6. Database Schema Impact

**Summary**: **NO DATABASE CHANGES REQUIRED**

All new functionality uses:
- **In-memory structures** (Bucket4j buckets in Caffeine)
- **Existing entities** (BillingPlan, Subscription, User)
- **Response DTOs** (ErrorResponse, FieldError)
- **Configuration classes** (CacheConfig, RateLimitConfig)

**Rationale**:
- Rate limiting is ephemeral (no need to persist token counts)
- Caching is performance optimization (no new data)
- Exception handling is response formatting (no persistence)

---

## 7. Data Flow Diagrams

### 7.1 Rate Limiting Flow

```
Request → RateLimitingFilter
           ↓
       Extract tenantId from JWT
           ↓
       Load Subscription → Get BillingPlan
           ↓
       Map plan.name → PlanRateLimit enum
           ↓
       Get/Create Bucket from Caffeine cache
           ↓
       Try consume 1 token
           ↓
    ┌──────┴──────┐
    ↓             ↓
 Success       Failure
    ↓             ↓
Add headers   Return 429
    ↓         + Retry-After
Continue
```

### 7.2 Cache Flow

```
GET /api/v1/plans
    ↓
PlanController.getAllPlans()
    ↓
PlanService.getAllActivePlans()
    ↓
@Cacheable("plans") check
    ↓
┌───┴───┐
↓       ↓
Hit    Miss
↓       ↓
Return  Query DB
Cache   ↓
        Store in cache
        ↓
        Return
```

### 7.3 Exception Handling Flow

```
Exception thrown
    ↓
GlobalExceptionHandler catches
    ↓
Determine exception type
    ↓
Map to HTTP status + error code
    ↓
Generate ErrorResponse DTO
    ↓
Log (WARN/ERROR level)
    ↓
Return ResponseEntity<ErrorResponse>
```

---

## 8. Memory Footprint Estimates

| Component | Per-Item Size | Max Items | Total Memory |
|-----------|---------------|-----------|--------------|
| Rate Limit Buckets | ~200 bytes | 10,000 users | ~2 MB |
| Plans Cache | ~1 KB | 1,000 plans | ~1 MB |
| Error Responses | ~500 bytes | N/A (transient) | Negligible |
| **Total** | | | **~3 MB** |

**Conclusion**: Minimal memory impact (<0.1% of typical JVM heap).

---

## 9. Relationships

```
User (1) ──→ (1) Subscription ──→ (1) BillingPlan
  ↓                                      ↓
tenantId                              plan.name
  ↓                                      ↓
RateLimitBucket                    PlanRateLimit enum
(Caffeine cache)                   (capacity, refillPeriod)
```

---

## 10. Summary

- **No new database tables** required
- **2 new Java classes**: `RateLimitingFilter`, `RateLimitExceededException`
- **1 enhanced DTO**: `ErrorResponse` (add 4 fields)
- **1 new configuration**: `RateLimitConfig`
- **1 updated configuration**: `CacheConfig` (TTL change)
- **Total memory footprint**: ~3 MB for 10,000 users

All changes are **non-breaking** and **backward-compatible**.
