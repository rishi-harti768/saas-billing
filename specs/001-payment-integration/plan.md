# Implementation Plan: External Payment Integration

**Branch**: `001-payment-integration` | **Date**: 2026-01-25 | **Spec**: [spec.md](file:///c:/Users/Sameer%20Khan%20S/Desktop/saas-billing/specs/001-payment-integration/spec.md)  
**Input**: Feature specification from `/specs/001-payment-integration/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Integrate external payment gateway (Stripe or PayPal) to process subscription payments for the SaaS Billing Engine. The system will redirect users to the payment gateway for checkout, implement webhook endpoints to receive payment notifications, update subscription status based on payment outcomes, and maintain a complete audit trail of all payment transactions. This feature enables the core revenue-generating capability of the platform while ensuring security through webhook signature verification and idempotent processing.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.2, Spring Security 6.x, Spring Data JPA, PostgreSQL, Stripe Java SDK (NEEDS CLARIFICATION: version), Resilience4j (for circuit breaker pattern)  
**Storage**: PostgreSQL (existing database with Flyway migrations)  
**Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 (for integration tests), Testcontainers (NEEDS CLARIFICATION: for Stripe webhook testing)  
**Target Platform**: JVM-based server (Linux/Windows)  
**Project Type**: Single backend application (Spring Boot REST API)  
**Performance Goals**: Process 95% of webhooks within 5 seconds, support 1000 concurrent payment transactions  
**Constraints**: <200ms API response time (p95), webhook idempotency required, ACID transaction guarantees for payment processing  
**Scale/Scope**: Multi-tenant SaaS platform with subscription-based billing, expected 10k+ users

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Layered Architecture (NON-NEGOTIABLE)
- **Status**: PASS
- **Verification**: Payment integration will follow existing layered structure:
  - `controller/PaymentController.java` - REST endpoints for payment initiation and webhook handling
  - `service/PaymentService.java` - Business logic for payment processing
  - `service/WebhookService.java` - Webhook signature verification and event processing
  - `repository/PaymentTransactionRepository.java` - Data access for payment records
  - `repository/WebhookEventRepository.java` - Data access for webhook events
  - `entity/PaymentTransaction.java` - JPA entity for payment data
  - `entity/WebhookEvent.java` - JPA entity for webhook events
  - `dto/request/PaymentInitiationRequest.java` - Request DTOs
  - `dto/response/PaymentResponse.java` - Response DTOs
  - `exception/PaymentProcessingException.java` - Custom exceptions
  - `config/StripeConfig.java` - Payment gateway configuration

### ✅ Domain-Driven Design
- **Status**: PASS
- **Verification**: 
  - Aggregate roots: `PaymentTransaction` (encapsulates payment lifecycle), `WebhookEvent` (encapsulates webhook processing)
  - Value objects: `Money` (amount + currency), `PaymentStatus` (enum)
  - Domain events: `PaymentSucceededEvent`, `PaymentFailedEvent`, `SubscriptionActivatedEvent`
  - Business rules enforced in domain models (e.g., idempotency checks, status transitions)

### ✅ Security-First (NON-NEGOTIABLE)
- **Status**: PASS
- **Verification**:
  - JWT authentication required for payment initiation endpoints
  - Webhook signature verification using payment gateway's signing secret
  - Multi-tenant isolation enforced via `tenantId` in payment records
  - Sensitive data (API keys, webhook secrets) stored in environment variables
  - HTTPS required for all webhook endpoints
  - No logging of payment card details or sensitive tokens

### ✅ Transaction Management
- **Status**: PASS
- **Verification**:
  - `@Transactional` on all webhook processing methods
  - SERIALIZABLE isolation level for payment status updates to prevent race conditions
  - Optimistic locking (`@Version`) on Subscription entity for concurrent updates
  - Rollback on all exceptions by default
  - Idempotency key checks within transaction boundary

### ✅ API Design Excellence
- **Status**: PASS
- **Verification**:
  - Versioned endpoints: `/api/v1/payments/*`, `/api/v1/webhooks/*`
  - RESTful methods: POST for payment initiation, POST for webhook reception
  - Appropriate status codes: 200 (success), 201 (created), 400 (bad request), 401 (unauthorized), 500 (server error)
  - DTOs for all request/response (never expose entities)
  - Swagger/OpenAPI annotations on all endpoints
  - Pagination for payment history endpoints

### ✅ Test-First Development (NON-NEGOTIABLE)
- **Status**: PASS
- **Verification**:
  - Unit tests for `PaymentService`, `WebhookService` with mocked dependencies
  - Integration tests for payment endpoints using `@SpringBootTest` and `MockMvc`
  - Repository tests using `@DataJpaTest`
  - Webhook signature verification tests
  - Idempotency tests (duplicate webhook handling)
  - Target: 80%+ code coverage

### ✅ Exception Handling & Observability
- **Status**: PASS
- **Verification**:
  - Global exception handler via existing `@ControllerAdvice`
  - Custom exceptions: `PaymentProcessingException`, `WebhookVerificationException`, `IdempotencyViolationException`
  - Structured logging with SLF4J for all payment events
  - Actuator metrics for payment success/failure rates
  - Never expose stack traces in webhook responses

### ✅ Performance & Caching
- **Status**: PASS (with considerations)
- **Verification**:
  - No caching for payment data (financial data must be real-time)
  - Database indexes on `paymentGatewayTransactionId`, `subscriptionId`, `webhookEventId`
  - Lazy loading for payment transaction associations
  - HikariCP connection pooling (already configured)
  - Async processing for non-critical webhook side effects (NEEDS CLARIFICATION: email notifications)

### ✅ Data Integrity & Validation
- **Status**: PASS
- **Verification**:
  - Database constraints: NOT NULL, UNIQUE on `paymentGatewayTransactionId`, FOREIGN KEY to subscriptions
  - JPA validation on entities: `@NotNull`, `@Column(nullable = false)`
  - DTO validation: `@Valid`, `@NotBlank`, `@Positive` on amount fields
  - Business rules: subscription cannot activate without successful payment
  - Audit trail: `@CreatedDate`, `@LastModifiedDate` on payment entities

### ✅ External Integration Resilience (NON-NEGOTIABLE)
- **Status**: PASS
- **Verification**:
  - Circuit breaker (Resilience4j) for payment gateway API calls
  - Exponential backoff retry for transient failures
  - Timeouts: 10s for payment initiation, 5s for webhook processing
  - Idempotent webhook handlers with deduplication
  - Graceful degradation: return user-friendly error if gateway unavailable
  - Mock mode support for development/testing

### Constitution Compliance Summary

**Overall Status**: ✅ **PASS** - All constitutional principles are satisfied.

**Notes**:
- All 10 core principles align with the payment integration design
- No violations or exceptions required
- Existing project structure supports all requirements

## Project Structure

### Documentation (this feature)

```text
specs/001-payment-integration/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (to be created)
├── data-model.md        # Phase 1 output (to be created)
├── quickstart.md        # Phase 1 output (to be created)
├── contracts/           # Phase 1 output (to be created)
│   └── payment-api.yaml # OpenAPI specification
├── checklists/
│   └── requirements.md  # Quality checklist (completed)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/java/org/gb/billing/
├── controller/
│   ├── PaymentController.java          # NEW: Payment initiation and history endpoints
│   └── WebhookController.java          # NEW: Webhook reception endpoint
├── service/
│   ├── PaymentService.java             # NEW: Payment processing business logic
│   ├── WebhookService.java             # NEW: Webhook verification and processing
│   └── SubscriptionService.java        # MODIFY: Add payment status update logic
├── repository/
│   ├── PaymentTransactionRepository.java   # NEW: Payment data access
│   └── WebhookEventRepository.java         # NEW: Webhook event data access
├── entity/
│   ├── PaymentTransaction.java         # NEW: Payment transaction entity
│   ├── WebhookEvent.java               # NEW: Webhook event entity
│   ├── Subscription.java               # MODIFY: Add payment gateway subscription ID
│   └── PaymentStatus.java              # NEW: Enum for payment statuses
├── dto/
│   ├── request/
│   │   ├── PaymentInitiationRequest.java   # NEW: Payment initiation DTO
│   │   └── WebhookPayload.java             # NEW: Webhook payload DTO
│   └── response/
│       ├── PaymentResponse.java            # NEW: Payment response DTO
│       └── PaymentHistoryResponse.java     # NEW: Payment history DTO
├── exception/
│   ├── PaymentProcessingException.java     # NEW: Payment processing errors
│   ├── WebhookVerificationException.java   # NEW: Webhook verification errors
│   └── IdempotencyViolationException.java  # NEW: Duplicate webhook errors
├── config/
│   ├── StripeConfig.java               # NEW: Stripe SDK configuration
│   └── ResilienceConfig.java           # NEW: Circuit breaker configuration
├── security/
│   └── WebhookSignatureValidator.java  # NEW: Webhook signature verification
└── util/
    └── IdempotencyKeyGenerator.java    # NEW: Idempotency key generation

src/main/resources/
├── db/migration/
│   └── V3__create_payment_tables.sql   # NEW: Flyway migration for payment tables
└── application.yml                     # MODIFY: Add Stripe configuration

src/test/java/org/gb/billing/
├── controller/
│   ├── PaymentControllerTest.java      # NEW: Payment endpoint tests
│   └── WebhookControllerTest.java      # NEW: Webhook endpoint tests
├── service/
│   ├── PaymentServiceTest.java         # NEW: Payment service unit tests
│   └── WebhookServiceTest.java         # NEW: Webhook service unit tests
└── repository/
    ├── PaymentTransactionRepositoryTest.java   # NEW: Payment repository tests
    └── WebhookEventRepositoryTest.java         # NEW: Webhook repository tests
```

**Structure Decision**: Single backend application following the existing Spring Boot layered architecture. All payment-related code will be organized into the established package structure (`controller`, `service`, `repository`, `entity`, `dto`, `exception`, `config`, `security`, `util`). This maintains consistency with the existing codebase and adheres to the constitutional principle of layered architecture.

## Complexity Tracking

> **No violations** - All constitutional principles are satisfied without exceptions.

This implementation plan is ready for Phase 0 (Research) to resolve the NEEDS CLARIFICATION items identified in the Technical Context section.
