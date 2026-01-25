# Technical Research: Product & Subscription Engine

**Feature**: Product & Subscription Engine  
**Branch**: `002-product-subscription-engine`  
**Date**: 2026-01-25  
**Purpose**: Resolve technical unknowns and validate technology choices for implementation

## Research Tasks

### 1. Money Pattern Implementation in Java/Spring Boot

**Question**: How should we represent monetary values (plan prices) to avoid floating-point precision issues?

**Research Findings**:

**Decision**: Use `BigDecimal` with explicit scale and rounding mode

**Rationale**:

- `BigDecimal` is the Java standard for precise decimal arithmetic, essential for financial calculations
- Avoids floating-point precision errors (e.g., 0.1 + 0.2 != 0.3 in double)
- Spring Data JPA natively supports `BigDecimal` mapping to PostgreSQL `NUMERIC` type
- Allows explicit control over scale (2 decimal places for currency) and rounding mode (HALF_UP for standard rounding)

**Implementation Details**:

```java
@Entity
public class BillingPlan {
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;  // e.g., 29.99

    // Always use BigDecimal arithmetic
    public BigDecimal calculateAnnualPrice() {
        return price.multiply(new BigDecimal("12"));
    }
}
```

**Validation**:

- Use `@DecimalMin("0.00")` for non-negative price validation
- Use `@Digits(integer=8, fraction=2)` to enforce precision
- PostgreSQL `NUMERIC(10,2)` column type for storage

**Alternatives Considered**:

- **Joda-Money / JavaMoney (JSR 354)**: More feature-rich with currency support, but adds dependency overhead. Overkill for single-currency (USD) system. Can migrate later if multi-currency needed.
- **Integer cents**: Store prices as cents (2999 instead of 29.99). Simpler but less readable and requires conversion logic everywhere. Not idiomatic for Spring/JPA.
- **Double/Float**: Unacceptable for financial data due to precision errors.

---

### 2. Subscription State Machine Design

**Question**: How should we implement and validate subscription state transitions (Active → Past Due → Canceled)?

**Research Findings**:

**Decision**: Implement explicit state machine with transition validation in `SubscriptionStateMachine` service

**Rationale**:

- State machines prevent invalid transitions and encode business rules explicitly
- Centralized transition logic ensures consistency across all subscription operations
- Easier to test, audit, and extend with new states (e.g., SUSPENDED, TRIAL)
- Aligns with DDD principle of encoding business rules in domain logic

**Implementation Pattern**:

```java
@Service
public class SubscriptionStateMachine {

    private static final Map<SubscriptionState, Set<SubscriptionState>> ALLOWED_TRANSITIONS = Map.of(
        SubscriptionState.ACTIVE, Set.of(SubscriptionState.PAST_DUE, SubscriptionState.CANCELED),
        SubscriptionState.PAST_DUE, Set.of(SubscriptionState.ACTIVE, SubscriptionState.CANCELED),
        SubscriptionState.CANCELED, Set.of() // Terminal state
    );

    public void validateTransition(SubscriptionState from, SubscriptionState to) {
        if (!ALLOWED_TRANSITIONS.get(from).contains(to)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition from %s to %s", from, to)
            );
        }
    }

    public void transitionTo(Subscription subscription, SubscriptionState newState, String reason) {
        validateTransition(subscription.getStatus(), newState);
        SubscriptionState oldState = subscription.getStatus();
        subscription.setStatus(newState);
        logTransition(subscription, oldState, newState, reason);
    }
}
```

**Transition Rules**:

- **ACTIVE → PAST_DUE**: Payment failure
- **ACTIVE → CANCELED**: User cancellation or admin action
- **PAST_DUE → ACTIVE**: Successful payment after failure
- **PAST_DUE → CANCELED**: Grace period expired (7 days)
- **CANCELED → (none)**: Terminal state, requires new subscription

**Alternatives Considered**:

- **Spring State Machine**: Full-featured state machine framework. Too heavyweight for simple 3-state machine. Adds complexity and learning curve.
- **Enum with transition methods**: Encode transitions in enum itself. Violates single responsibility (enum should represent state, not behavior).
- **No validation**: Allow any state change. Dangerous - can lead to invalid states and data corruption.

---

### 3. Caching Strategy for Billing Plans

**Question**: Should we use Redis or Caffeine for caching billing plans, and what should be the cache eviction policy?

**Research Findings**:

**Decision**: Use **Caffeine** for local in-memory caching with TTL-based eviction

**Rationale**:

- Billing plans are read-heavy (every subscription operation needs plan details) but rarely change
- Caffeine is faster than Redis for local caching (no network overhead)
- Simpler deployment (no separate Redis instance needed for Phase 2)
- Spring Boot has excellent Caffeine integration via `spring-boot-starter-cache`
- TTL-based eviction ensures eventual consistency (plans updated within 5 minutes)

**Configuration**:

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=5m
    cache-names:
      - plans
```

**Cache Usage**:

```java
@Service
public class PlanService {

    @Cacheable(value = "plans", key = "#id")
    public PlanResponse getPlanById(Long id) {
        // Cache hit: return from memory
        // Cache miss: query DB and cache result
    }

    @Cacheable(value = "plans", key = "'all'")
    public List<PlanResponse> getAllPlans() {
        // Cache all plans list
    }

    @CacheEvict(value = "plans", allEntries = true)
    public PlanResponse updatePlan(Long id, UpdatePlanRequest request) {
        // Evict entire cache on any plan update
    }
}
```

**Eviction Policy**:

- **Write-through**: Evict entire cache on plan create/update/delete
- **TTL**: 5-minute expiration for eventual consistency
- **Size limit**: 500 plans max (well above expected 100 plans)

**Alternatives Considered**:

- **Redis**: Better for distributed caching across multiple instances. Overkill for Phase 2 (single instance). Can migrate to Redis in Phase 4 for horizontal scaling.
- **No caching**: Simpler but poor performance. Every subscription operation would hit DB for plan details.
- **Database query cache**: PostgreSQL query cache. Less control and harder to invalidate programmatically.

---

### 4. Optimistic Locking for Concurrent Subscription Updates

**Question**: How do we prevent race conditions when multiple operations modify the same subscription simultaneously?

**Research Findings**:

**Decision**: Use JPA `@Version` for optimistic locking on `Subscription` entity

**Rationale**:

- Prevents lost updates when two users/processes try to modify the same subscription concurrently
- Optimistic locking is appropriate for subscription operations (conflicts are rare)
- JPA/Hibernate provides built-in support with minimal code
- Better performance than pessimistic locking (no database locks held)

**Implementation**:

```java
@Entity
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;  // Automatically incremented on each update

    // Other fields...
}
```

**Conflict Handling**:

```java
@Service
public class SubscriptionService {

    @Transactional
    public SubscriptionResponse upgradeSubscription(Long subscriptionId, UpgradeRequest request) {
        try {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

            // Modify subscription
            subscription.setPlan(newPlan);
            subscription.setVersion(subscription.getVersion()); // JPA auto-increments

            return subscriptionRepository.save(subscription);
        } catch (OptimisticLockException e) {
            throw new ConcurrentModificationException(
                "Subscription was modified by another process. Please retry."
            );
        }
    }
}
```

**Alternatives Considered**:

- **Pessimistic locking**: Use `SELECT FOR UPDATE` to lock rows. Higher overhead, can cause deadlocks. Only needed for high-contention scenarios.
- **No locking**: Risk of lost updates. Unacceptable for financial data.
- **Application-level locking**: Use distributed locks (Redis). Too complex for Phase 2.

---

### 5. Billing Date Calculation Logic

**Question**: How should we calculate next billing dates for MONTHLY vs YEARLY subscriptions?

**Research Findings**:

**Decision**: Use `java.time.LocalDate` with `plusMonths()` / `plusYears()` for date arithmetic

**Rationale**:

- `java.time` API (Java 8+) handles date arithmetic correctly (leap years, month-end edge cases)
- `LocalDate` is timezone-agnostic (billing dates are calendar dates, not timestamps)
- Spring Data JPA maps `LocalDate` to PostgreSQL `DATE` type
- Handles edge cases automatically (e.g., Jan 31 + 1 month = Feb 28/29)

**Implementation**:

```java
public class Subscription {

