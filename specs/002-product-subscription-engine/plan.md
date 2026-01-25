# Implementation Plan: Product & Subscription Engine

**Branch**: `002-product-subscription-engine` | **Date**: 2026-01-25 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/002-product-subscription-engine/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command.

## Summary

This feature implements the core domain of the SaaS billing platform: Product & Subscription Management. It provides comprehensive plan management capabilities (CRUD operations for Free, Pro, Enterprise tiers) and complete subscription lifecycle management (subscribe, upgrade, cancel) with automated state transitions (Active, Past Due, Canceled). The system enforces multi-tenant data isolation, maintains referential integrity, and provides analytics capabilities for subscription metrics and churn analysis.

## Technical Context

**Language/Version**: Java 17+  
**Primary Dependencies**: Spring Boot 3.x, Spring Data JPA, Hibernate Validator, PostgreSQL Driver  
**Storage**: PostgreSQL (ACID compliance for financial data, referential integrity for subscriptions)  
**Testing**: JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers (PostgreSQL)  
**Target Platform**: JVM-based server (Linux/Windows)  
**Project Type**: Single backend application (REST API)  
**Performance Goals**:

- Plan retrieval (cached) < 500ms for 1000+ plans
- Subscription operations < 3 seconds under normal load
- Support 1,000+ concurrent subscription operations
- Analytics queries < 3 seconds for 10,000+ subscriptions

**Constraints**:

- Multi-tenant data isolation must be enforced at data access layer (inherited from Phase 1)
- All subscription state transitions must be logged for audit trail
- Referential integrity must prevent orphaned subscriptions
- All endpoints must be versioned (/api/v1/)
- Subscription operations must be transactional (ACID guarantees)

**Scale/Scope**:

- Support for 10,000+ subscriptions across multiple tenants
- Support for 100+ billing plans
- Horizontal scalability (stateless design)
- Production-ready financial data handling

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

### ✅ Layered Architecture Compliance

- **Controller Layer**: PlanController, SubscriptionController, AnalyticsController for HTTP endpoints
- **Service Layer**: PlanService, SubscriptionService, AnalyticsService for business logic and state transitions
- **Repository Layer**: PlanRepository, SubscriptionRepository, SubscriptionTransitionLogRepository for data access
- **DTO Layer**: PlanRequest, PlanResponse, SubscriptionRequest, SubscriptionResponse, UpgradeRequest, AnalyticsResponse DTOs
- **Entity Layer**: BillingPlan, Subscription, SubscriptionState (enum), FeatureLimit, SubscriptionTransitionLog entities
- **Exception Layer**: PlanNotFoundException, SubscriptionNotFoundException, InvalidStateTransitionException, DuplicateSubscriptionException, PlanHasActiveSubscriptionsException
- **Config Layer**: CacheConfig (for plan caching), TransactionConfig

**Status**: ✅ COMPLIANT - All layers properly separated

### ✅ Domain-Driven Design Compliance

- **Aggregates**:
  - BillingPlan (aggregate root with feature limits)
  - Subscription (aggregate root with state transitions, billing dates)
- **Value Objects**:
  - Money (BigDecimal-based price representation, USD only per research.md)
  - BillingCycle (enum: MONTHLY, YEARLY)
  - SubscriptionState (enum: ACTIVE, PAST_DUE, CANCELED)
  - FeatureLimit (embedded value object)
- **Domain Events**:
  - SubscriptionCreated, SubscriptionUpgraded, SubscriptionCanceled, SubscriptionStateChanged
- **Repositories**:
  - PlanRepository operates on BillingPlan aggregate
  - SubscriptionRepository operates on Subscription aggregate

**Status**: ✅ COMPLIANT - Rich domain models with explicit business concepts

### ✅ Security-First Compliance

- **Authentication**: Inherited JWT-based authentication from Phase 1 ✅
- **Authorization**:
  - ROLE_ADMIN for plan management (create, update, delete, analytics)
  - ROLE_USER for subscription operations (subscribe, upgrade, cancel, view own subscriptions)
  - ✅
