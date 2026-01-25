# Data Model: Product & Subscription Engine

**Feature**: Product & Subscription Engine  
**Branch**: `002-product-subscription-engine`  
**Date**: 2026-01-25  
**Purpose**: Define entities, relationships, and database schema for billing plans and subscriptions

## Entity Relationship Diagram

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│   BillingPlan   │         │   Subscription   │         │      User       │
├─────────────────┤         ├──────────────────┤         ├─────────────────┤
│ id (PK)         │◄────────│ planId (FK)      │         │ id (PK)         │
│ name (UNIQUE)   │         │ userId (FK)      │────────►│ email           │
│ description     │         │ tenantId (FK)    │         │ role            │
│ price           │         │ status           │         │ tenantId (FK)   │
│ billingCycle    │         │ startDate        │         └─────────────────┘
│ deleted         │         │ nextBillingDate  │                 │
│ deletedAt       │         │ canceledAt       │                 │
│ createdDate     │         │ version          │                 │
│ lastModifiedDate│         │ createdDate      │                 │
└─────────────────┘         │ lastModifiedDate │                 │
        │                   └──────────────────┘                 │
        │                            │                            │
        │                            │                            │
        ▼                            ▼                            ▼
┌─────────────────┐         ┌──────────────────────────┐  ┌─────────────────┐
│  FeatureLimit   │         │ SubscriptionTransitionLog│  │     Tenant      │
├─────────────────┤         ├──────────────────────────┤  ├─────────────────┤
│ id (PK)         │         │ id (PK)                  │  │ id (PK)         │
│ planId (FK)     │         │ subscriptionId (FK)      │  │ name            │
│ limitType       │         │ fromState                │  │ createdDate     │
│ limitValue      │         │ toState                  │  └─────────────────┘
└─────────────────┘         │ reason                   │
                            │ transitionDate           │
                            └──────────────────────────┘

