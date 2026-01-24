# Enterprise SaaS Billing Engine Constitution

<!--
Sync Impact Report:
Version: 0.0.0 → 1.0.0
Rationale: Initial constitution creation for Spring Boot SaaS Billing project
Added Sections:
  - All 10 core principles (Layered Architecture, Domain-Driven Design, Security-First, etc.)
  - Technology Stack & Standards
  - Development Workflow & Quality Gates
  - Governance
Templates Status:
  ✅ plan-template.md - reviewed, aligns with constitution
  ✅ spec-template.md - reviewed, aligns with constitution
  ✅ tasks-template.md - reviewed, aligns with constitution
  ✅ checklist-template.md - reviewed, aligns with constitution
  ✅ agent-file-template.md - reviewed, aligns with constitution
Follow-up TODOs: None
-->

## Core Principles

### I. Layered Architecture (NON-NEGOTIABLE)

**Strict separation of concerns across architectural layers is mandatory.**

All code MUST adhere to the following layered structure:

- **Controller Layer** (`controller/`): HTTP endpoints only. No business logic. Delegates to service layer.
- **Service Layer** (`service/`): Business logic and orchestration. Transactional boundaries defined here.
- **Repository Layer** (`repository/`): Data access using Spring Data JPA. No business logic.
- **DTO Layer** (`dto/`): Request/Response objects. Never expose entities directly via APIs.
- **Entity Layer** (`entity/` or `model/`): JPA entities representing database tables.
- **Exception Layer** (`exception/`): Custom exceptions for domain-specific errors.
- **Config Layer** (`config/`): Spring configuration classes (Security, Redis, Swagger, etc.).

**Rationale:** This separation ensures testability, maintainability, and clear boundaries between concerns. It prevents tight coupling and makes the codebase easier to understand and modify.

### II. Domain-Driven Design

**Business domain concepts MUST be explicitly modeled in code.**

- Use rich domain models with behavior, not anemic data structures.
- Aggregate roots (e.g., `Subscription`, `Invoice`) enforce invariants and encapsulate business rules.
- Value objects (e.g., `Money`, `BillingCycle`) represent immutable concepts.
- Domain events (e.g., `SubscriptionActivated`, `PaymentProcessed`) capture state changes.
- Repositories operate on aggregates, not individual entities.

**Rationale:** DDD ensures the code reflects business reality, making it easier for domain experts and developers to communicate. It also prevents business logic from leaking into infrastructure layers.

### III. Security-First (NON-NEGOTIABLE)

**Security is not optional and MUST be implemented from day one.**