- **Multi-Tenancy**: TenantId filtering for all subscription queries (users see only their subscriptions) ✅
- **Input Validation**: Bean Validation on all DTOs (@Valid, @NotBlank, @NotNull, @Positive, @DecimalMin) ✅
- **Sensitive Data**: No logging of financial details in plain text ✅

**Status**: ✅ COMPLIANT - All security requirements met

### ✅ Transaction Management Compliance

- **Plan Creation/Update**: @Transactional (create plan + feature limits atomically)
- **Subscription Creation**: @Transactional (create subscription + log initial state)
- **Subscription Upgrade**: @Transactional (update subscription + recalculate billing + log transition)
- **Subscription Cancellation**: @Transactional (update status + clear billing date + log transition)
- **State Transitions**: @Transactional with optimistic locking (@Version) to prevent concurrent state changes
- **Plan Deletion**: @Transactional with referential integrity check (prevent deletion if active subscriptions exist)
- **Isolation Level**: SERIALIZABLE for subscription state transitions to prevent race conditions

**Status**: ✅ COMPLIANT - All financial operations properly transactional with appropriate isolation

### ✅ API Design Excellence Compliance

- **Versioning**: /api/v1/plans/_, /api/v1/subscriptions/_, /api/v1/admin/analytics/\* ✅
- **HTTP Methods**:
  - GET for reads (list plans, get subscription)
  - POST for creates (create plan, subscribe)
  - PUT for updates (update plan, upgrade subscription)
  - DELETE for deletes (delete plan, cancel subscription)
  - ✅
- **Status Codes**:
  - 200 (success), 201 (created), 204 (no content for cancel)
  - 400 (validation), 401 (unauthorized), 403 (forbidden), 404 (not found)
  - 409 (conflict for duplicate subscription or plan deletion with active subscriptions)
  - ✅
- **Pagination**: All list endpoints support pagination (Pageable) and sorting ✅
- **DTOs**: No entity exposure, all responses use DTOs ✅
- **Documentation**: Swagger/OpenAPI annotations on all endpoints with examples ✅

**Status**: ✅ COMPLIANT - RESTful design with proper versioning and pagination

### ✅ Test-First Development Compliance

- **TDD Cycle**: Tests written before implementation ✅
- **Unit Tests**:
  - PlanService (CRUD operations, validation, deletion constraints)
  - SubscriptionService (lifecycle operations, state transitions, upgrade logic)
  - AnalyticsService (aggregation calculations, churn rate)
  - All tested with mocked repositories
  - ✅
- **Integration Tests**:
  - All endpoints tested with MockMvc and @SpringBootTest
  - Test multi-tenant isolation
  - Test state transition flows
  - Test referential integrity constraints
  - ✅
- **Repository Tests**:
  - @DataJpaTest for custom queries
  - Test pagination and sorting
  - Test tenant filtering
  - ✅
- **Coverage**: Target 80%+ for service and controller layers ✅

**Status**: ✅ COMPLIANT - Comprehensive test strategy with focus on state transitions

### ✅ Exception Handling & Observability Compliance

- **Global Handler**: @ControllerAdvice handles all exceptions globally ✅
- **Custom Exceptions**:
  - PlanNotFoundException (404)
  - SubscriptionNotFoundException (404)
  - InvalidStateTransitionException (400)
  - DuplicateSubscriptionException (409)
  - PlanHasActiveSubscriptionsException (409)
  - InvalidUpgradeException (400)
  - ✅
- **Error Responses**: Consistent JSON format with error code, message, timestamp, path ✅
- **Logging**:
  - SLF4J with structured logging
  - Log all state transitions at INFO level
  - Log business events (subscription created, upgraded, canceled)
  - Log errors at ERROR level with context
  - ✅
- **Monitoring**: Actuator endpoints + custom metrics for subscription operations ✅

**Status**: ✅ COMPLIANT - Comprehensive error handling with business event logging

### ✅ Performance & Caching Compliance

- **Caching**:
  - @Cacheable on PlanService.getAllPlans() and PlanService.getPlanById()
  - Cache eviction on plan updates/deletes
  - Redis or Caffeine for caching layer
  - ✅
