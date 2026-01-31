# Tasks: Analytics & Documentation

**Branch**: `004-analytics-documentation` | **Spec**: [spec.md](./spec.md)
**Plan**: [implementation_plan.md](./implementation_plan.md)

## Success Metrics
- [ ] 2 new endpoints (`/analytics/mrr`, `/analytics/churn`) implemented and secured
- [ ]Swagger UI accessible at `/swagger-ui.html`
- [ ] <500ms response time for analytics
- [ ] 100% test coverage for new logic

## Phase 1: Setup

*Goal: Ensure project configuration supports Analytics and Documentation.*

- [x] T001 Verify `springdoc-openapi-starter-webmvc-ui` dependency in pom.xml
- [x] T002 Create `OpenApiConfig` class in `src/main/java/org/gb/billing/config/OpenApiConfig.java` to define API Info (Title, Version)

## Phase 2: User Story 1 - Revenue Analytics Dashboard

*Goal: Admin-only endpoints for MRR and Churn statistics.*
*Priority: P1*

### Dependencies & Setup
- [x] TSH01 [US1] Create `RevenueStats` interface projection in `src/main/java/org/gb/billing/dto/RevenueStats.java`
- [x] TSH02 [US1] Create `MrrStats` record in `src/main/java/org/gb/billing/dto/MrrStats.java`
- [x] TSH03 [US1] Create `ChurnStats` record in `src/main/java/org/gb/billing/dto/ChurnStats.java`

### Implementation
- [x] T003 [US1] Add `calculateMonthlyRevenue` JPQL query to `SubscriptionRepository` in `src/main/java/org/gb/billing/repository/SubscriptionRepository.java`
- [x] T004 [US1] Add `countTotalSubscriptions` and `countCancelledSubscriptions` queries to `SubscriptionRepository`
- [x] T005 [US1] Create `AnalyticsService` in `src/main/java/org/gb/billing/service/AnalyticsService.java` with methods `getMrrStats()` and `getChurnStats()`
- [x] T006 [US1] Implement `getMrrStats` logic: Aggregate revenue by month and calculate total
- [x] T007 [US1] Implement `getChurnStats` logic: Calculate percentage (cancelled / total * 100)
- [x] T008 [US1] Create `AnalyticsController` in `src/main/java/org/gb/billing/controller/AnalyticsController.java`
- [x] T009 [US1] Implement `GET /api/v1/analytics/mrr` endpoint in `AnalyticsController` secured with `@PreAuthorize("hasRole('ADMIN')")`
- [x] T010 [US1] Implement `GET /api/v1/analytics/churn` endpoint in `AnalyticsController` secured with `@PreAuthorize("hasRole('ADMIN')")`

### Testing
- [x] T011 [US1] Create `AnalyticsServiceTest` unit test in `src/test/java/org/gb/billing/service/AnalyticsServiceTest.java`
- [x] T012 [US1] Create `AnalyticsControllerIntegrationTest` in `src/test/java/org/gb/billing/controller/AnalyticsControllerIntegrationTest.java` verifying 403 for user and 200 for admin

## Phase 3: User Story 2 - API Documentation

*Goal: Interactive Swagger UI for all endpoints.*
*Priority: P1*

### Implementation
- [x] T013 [US2] Annotate `AnalyticsController` endpoints with `@Operation` and `@ApiResponse`
- [x] T014 [US2] Annotate `MrrStats`, `ChurnStats`, `RevenueStats` schemas with `@Schema` for documentation

### Verification
### Verification
- [ ] T015 [US2] Verify Swagger UI loads at `/swagger-ui.html` and displays Analytics endpoints correctly (Manual Verification Required: Start app via `mvn spring-boot:run`)

## Phase 4: Polish

- [ ] T016 Run full test suite to ensure no regressions (Build Failed: Compilation errors in existing test suite)
- [ ] T017 Review code for checkstyle/formatting compliance

## Dependencies
- US1 (Analytics) MUST complete before US2 (Documentation tags) can be fully applied to new endpoints.
