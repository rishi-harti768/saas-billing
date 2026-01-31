# Implementation Plan: Analytics & Documentation

**Branch**: `004-analytics-documentation` | **Date**: 2026-01-31 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/004-analytics-documentation/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement Revenue Analytics Dashboard (MRR and Churn Rate) for Admins and integrated OpenAPI (Swagger) documentation. The analytics will use optimized JPQL queries for performance, and API documentation will be generated using SpringDoc OpenAPI.

## Technical Context

**Language/Version**: Java 17+ (Spring Boot 3.x)
**Primary Dependencies**: Spring Boot Starter Web, Spring Data JPA, Spring Security, SpringDoc OpenAPI, H2/PostgreSQL (dev/prod)
**Storage**: PostgreSQL (Production), H2 (Test)
**Testing**: JUnit 5, Mockito, SpringBootTest
**Target Platform**: Linux Server (JVM)
**Project Type**: Single (Spring Boot Backend)
**Performance Goals**: <500ms for analytics queries
**Constraints**: Role-based access (Admin only for analytics), Strict typing, Layered Architecture
**Scale/Scope**: ~10k subscriptions initial target

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Layered Architecture**: Validated. Plan respects Controller/Service/Repository layers.
- [x] **Domain-Driven Design**: Validated. Analytics will likely use DTO projections/Value Objects.
- [x] **Security-First**: Validated. RBAC (`ROLE_ADMIN`) required for new endpoints.
- [x] **Transaction Management**: Validated. Read-only transactions for analytics.
- [x] **API Design Excellence**: Validated. RESTful endpoints, documented via Swagger.
- [x] **Test-First Development**: Validated. Tests are mandatory.
- [x] **Exception Handling**: Validated. Global exception handling in place.
- [x] **Performance & Caching**: Validated. Optimized JPQL required.
- [x] **Data Integrity**: Validated. No state changes, but data accuracy is key.
- [x] **External Integration**: N/A for this feature.

## Project Structure

### Documentation (this feature)

```text
specs/004-analytics-documentation/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
src/main/java/org/gb/billing/
├── controller/          # AnalyticsController.java
├── service/             # AnalyticsService.java
├── repository/          # SubscriptionRepository.java (custom queries)
├── dto/                 # AnalyticsResponse DTOs
└── config/              # OpenApiConfig.java (if not exists)
```

**Structure Decision**: Standard layered architecture as per constitution.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None      | N/A        | N/A                                 |