- **N+1 Prevention**:
  - Use @EntityGraph for Subscription + BillingPlan queries
  - JOIN FETCH for loading subscriptions with plan details
  - ✅
- **Lazy Loading**:
  - BillingPlan ↔ FeatureLimits: EAGER (small collection)
  - Subscription → BillingPlan: LAZY with explicit fetch when needed
  - ✅
- **Database Indexing**:
  - Unique index on BillingPlan.name
  - Index on Subscription.userId (tenant filtering)
  - Index on Subscription.tenantId (multi-tenant queries)
  - Index on Subscription.status (analytics queries)
  - Composite index on (tenantId, status) for common queries
  - ✅
- **Connection Pooling**: HikariCP with appropriate pool sizes (inherited from Phase 1) ✅
- **Rate Limiting**: Deferred to Phase 4 (per project plan) ✅

**Status**: ✅ COMPLIANT - Caching strategy for read-heavy plan operations, optimized queries

### ✅ Data Integrity & Validation Compliance

- **Database Constraints**:
  - UNIQUE(name) on BillingPlan
  - NOT NULL on required fields (price, billingCycle, status)
  - FOREIGN KEY constraints (Subscription → BillingPlan, Subscription → User)
  - CHECK constraint (price >= 0)
  - ✅
- **JPA Validation**:
  - @Column(nullable=false), @NotNull on entities
  - @Enumerated for BillingCycle and SubscriptionState
  - @Version for optimistic locking on Subscription
  - ✅
- **DTO Validation**:
  - @Valid, @NotBlank, @NotNull on DTOs
  - @Positive, @DecimalMin for price validation
  - @Pattern for plan name validation
  - Custom validators for state transition rules
  - ✅
- **Business Rules**:
  - Prevent duplicate active subscriptions per user
  - Prevent plan deletion if active subscriptions exist
  - Enforce valid state transitions (Active → Past Due → Canceled)
  - Prevent upgrade from Past Due status
  - ✅
- **Audit Trail**:
  - @CreatedDate, @LastModifiedDate on all entities
  - SubscriptionTransitionLog for all state changes
  - ✅

**Status**: ✅ COMPLIANT - Multi-layer validation with comprehensive business rule enforcement

### ✅ External Integration Resilience Compliance

- **No external integrations in Phase 2** (payment processing deferred to Phase 3)
- **Future-ready**:
  - Service layer abstraction allows easy integration with payment gateways
  - Domain events (SubscriptionCreated, SubscriptionStateChanged) enable event-driven architecture
  - State transition hooks for future webhook notifications
  - ✅

**Status**: ✅ COMPLIANT - Not applicable for Phase 2, architecture supports future integrations

### 🎯 Overall Constitution Compliance: 100%