    public void calculateNextBillingDate() {
        LocalDate startDate = this.startDate != null ? this.startDate : LocalDate.now();

        this.nextBillingDate = switch (this.plan.getBillingCycle()) {
            case MONTHLY -> startDate.plusMonths(1);
            case YEARLY -> startDate.plusYears(1);
        };
    }

    public void recalculateNextBillingDateOnUpgrade(BillingPlan newPlan) {
        // On upgrade, calculate from current date with new cycle
        this.nextBillingDate = switch (newPlan.getBillingCycle()) {
            case MONTHLY -> LocalDate.now().plusMonths(1);
            case YEARLY -> LocalDate.now().plusYears(1);
        };
    }
}
```

**Edge Cases Handled**:

- **Month-end dates**: Jan 31 + 1 month = Feb 28 (or Feb 29 in leap year) - handled automatically
- **Leap years**: Feb 29 + 1 year = Feb 28 (next non-leap year) - handled automatically
- **Timezone independence**: Billing dates are calendar dates, not affected by timezone changes

**Alternatives Considered**:

- **Manual date arithmetic**: Error-prone, doesn't handle edge cases.
- **Joda-Time**: Legacy library, superseded by `java.time`.
- **Store billing cycle duration in days**: Inflexible, doesn't handle month-end correctly.

---

### 6. Subscription Analytics Query Optimization

**Question**: How should we efficiently calculate subscription metrics (active count, churn rate) for 10,000+ subscriptions?

**Research Findings**:

**Decision**: Use native SQL queries with database aggregation for analytics

**Rationale**:

- Database aggregation (COUNT, SUM, GROUP BY) is much faster than loading all records into memory
- PostgreSQL query optimizer handles large datasets efficiently
- Reduces memory footprint (only aggregated results returned, not all records)
- Can leverage database indexes for performance

**Implementation**:

```java
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Count active subscriptions by plan
    @Query("SELECT s.plan.id, COUNT(s) FROM Subscription s " +
           "WHERE s.status = 'ACTIVE' AND s.tenantId = :tenantId " +
           "GROUP BY s.plan.id")
    List<Object[]> countActiveSubscriptionsByPlan(@Param("tenantId") Long tenantId);

    // Calculate churn rate for a period
    @Query(value = "SELECT " +
           "  COUNT(CASE WHEN status = 'CANCELED' THEN 1 END) * 100.0 / COUNT(*) AS churn_rate " +
           "FROM subscription " +
           "WHERE tenant_id = :tenantId " +
           "  AND created_date BETWEEN :startDate AND :endDate",
           nativeQuery = true)
    Double calculateChurnRate(@Param("tenantId") Long tenantId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);
}
```

**Performance Optimizations**:

- **Indexes**: Composite index on `(tenant_id, status)` for filtering
- **Pagination**: Use `Pageable` for large result sets
- **Caching**: Cache analytics results with short TTL (1 minute) for dashboards

**Alternatives Considered**:

- **Load all subscriptions and calculate in Java**: Simple but doesn't scale. 10,000 subscriptions = high memory usage and slow.
- **Materialized views**: Pre-computed analytics. Complex to maintain and refresh. Overkill for Phase 2.
- **Separate analytics database**: Better for very large scale. Not needed for 10,000 subscriptions.

---

### 7. Plan Deletion Constraints

**Question**: How do we prevent deletion of billing plans that have active subscriptions?

**Research Findings**:

**Decision**: Implement soft delete with business logic validation

**Rationale**:

- Hard delete would break referential integrity (orphaned subscriptions)
- Soft delete preserves historical data for canceled subscriptions
- Business logic check prevents deletion of plans with active subscriptions
- Allows "archiving" of old plans while maintaining data integrity

**Implementation**:

```java
@Entity
public class BillingPlan {
    @Column(nullable = false)
    private Boolean deleted = false;