Relationships:
- BillingPlan (1) ──< (N) FeatureLimit
- BillingPlan (1) ──< (N) Subscription
- User (1) ──< (N) Subscription
- Tenant (1) ──< (N) Subscription
- Subscription (1) ──< (N) SubscriptionTransitionLog
```

## Core Entities

### 1. BillingPlan (Aggregate Root)

**Purpose**: Represents a subscription tier (Free, Pro, Enterprise) with pricing and feature limits.

**Attributes**:

| Field              | Type                  | Constraints                     | Description                           |
| ------------------ | --------------------- | ------------------------------- | ------------------------------------- |
| `id`               | `Long`                | PK, AUTO_INCREMENT              | Unique identifier                     |
| `name`             | `String`              | NOT NULL, UNIQUE, max 100 chars | Plan name (e.g., "Pro", "Enterprise") |
| `description`      | `String`              | max 500 chars                   | Plan description for marketing        |
| `price`            | `BigDecimal`          | NOT NULL, precision(10,2), >= 0 | Monthly or yearly price in USD        |
| `billingCycle`     | `BillingCycle` (enum) | NOT NULL                        | MONTHLY or YEARLY                     |
| `deleted`          | `Boolean`             | NOT NULL, default false         | Soft delete flag                      |
| `deletedAt`        | `LocalDateTime`       | nullable                        | Timestamp of soft deletion            |
| `createdDate`      | `LocalDateTime`       | NOT NULL, auto-set              | Creation timestamp                    |
| `lastModifiedDate` | `LocalDateTime`       | NOT NULL, auto-update           | Last update timestamp                 |

**Relationships**:

- **One-to-Many** with `FeatureLimit`: A plan has multiple feature limits
- **One-to-Many** with `Subscription`: A plan can have multiple subscriptions

**Business Rules**:

- Plan name must be unique across all plans (including soft-deleted)
- Price must be non-negative
- Cannot delete plan if active subscriptions exist
- Soft delete preserves historical data

**JPA Entity**:

```java
@Entity
@Table(name = "billing_plan")
public class BillingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingCycle billingCycle;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureLimit> featureLimits = new ArrayList<>();

    @Column(nullable = false)
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    // Business methods
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return !deleted;
    }
}
```

---

### 2. FeatureLimit (Value Object / Embedded Entity)

**Purpose**: Represents usage or access limits for a billing plan (e.g., API calls per month, max users).

**Attributes**:

| Field        | Type      | Constraints                 | Description                                    |
| ------------ | --------- | --------------------------- | ---------------------------------------------- |
| `id`         | `Long`    | PK, AUTO_INCREMENT          | Unique identifier                              |
| `planId`     | `Long`    | FK to BillingPlan, NOT NULL | Associated plan                                |
| `limitType`  | `String`  | NOT NULL, max 50 chars      | Type of limit (e.g., "api_calls", "max_users") |
| `limitValue` | `Integer` | NOT NULL, >= 0              | Numeric limit value                            |

**Relationships**:

- **Many-to-One** with `BillingPlan`: Each limit belongs to one plan

**Business Rules**:

- Limit value must be non-negative
- Limit type should be descriptive (snake_case convention)
- Limits are deleted when plan is deleted (cascade)

**JPA Entity**:

```java
@Entity
@Table(name = "feature_limit")
public class FeatureLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private BillingPlan plan;

    @Column(nullable = false, length = 50)
    private String limitType;

    @Column(nullable = false)
    private Integer limitValue;
}
```

---

### 3. Subscription (Aggregate Root)

**Purpose**: Represents a customer's subscription to a billing plan with lifecycle state.

**Attributes**:

| Field              | Type                       | Constraints                 | Description                          |
| ------------------ | -------------------------- | --------------------------- | ------------------------------------ |
| `id`               | `Long`                     | PK, AUTO_INCREMENT          | Unique identifier                    |
| `userId`           | `Long`                     | FK to User, NOT NULL        | Subscriber (customer)                |
| `planId`           | `Long`                     | FK to BillingPlan, NOT NULL | Subscribed plan                      |
| `tenantId`         | `Long`                     | FK to Tenant, NOT NULL      | Tenant for multi-tenancy             |
| `status`           | `SubscriptionState` (enum) | NOT NULL                    | ACTIVE, PAST_DUE, or CANCELED        |
| `startDate`        | `LocalDate`                | NOT NULL                    | Subscription start date              |
| `nextBillingDate`  | `LocalDate`                | nullable                    | Next billing date (null if canceled) |
| `canceledAt`       | `LocalDateTime`            | nullable                    | Cancellation timestamp               |
| `version`          | `Long`                     | NOT NULL, auto-increment    | Optimistic locking version           |
| `createdDate`      | `LocalDateTime`            | NOT NULL, auto-set          | Creation timestamp                   |
| `lastModifiedDate` | `LocalDateTime`            | NOT NULL, auto-update       | Last update timestamp                |

**Relationships**:

- **Many-to-One** with `User`: Each subscription belongs to one user
- **Many-to-One** with `BillingPlan`: Each subscription references one plan
- **Many-to-One** with `Tenant`: Each subscription belongs to one tenant
- **One-to-Many** with `SubscriptionTransitionLog`: A subscription has multiple state transition logs

**Business Rules**:

- User can have only one active subscription per tenant
- Next billing date is calculated based on plan's billing cycle
- Canceled subscriptions have null `nextBillingDate`
- State transitions must follow valid state machine rules
- Optimistic locking prevents concurrent modification conflicts

**JPA Entity**:

```java
@Entity
@Table(name = "subscription",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tenant_id", "status"}))
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private BillingPlan plan;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionState status;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate nextBillingDate;

    private LocalDateTime canceledAt;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    // Business methods
    public void calculateNextBillingDate() {
        if (this.status == SubscriptionState.CANCELED) {
            this.nextBillingDate = null;
            return;
        }

        LocalDate baseDate = this.startDate != null ? this.startDate : LocalDate.now();
        this.nextBillingDate = switch (this.plan.getBillingCycle()) {
            case MONTHLY -> baseDate.plusMonths(1);
            case YEARLY -> baseDate.plusYears(1);
        };
    }

    public void cancel() {
        this.status = SubscriptionState.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.nextBillingDate = null;
    }

    public void upgradeToPlan(BillingPlan newPlan) {
        this.plan = newPlan;
        this.nextBillingDate = switch (newPlan.getBillingCycle()) {
            case MONTHLY -> LocalDate.now().plusMonths(1);
            case YEARLY -> LocalDate.now().plusYears(1);
        };
    }
}
```

---

### 4. SubscriptionTransitionLog (Audit Entity)

**Purpose**: Audit log for all subscription state transitions (for compliance and debugging).

**Attributes**:

| Field            | Type                       | Constraints                  | Description             |
| ---------------- | -------------------------- | ---------------------------- | ----------------------- |
| `id`             | `Long`                     | PK, AUTO_INCREMENT           | Unique identifier       |
| `subscriptionId` | `Long`                     | FK to Subscription, NOT NULL | Associated subscription |
| `fromState`      | `SubscriptionState` (enum) | NOT NULL                     | Previous state          |
| `toState`        | `SubscriptionState` (enum) | NOT NULL                     | New state               |
| `reason`         | `String`                   | max 255 chars                | Reason for transition   |
| `transitionDate` | `LocalDateTime`            | NOT NULL, auto-set           | Timestamp of transition |

**Relationships**:

- **Many-to-One** with `Subscription`: Each log entry belongs to one subscription

**Business Rules**:

- Immutable - never updated or deleted
- Automatically created on every state transition
- Used for audit trails and compliance

**JPA Entity**:

```java
@Entity
@Table(name = "subscription_transition_log")
public class SubscriptionTransitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionState fromState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionState toState;

    @Column(length = 255)
    private String reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime transitionDate;
}
```

---

## Enumerations

### BillingCycle

**Purpose**: Represents billing frequency for a plan.

```java
public enum BillingCycle {
    MONTHLY,  // Bill every 30 days
    YEARLY    // Bill every 365 days
}
```

### SubscriptionState

**Purpose**: Represents the current status of a subscription.

```java
public enum SubscriptionState {
    ACTIVE,      // Subscription is active and paid
    PAST_DUE,    // Payment failed, in grace period
    CANCELED     // Subscription terminated
}
```

**State Transition Rules**:

- `ACTIVE` → `PAST_DUE`: Payment failure
- `ACTIVE` → `CANCELED`: User cancellation
- `PAST_DUE` → `ACTIVE`: Successful payment
- `PAST_DUE` → `CANCELED`: Grace period expired
- `CANCELED` → (none): Terminal state

---

## Database Schema (PostgreSQL)

### Migration V003: Create billing_plan table

```sql
CREATE TABLE billing_plan (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    billing_cycle VARCHAR(20) NOT NULL CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_billing_plan_deleted ON billing_plan(deleted);
```

### Migration V004: Create feature_limit table

```sql
CREATE TABLE feature_limit (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES billing_plan(id) ON DELETE CASCADE,
    limit_type VARCHAR(50) NOT NULL,
    limit_value INTEGER NOT NULL CHECK (limit_value >= 0)
);

CREATE INDEX idx_feature_limit_plan_id ON feature_limit(plan_id);
```

### Migration V005: Create subscription table

```sql
CREATE TABLE subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    plan_id BIGINT NOT NULL REFERENCES billing_plan(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELED')),
    start_date DATE NOT NULL,
    next_billing_date DATE,
    canceled_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_active_subscription UNIQUE (user_id, tenant_id, status)
);

CREATE INDEX idx_subscription_user_id ON subscription(user_id);
CREATE INDEX idx_subscription_tenant_id ON subscription(tenant_id);
CREATE INDEX idx_subscription_status ON subscription(status);
CREATE INDEX idx_subscription_tenant_status ON subscription(tenant_id, status);
```

### Migration V006: Create subscription_transition_log table

```sql
CREATE TABLE subscription_transition_log (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    from_state VARCHAR(20) NOT NULL,
    to_state VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    transition_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transition_log_subscription_id ON subscription_transition_log(subscription_id);
CREATE INDEX idx_transition_log_transition_date ON subscription_transition_log(transition_date);
```

---

## Validation Rules

### BillingPlan Validation

```java
@NotBlank(message = "Plan name is required")
@Size(max = 100, message = "Plan name must not exceed 100 characters")
private String name;

@Size(max = 500, message = "Description must not exceed 500 characters")
private String description;

@NotNull(message = "Price is required")
@DecimalMin(value = "0.00", message = "Price must be non-negative")
@Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
private BigDecimal price;

@NotNull(message = "Billing cycle is required")
private BillingCycle billingCycle;
```

### Subscription Validation

```java
@NotNull(message = "User ID is required")
private Long userId;

@NotNull(message = "Plan ID is required")
private Long planId;

@NotNull(message = "Tenant ID is required")
private Long tenantId;

@NotNull(message = "Status is required")
private SubscriptionState status;

@NotNull(message = "Start date is required")
@PastOrPresent(message = "Start date cannot be in the future")
private LocalDate startDate;
```

---

## Indexes for Performance

| Table                         | Index                                | Columns             | Purpose              |
| ----------------------------- | ------------------------------------ | ------------------- | -------------------- |
| `billing_plan`                | `idx_billing_plan_deleted`           | `deleted`           | Filter active plans  |
| `feature_limit`               | `idx_feature_limit_plan_id`          | `plan_id`           | Join with plans      |
| `subscription`                | `idx_subscription_user_id`           | `user_id`           | User's subscriptions |
| `subscription`                | `idx_subscription_tenant_id`         | `tenant_id`         | Tenant filtering     |
| `subscription`                | `idx_subscription_status`            | `status`            | Analytics queries    |
| `subscription`                | `idx_subscription_tenant_status`     | `tenant_id, status` | Common query pattern |
| `subscription_transition_log` | `idx_transition_log_subscription_id` | `subscription_id`   | Audit trail lookup   |

---

## Data Integrity Constraints

1. **Referential Integrity**:
   - `FeatureLimit.planId` → `BillingPlan.id` (CASCADE DELETE)
   - `Subscription.planId` → `BillingPlan.id` (RESTRICT DELETE)
   - `Subscription.userId` → `User.id` (RESTRICT DELETE)
   - `Subscription.tenantId` → `Tenant.id` (RESTRICT DELETE)
   - `SubscriptionTransitionLog.subscriptionId` → `Subscription.id` (RESTRICT DELETE)

2. **Unique Constraints**:
   - `BillingPlan.name` must be unique
   - `(Subscription.userId, Subscription.tenantId, Subscription.status)` must be unique when status = 'ACTIVE'

3. **Check Constraints**:
   - `BillingPlan.price >= 0`
   - `FeatureLimit.limitValue >= 0`
   - `BillingPlan.billingCycle IN ('MONTHLY', 'YEARLY')`
   - `Subscription.status IN ('ACTIVE', 'PAST_DUE', 'CANCELED')`

4. **Optimistic Locking**:
   - `Subscription.version` automatically incremented on each update
   - Prevents lost updates from concurrent modifications

---

## Sample Data

### Billing Plans

```sql
INSERT INTO billing_plan (name, description, price, billing_cycle) VALUES
('Free', 'Basic features for individuals', 0.00, 'MONTHLY'),
('Pro', 'Advanced features for professionals', 29.99, 'MONTHLY'),
('Enterprise', 'Full features for organizations', 299.99, 'YEARLY');
```

### Feature Limits

```sql
INSERT INTO feature_limit (plan_id, limit_type, limit_value) VALUES
(1, 'api_calls', 100),
(1, 'max_users', 1),
(2, 'api_calls', 10000),
(2, 'max_users', 10),
(3, 'api_calls', 1000000),
(3, 'max_users', 100);
```

---

## Next Steps

1. ✅ Data model complete - all entities and relationships defined
2. → Generate API contracts in `/contracts/` directory
3. → Create `quickstart.md` for local development setup
4. → Implement JPA entities and repositories
5. → Write migration scripts for database schema