All constitutional principles are satisfied. No violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/002-product-subscription-engine/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output - Money pattern, state machine, caching strategy
├── data-model.md        # Phase 1 output - BillingPlan, Subscription entities and relationships
├── quickstart.md        # Phase 1 output - Setup and testing guide
├── contracts/           # Phase 1 output - OpenAPI specification
│   ├── plan-api.yaml
│   ├── subscription-api.yaml
│   └── analytics-api.yaml
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── spec.md              # Feature specification
```

### Source Code (repository root)

```text
src/main/java/org/gb/billing/
├── controller/
│   ├── PlanController.java              # POST/GET/PUT/DELETE /api/v1/plans
│   ├── SubscriptionController.java      # POST/GET/PUT/DELETE /api/v1/subscriptions
│   └── AnalyticsController.java         # GET /api/v1/admin/analytics/*
├── service/
│   ├── PlanService.java                 # Plan CRUD, validation, caching
│   ├── SubscriptionService.java         # Subscription lifecycle, state transitions
│   ├── SubscriptionStateMachine.java    # State transition validation logic
│   └── AnalyticsService.java            # Subscription metrics, churn calculation
├── repository/
│   ├── PlanRepository.java              # Spring Data JPA repository
│   ├── SubscriptionRepository.java      # Custom queries for tenant filtering
│   └── SubscriptionTransitionLogRepository.java  # Audit log repository
├── entity/
│   ├── BillingPlan.java                 # Plan aggregate (id, name, price, billingCycle, featureLimits)
│   ├── Subscription.java                # Subscription aggregate (id, userId, planId, status, dates)
│   ├── SubscriptionState.java           # Enum (ACTIVE, PAST_DUE, CANCELED)
│   ├── BillingCycle.java                # Enum (MONTHLY, YEARLY)
│   ├── FeatureLimit.java                # Embeddable (limitType, limitValue)
│   └── SubscriptionTransitionLog.java   # Audit log entity
├── dto/
│   ├── request/
│   │   ├── CreatePlanRequest.java       # Plan creation DTO
│   │   ├── UpdatePlanRequest.java       # Plan update DTO
│   │   ├── SubscribeRequest.java        # Subscription creation DTO
│   │   └── UpgradeRequest.java          # Subscription upgrade DTO
│   └── response/
│       ├── PlanResponse.java            # Plan details response
│       ├── SubscriptionResponse.java    # Subscription details response
│       ├── AnalyticsResponse.java       # Analytics data response
│       └── ErrorResponse.java           # Standardized error response (inherited)
├── exception/
│   ├── PlanNotFoundException.java
│   ├── SubscriptionNotFoundException.java
│   ├── InvalidStateTransitionException.java
│   ├── DuplicateSubscriptionException.java
│   ├── PlanHasActiveSubscriptionsException.java
│   ├── InvalidUpgradeException.java
│   └── GlobalExceptionHandler.java      # @ControllerAdvice (extend from Phase 1)
├── config/
│   ├── CacheConfig.java                 # Redis/Caffeine cache configuration
│   └── TransactionConfig.java           # Transaction isolation levels
├── aspect/
│   └── TenantFilterAspect.java          # AOP for automatic tenant filtering (inherited from Phase 1)
└── BillingApplication.java

src/main/resources/
├── application.yml                      # Cache, transaction configuration
└── db/migration/
    ├── V003__create_billing_plan_table.sql
    ├── V004__create_feature_limit_table.sql
    ├── V005__create_subscription_table.sql
    └── V006__create_subscription_transition_log_table.sql

src/test/java/org/gb/billing/
├── controller/
│   ├── PlanControllerTest.java          # Integration tests with MockMvc
│   ├── SubscriptionControllerTest.java  # Integration tests for lifecycle
│   └── AnalyticsControllerTest.java     # Integration tests for analytics
├── service/
│   ├── PlanServiceTest.java             # Unit tests with mocked repository
│   ├── SubscriptionServiceTest.java     # Unit tests for state machine
│   ├── SubscriptionStateMachineTest.java # Unit tests for transition logic
│   └── AnalyticsServiceTest.java        # Unit tests for calculations
├── repository/
│   ├── PlanRepositoryTest.java          # @DataJpaTest tests
│   └── SubscriptionRepositoryTest.java  # @DataJpaTest with tenant filtering
└── integration/
    └── SubscriptionLifecycleTest.java   # End-to-end lifecycle tests
```

**Structure Decision**: Single backend application structure following Spring Boot layered architecture (consistent with Phase 1). This aligns with the constitution's mandatory layered architecture principle and supports the REST API project type. The structure separates concerns across controller, service, repository, entity, DTO, exception, config, and aspect layers. New aspect layer introduced for cross-cutting tenant filtering concerns.

## Complexity Tracking

> **No complexity violations** - All constitutional principles are satisfied without exceptions.

This feature builds upon the authentication foundation from Phase 1 and implements well-established subscription management patterns. The state machine for subscription lifecycle is a standard domain pattern, and all complexity is justified by business requirements (financial data integrity, multi-tenant isolation, audit trails).

## Out of Scope (Explicitly Excluded)

The following features are intentionally excluded from this phase and will be addressed in future iterations:

- **Subscription Downgrades**: Moving from higher tier to lower tier (e.g., Pro → Free). Requires refund/proration logic.
- **Automatic Subscription Renewals**: Handled by payment processing system in Phase 3.
- **Multi-Currency Support**: All prices are in USD for Phase 2.
- **Proration**: Partial billing period charges for upgrades/cancellations.