    private LocalDateTime deletedAt;
}

@Service
public class PlanService {

    @Transactional
    public void deletePlan(Long planId) {
        BillingPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new PlanNotFoundException(planId));

        // Check for active subscriptions
        long activeCount = subscriptionRepository.countByPlanIdAndStatus(
            planId, SubscriptionState.ACTIVE
        );

        if (activeCount > 0) {
            throw new PlanHasActiveSubscriptionsException(
                String.format("Cannot delete plan %s: %d active subscriptions exist",
                              plan.getName(), activeCount)
            );
        }

        // Soft delete
        plan.setDeleted(true);
        plan.setDeletedAt(LocalDateTime.now());
        planRepository.save(plan);
    }
}
```

**Query Filtering**:

```java
@Repository
public interface PlanRepository extends JpaRepository<BillingPlan, Long> {

    // Exclude deleted plans from normal queries
    @Query("SELECT p FROM BillingPlan p WHERE p.deleted = false")
    List<BillingPlan> findAllActive();

    @Query("SELECT p FROM BillingPlan p WHERE p.id = :id AND p.deleted = false")
    Optional<BillingPlan> findByIdActive(@Param("id") Long id);
}
```

**Alternatives Considered**:

- **Hard delete with FK constraint**: Would prevent deletion if subscriptions exist, but loses historical data.
- **Cascade delete subscriptions**: Dangerous - would delete customer subscription data.
- **No deletion**: Keep all plans forever. Clutters plan list, no way to retire old plans.

---

## Summary of Technology Choices

| Component                | Technology                                | Rationale                                     |
| ------------------------ | ----------------------------------------- | --------------------------------------------- |
| **Money Representation** | `BigDecimal` with scale=2                 | Precise decimal arithmetic for financial data |
| **State Machine**        | Custom `SubscriptionStateMachine` service | Explicit transition validation, easy to test  |
| **Caching**              | Caffeine (local in-memory)                | Fast, simple, sufficient for Phase 2          |
| **Concurrency Control**  | JPA `@Version` (optimistic locking)       | Prevents lost updates, good performance       |
| **Date Arithmetic**      | `java.time.LocalDate`                     | Handles edge cases, timezone-agnostic         |
| **Analytics Queries**    | Native SQL with DB aggregation            | Efficient for large datasets                  |
| **Plan Deletion**        | Soft delete with validation               | Preserves data integrity and history          |

## Implementation Risks & Mitigations

| Risk                                | Impact                           | Mitigation                                  |
| ----------------------------------- | -------------------------------- | ------------------------------------------- |
| **Concurrent subscription updates** | Lost updates, data corruption    | Use `@Version` for optimistic locking       |
| **Cache staleness**                 | Users see outdated plan prices   | 5-minute TTL + evict on update              |
| **Invalid state transitions**       | Subscriptions in illegal states  | State machine with validation               |
| **Floating-point precision errors** | Incorrect billing amounts        | Use `BigDecimal` for all money calculations |
| **Orphaned subscriptions**          | Referential integrity violations | Soft delete plans + FK constraints          |
| **Slow analytics queries**          | Poor dashboard performance       | Database indexes + query optimization       |

## Next Steps

1. ✅ Research complete - all technical unknowns resolved
2. → Proceed to Phase 1: Create `data-model.md` with entity definitions
3. → Generate API contracts in `/contracts/` directory
4. → Create `quickstart.md` for local development setup
5. → Update agent context with new technologies