- **Authentication:** Stateless JWT-based authentication using Spring Security.
- **Authorization:** Role-based access control (RBAC) with roles: `ROLE_ADMIN` (SaaS Owner), `ROLE_USER` (Subscriber).
- **Multi-Tenancy:** Tenant isolation MUST be enforced at the data access layer using `tenantId` or `organizationId`.
- **Password Storage:** BCrypt encoding mandatory for all passwords.
- **Input Validation:** Use Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`) on all DTOs.
- **HTTPS Only:** All production endpoints MUST use HTTPS.
- **Sensitive Data:** Never log passwords, tokens, or payment details.

**Rationale:** Financial applications handle sensitive data. Security breaches can destroy trust and violate compliance requirements. Security must be baked in, not bolted on.

### IV. Transaction Management

**All state-changing operations MUST be transactional.**

- Use `@Transactional` on service methods that modify data.
- Define transaction boundaries at the service layer, not the repository layer.
- Use appropriate isolation levels for financial operations (e.g., `SERIALIZABLE` for payment processing).
- Handle optimistic locking (`@Version`) for concurrent updates to subscriptions and invoices.
- Rollback on all exceptions by default; use `noRollbackFor` only when justified.

**Rationale:** Financial data requires ACID guarantees. Partial updates can lead to inconsistent state and data corruption.

### V. API Design Excellence

**APIs MUST be RESTful, versioned, and well-documented.**

- **Versioning:** All endpoints MUST include version prefix (e.g., `/api/v1/`).
- **HTTP Methods:** Use semantically correct methods (GET for reads, POST for creates, PUT for updates, DELETE for deletes).
- **Status Codes:** Return appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 500).
- **Pagination:** All list endpoints MUST support pagination and sorting.
- **DTOs:** Never expose JPA entities directly. Use DTOs for request/response.
- **Documentation:** Swagger/OpenAPI annotations MUST be present on all endpoints with descriptions and examples.

**Rationale:** Well-designed APIs are easier to consume, test, and maintain. Versioning prevents breaking changes from affecting existing clients.

### VI. Test-First Development (NON-NEGOTIABLE)

**Tests MUST be written before implementation.**

- **TDD Cycle:** Write test → Test fails → Implement → Test passes → Refactor.
- **Unit Tests:** Every service method MUST have unit tests with mocked dependencies.
- **Integration Tests:** All API endpoints MUST have integration tests using `@SpringBootTest` and `MockMvc`.
- **Repository Tests:** Use `@DataJpaTest` for repository layer tests.
- **Coverage:** Minimum 80% code coverage for service and controller layers.
- **Test Naming:** Use descriptive names following `methodName_scenario_expectedBehavior` pattern.

**Rationale:** TDD ensures code is testable by design, catches bugs early, and serves as living documentation. It prevents regression and increases confidence in refactoring.

### VII. Exception Handling & Observability

**All errors MUST be handled gracefully with comprehensive logging.**

- **Global Exception Handler:** Use `@ControllerAdvice` to catch and handle all exceptions globally.
- **Custom Exceptions:** Define domain-specific exceptions (e.g., `SubscriptionNotFoundException`, `PaymentFailedException`, `InsufficientBalanceException`).
- **Error Responses:** Return consistent JSON error responses with error code, message, timestamp, and path.
- **Logging:** Use SLF4J with structured logging. Log at appropriate levels (ERROR for failures, WARN for recoverable issues, INFO for business events, DEBUG for diagnostics).
- **Monitoring:** Expose Actuator endpoints (`/actuator/health`, `/actuator/metrics`) for monitoring.
- **Never Expose Stack Traces:** In production, return user-friendly messages, not technical details.

**Rationale:** Proper error handling improves user experience and makes debugging easier. Observability is critical for production systems.

### VIII. Performance & Caching

**Performance MUST be considered from the start, not as an afterthought.**

- **Caching:** Use `@Cacheable` for read-heavy operations (e.g., listing plans). Use Redis or Caffeine.
- **N+1 Queries:** Prevent N+1 problems using `@EntityGraph` or `JOIN FETCH` in JPQL.
- **Lazy Loading:** Use lazy loading for associations, but fetch eagerly when needed to avoid LazyInitializationException.
- **Database Indexing:** Add indexes on foreign keys and frequently queried columns.
- **Connection Pooling:** Configure HikariCP with appropriate pool sizes.
- **Rate Limiting:** Implement tiered rate limiting based on subscription plans (e.g., Free: 100 req/min, Pro: 1000 req/min).

**Rationale:** SaaS applications must scale efficiently. Poor performance leads to bad user experience and increased infrastructure costs.

### IX. Data Integrity & Validation

**Data integrity MUST be enforced at multiple layers.**

- **Database Constraints:** Use NOT NULL, UNIQUE, FOREIGN KEY constraints in schema.
- **JPA Validation:** Use `@Column(nullable = false)`, `@NotNull`, etc., on entities.
- **DTO Validation:** Use Bean Validation on DTOs (`@Valid`, `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`).
- **Business Rules:** Enforce business invariants in domain models (e.g., subscription cannot be activated if payment fails).
- **Audit Trail:** Use `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` for auditing.

**Rationale:** Multiple validation layers prevent bad data from entering the system. Audit trails are essential for financial systems.

### X. External Integration Resilience

**All external integrations MUST be resilient and fault-tolerant.**

- **Circuit Breaker:** Use Resilience4j for external API calls (Stripe, PayPal, email services).
- **Retry Logic:** Implement exponential backoff for transient failures.
- **Timeouts:** Set appropriate timeouts for all external calls.
- **Webhooks:** Implement idempotent webhook handlers with signature verification.
- **Fallbacks:** Provide graceful degradation when external services are unavailable.
- **Mocking:** Support mock mode for development and testing.

**Rationale:** External services can fail. Resilience patterns prevent cascading failures and improve system reliability.

## Technology Stack & Standards

### Mandatory Technologies

- **Framework:** Spring Boot 3.x (latest stable version)
- **Language:** Java 17+ (use modern Java features: records, sealed classes, pattern matching)
- **Database:** PostgreSQL (for ACID compliance and financial data integrity)
- **Caching:** Redis or Caffeine
- **Security:** Spring Security 6.x with JWT
- **ORM:** Spring Data JPA with Hibernate
- **Validation:** Jakarta Bean Validation (Hibernate Validator)
- **Documentation:** SpringDoc OpenAPI (Swagger UI)
- **Testing:** JUnit 5, Mockito, AssertJ, Testcontainers (for integration tests)
- **Build Tool:** Maven or Gradle (consistent across project)
- **Logging:** SLF4J with Logback

### Coding Standards

- **Code Style:** Follow Google Java Style Guide or Spring conventions.
- **Naming:** Use descriptive names. Avoid abbreviations unless universally understood.
- **Immutability:** Prefer immutable objects (use `final` fields, Java records).
- **Null Safety:** Avoid nulls. Use `Optional` for potentially absent values.
- **Comments:** Write self-documenting code. Use comments only for "why", not "what".
- **Constants:** Define magic numbers and strings as named constants.

### File Organization

```
src/main/java/com/company/billing/
├── controller/          # REST controllers
├── service/             # Business logic
├── repository/          # Data access
├── entity/              # JPA entities
├── dto/                 # Request/Response DTOs
│   ├── request/
│   └── response/
├── exception/           # Custom exceptions
├── config/              # Spring configuration
├── security/            # Security filters, JWT utilities
├── util/                # Utility classes
└── BillingApplication.java
```

## Development Workflow & Quality Gates

### Code Review Requirements

- All code MUST be reviewed before merging.
- Reviewers MUST verify:
  - Compliance with all constitutional principles
  - Test coverage meets minimum 80%
  - No security vulnerabilities
  - API documentation is complete
  - Error handling is comprehensive

### Quality Gates

- **Build:** All builds MUST pass without warnings.
- **Tests:** All tests MUST pass. No skipped tests in CI.
- **Static Analysis:** Use SonarQube or similar. No critical/blocker issues.
- **Security Scan:** Use OWASP Dependency Check. No high/critical vulnerabilities.
- **Performance:** Load tests MUST meet SLA requirements (e.g., 95th percentile < 200ms).

### Git Workflow

- **Branching:** Use feature branches (`feature/subscription-management`).
- **Commits:** Write meaningful commit messages following Conventional Commits.
- **Pull Requests:** Include description, testing notes, and link to issue/task.

### Documentation Requirements

- **README:** Setup instructions, architecture overview, API documentation link.
- **API Docs:** Swagger UI MUST be accessible at `/swagger-ui.html`.
- **Javadoc:** Public APIs MUST have Javadoc comments.
- **Architecture Decision Records (ADRs):** Document significant architectural decisions.

## Governance

### Constitutional Authority

This constitution supersedes all other coding practices, guidelines, and conventions. When in doubt, refer to this document.

### Amendment Process

1. Propose amendment with rationale and impact analysis.
2. Review with team and stakeholders.
3. Update constitution with new version number.
4. Update all dependent templates and documentation.
5. Communicate changes to all team members.

### Compliance Verification

- All pull requests MUST include a checklist verifying constitutional compliance.
- Code reviews MUST explicitly check for principle violations.
- Automated checks (linters, tests, static analysis) MUST enforce rules where possible.

### Complexity Justification

Any deviation from these principles MUST be:

- Documented with clear rationale
- Approved by technical lead
- Recorded as an ADR
- Reviewed periodically for removal

### Version Management

**Version**: 1.0.0 | **Ratified**: 2026-01-24 | **Last Amended**: 2026-01-24

**Version History:**

- **1.0.0** (2026-01-24): Initial constitution for Enterprise SaaS Billing Engine with Spring Boot principles.
