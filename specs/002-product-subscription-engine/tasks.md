# Tasks: Product & Subscription Engine

**Input**: Design documents from `/specs/002-product-subscription-engine/`  
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Following TDD approach per constitution - tests written before implementation

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/org/gb/billing/`, `src/test/java/org/gb/billing/`
- **Resources**: `src/main/resources/`, `src/test/resources/`
- **Migrations**: `src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and database schema setup

- [ ] T001 Create database migration V003\_\_create_billing_plan_table.sql in src/main/resources/db/migration/
- [ ] T002 Create database migration V004\_\_create_feature_limit_table.sql in src/main/resources/db/migration/
- [ ] T003 Create database migration V005\_\_create_subscription_table.sql in src/main/resources/db/migration/
- [ ] T004 Create database migration V006\_\_create_subscription_transition_log_table.sql in src/main/resources/db/migration/
- [ ] T005 [P] Add Caffeine cache dependency to pom.xml (if not present)
- [ ] T006 [P] Configure Caffeine cache in src/main/resources/application.yml
- [ ] T006a [P] Add subscription grace period configuration property (billing.subscription.grace-period-days) in src/main/resources/application.yml
- [ ] T007 [P] Create CacheConfig.java in src/main/java/org/gb/billing/config/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain entities and enums that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T008 [P] Create BillingCycle enum in src/main/java/org/gb/billing/entity/BillingCycle.java
- [ ] T009 [P] Create SubscriptionState enum in src/main/java/org/gb/billing/entity/SubscriptionState.java
- [ ] T010 [P] Create BillingPlan entity in src/main/java/org/gb/billing/entity/BillingPlan.java
- [ ] T011 [P] Create FeatureLimit entity in src/main/java/org/gb/billing/entity/FeatureLimit.java
- [ ] T012 [P] Create Subscription entity in src/main/java/org/gb/billing/entity/Subscription.java
- [ ] T013 [P] Create SubscriptionTransitionLog entity in src/main/java/org/gb/billing/entity/SubscriptionTransitionLog.java
- [ ] T014 [P] Create PlanRepository interface in src/main/java/org/gb/billing/repository/PlanRepository.java
- [ ] T015 [P] Create SubscriptionRepository interface in src/main/java/org/gb/billing/repository/SubscriptionRepository.java
- [ ] T016 [P] Create SubscriptionTransitionLogRepository interface in src/main/java/org/gb/billing/repository/SubscriptionTransitionLogRepository.java
- [ ] T017 [P] Create custom exception PlanNotFoundException in src/main/java/org/gb/billing/exception/PlanNotFoundException.java
- [ ] T018 [P] Create custom exception SubscriptionNotFoundException in src/main/java/org/gb/billing/exception/SubscriptionNotFoundException.java
- [ ] T019 [P] Create custom exception InvalidStateTransitionException in src/main/java/org/gb/billing/exception/InvalidStateTransitionException.java
- [ ] T020 [P] Create custom exception DuplicateSubscriptionException in src/main/java/org/gb/billing/exception/DuplicateSubscriptionException.java
- [ ] T021 [P] Create custom exception PlanHasActiveSubscriptionsException in src/main/java/org/gb/billing/exception/PlanHasActiveSubscriptionsException.java
- [ ] T022 [P] Create custom exception InvalidUpgradeException in src/main/java/org/gb/billing/exception/InvalidUpgradeException.java
- [ ] T023 Update GlobalExceptionHandler in src/main/java/org/gb/billing/exception/GlobalExceptionHandler.java to handle new exceptions
- [ ] T024 [P] Create SubscriptionStateMachine service in src/main/java/org/gb/billing/service/SubscriptionStateMachine.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - SaaS Owner Creates and Manages Billing Plans (Priority: P1) 🎯 MVP

**Goal**: Enable admins to create, view, update, and delete billing plans with pricing and feature limits

**Independent Test**: Admin can create a "Pro" plan with price $29.99, retrieve it, update the price to $39.99, and verify deletion is prevented if subscriptions exist

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T025 [P] [US1] Create PlanRepositoryTest in src/test/java/org/gb/billing/repository/PlanRepositoryTest.java
- [ ] T026 [P] [US1] Create PlanServiceTest in src/test/java/org/gb/billing/service/PlanServiceTest.java
- [ ] T027 [P] [US1] Create PlanControllerTest in src/test/java/org/gb/billing/controller/PlanControllerTest.java

### DTOs for User Story 1

- [ ] T028 [P] [US1] Create CreatePlanRequest DTO in src/main/java/org/gb/billing/dto/request/CreatePlanRequest.java
- [ ] T029 [P] [US1] Create UpdatePlanRequest DTO in src/main/java/org/gb/billing/dto/request/UpdatePlanRequest.java
- [ ] T030 [P] [US1] Create FeatureLimitRequest DTO in src/main/java/org/gb/billing/dto/request/FeatureLimitRequest.java
- [ ] T031 [P] [US1] Create PlanResponse DTO in src/main/java/org/gb/billing/dto/response/PlanResponse.java
- [ ] T032 [P] [US1] Create FeatureLimitResponse DTO in src/main/java/org/gb/billing/dto/response/FeatureLimitResponse.java
- [ ] T033 [P] [US1] Create PlanListResponse DTO in src/main/java/org/gb/billing/dto/response/PlanListResponse.java

### Implementation for User Story 1

- [ ] T034 [US1] Implement PlanService.createPlan() with feature limit management in src/main/java/org/gb/billing/service/PlanService.java
- [ ] T035 [US1] Implement PlanService.getAllPlans() with @Cacheable in src/main/java/org/gb/billing/service/PlanService.java
- [ ] T036 [US1] Implement PlanService.getPlanById() with @Cacheable in src/main/java/org/gb/billing/service/PlanService.java
- [ ] T037 [US1] Implement PlanService.updatePlan() with @CacheEvict in src/main/java/org/gb/billing/service/PlanService.java
- [ ] T038 [US1] Implement PlanService.deletePlan() with soft delete and active subscription check in src/main/java/org/gb/billing/service/PlanService.java
- [ ] T039 [US1] Create PlanController with POST /api/v1/plans endpoint in src/main/java/org/gb/billing/controller/PlanController.java
- [ ] T040 [US1] Add GET /api/v1/plans endpoint to PlanController in src/main/java/org/gb/billing/controller/PlanController.java
- [ ] T041 [US1] Add GET /api/v1/plans/{id} endpoint to PlanController in src/main/java/org/gb/billing/controller/PlanController.java
- [ ] T042 [US1] Add PUT /api/v1/plans/{id} endpoint to PlanController in src/main/java/org/gb/billing/controller/PlanController.java
- [ ] T043 [US1] Add DELETE /api/v1/plans/{id} endpoint to PlanController in src/main/java/org/gb/billing/controller/PlanController.java
- [ ] T044 [US1] Add Swagger/OpenAPI annotations to all PlanController endpoints
- [ ] T045 [US1] Add validation annotations to all Plan DTOs (@Valid, @NotBlank, @DecimalMin, etc.)
- [ ] T046 [US1] Add logging for plan operations (create, update, delete) in PlanService

**Checkpoint**: At this point, User Story 1 should be fully functional - admins can manage billing plans

---

## Phase 4: User Story 2 - Customer Subscribes to a Billing Plan (Priority: P1)

**Goal**: Enable customers to subscribe to a billing plan and view their subscription details

**Independent Test**: Customer can subscribe to "Pro" plan, verify subscription is created with ACTIVE status, and retrieve subscription details showing plan info and next billing date

### Tests for User Story 2

- [ ] T047 [P] [US2] Create SubscriptionRepositoryTest in src/test/java/org/gb/billing/repository/SubscriptionRepositoryTest.java
- [ ] T048 [P] [US2] Create SubscriptionServiceTest in src/test/java/org/gb/billing/service/SubscriptionServiceTest.java
- [ ] T049 [P] [US2] Create SubscriptionControllerTest in src/test/java/org/gb/billing/controller/SubscriptionControllerTest.java

### DTOs for User Story 2

- [ ] T050 [P] [US2] Create SubscribeRequest DTO in src/main/java/org/gb/billing/dto/request/SubscribeRequest.java
- [ ] T051 [P] [US2] Create SubscriptionResponse DTO in src/main/java/org/gb/billing/dto/response/SubscriptionResponse.java
- [ ] T052 [P] [US2] Create PlanSummary DTO in src/main/java/org/gb/billing/dto/response/PlanSummary.java

### Implementation for User Story 2

- [ ] T053 [US2] Implement SubscriptionService.createSubscription() with duplicate check and state logging in src/main/java/org/gb/billing/service/SubscriptionService.java
- [ ] T054 [US2] Implement SubscriptionService.getSubscriptionById() with tenant filtering in src/main/java/org/gb/billing/service/SubscriptionService.java
- [ ] T055 [US2] Implement SubscriptionService.getMySubscription() for current user in src/main/java/org/gb/billing/service/SubscriptionService.java
- [ ] T056 [US2] Implement billing date calculation logic in Subscription entity (calculateNextBillingDate method)
- [ ] T057 [US2] Create SubscriptionController with POST /api/v1/subscriptions endpoint in src/main/java/org/gb/billing/controller/SubscriptionController.java
- [ ] T058 [US2] Add GET /api/v1/subscriptions/my-subscription endpoint to SubscriptionController in src/main/java/org/gb/billing/controller/SubscriptionController.java
- [ ] T059 [US2] Add GET /api/v1/subscriptions/{id} endpoint to SubscriptionController in src/main/java/org/gb/billing/controller/SubscriptionController.java
- [ ] T060 [US2] Add Swagger/OpenAPI annotations to all SubscriptionController endpoints
- [ ] T061 [US2] Add validation annotations to Subscription DTOs
- [ ] T062 [US2] Add logging for subscription creation in SubscriptionService
- [ ] T063 [US2] Implement tenant isolation check in SubscriptionService (prevent cross-tenant access)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work - customers can subscribe to plans created by admins

---

## Phase 5: User Story 3 - Customer Upgrades Subscription Plan (Priority: P1)

**Goal**: Enable customers to upgrade their subscription from a lower tier to a higher tier

**Independent Test**: Customer with "Free" plan can upgrade to "Pro" plan, verify subscription is updated with new plan, next billing date is recalculated, and state transition is logged

### Tests for User Story 3

- [ ] T064 [P] [US3] Add upgrade test cases to SubscriptionServiceTest in src/test/java/org/gb/billing/service/SubscriptionServiceTest.java
- [ ] T065 [P] [US3] Add upgrade integration test to SubscriptionControllerTest in src/test/java/org/gb/billing/controller/SubscriptionControllerTest.java

### DTOs for User Story 3

- [ ] T066 [P] [US3] Create UpgradeRequest DTO in src/main/java/org/gb/billing/dto/request/UpgradeRequest.java

### Implementation for User Story 3

- [ ] T067 [US3] Implement SubscriptionService.upgradeSubscription() with state validation and billing recalculation in src/main/java/org/gb/billing/service/SubscriptionService.java
- [ ] T068 [US3] Implement upgradeToPlan() method in Subscription entity
- [ ] T069 [US3] Add state transition validation in SubscriptionStateMachine (prevent upgrade from PAST_DUE)
- [ ] T070 [US3] Add PUT /api/v1/subscriptions/{id}/upgrade endpoint to SubscriptionController in src/main/java/org/gb/billing/controller/SubscriptionController.java
- [ ] T071 [US3] Add Swagger/OpenAPI annotations to upgrade endpoint
- [ ] T072 [US3] Add logging for subscription upgrades in SubscriptionService
- [ ] T073 [US3] Handle OptimisticLockException in SubscriptionService with retry logic

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work - customers can upgrade subscriptions

---

## Phase 6: User Story 4 - Customer Cancels Subscription (Priority: P1)

**Goal**: Enable customers to cancel their active subscriptions

**Independent Test**: Customer with "Active" subscription can cancel it, verify status changes to "Canceled", next billing date is cleared, and cancellation timestamp is recorded

### Tests for User Story 4

- [ ] T074 [P] [US4] Add cancellation test cases to SubscriptionServiceTest in src/test/java/org/gb/billing/service/SubscriptionServiceTest.java
- [ ] T075 [P] [US4] Add cancellation integration test to SubscriptionControllerTest in src/test/java/org/gb/billing/controller/SubscriptionControllerTest.java

### Implementation for User Story 4

- [ ] T076 [US4] Implement SubscriptionService.cancelSubscription() with state transition logging in src/main/java/org/gb/billing/service/SubscriptionService.java
- [ ] T077 [US4] Implement cancel() method with access retention logic in Subscription entity
- [ ] T078 [US4] Add state transition validation in SubscriptionStateMachine (prevent canceling already canceled subscription)
- [ ] T079 [US4] Add DELETE /api/v1/subscriptions/{id}/cancel endpoint to SubscriptionController in src/main/java/org/gb/billing/controller/SubscriptionController.java
- [ ] T080 [US4] Add Swagger/OpenAPI annotations to cancel endpoint
- [ ] T081 [US4] Add logging for subscription cancellations in SubscriptionService

**Checkpoint**: At this point, complete subscription lifecycle is functional (create, upgrade, cancel)

---

## Phase 7: User Story 5 - System Manages Subscription State Transitions (Priority: P2)

**Goal**: Implement automated subscription state transitions based on payment status and business rules

**Independent Test**: Subscription can transition from ACTIVE → PAST_DUE on payment failure, PAST_DUE → ACTIVE on successful payment, and PAST_DUE → CANCELED after grace period, with all transitions logged

### Tests for User Story 5

- [ ] T082 [P] [US5] Create SubscriptionStateMachineTest in src/test/java/org/gb/billing/service/SubscriptionStateMachineTest.java
- [ ] T083 [P] [US5] Add state transition test cases to SubscriptionServiceTest

### Implementation for User Story 5

- [ ] T084 [US5] Implement validateTransition() in SubscriptionStateMachine with transition rules
- [ ] T085 [US5] Implement transitionTo() in SubscriptionStateMachine with logging
- [ ] T086 [US5] Implement SubscriptionService.transitionToPastDue() for payment failures
- [ ] T087 [US5] Implement SubscriptionService.transitionToActive() for successful payments
- [ ] T088 [US5] Implement SubscriptionService.transitionToCanceled() for grace period expiry
- [ ] T089 [US5] Add state transition logging to SubscriptionTransitionLogRepository
- [ ] T090 [US5] Add GET /api/v1/subscriptions/{id}/transitions endpoint to SubscriptionController for viewing transition history
- [ ] T091 [US5] Add Swagger/OpenAPI annotations to transitions endpoint
- [ ] T092 [US5] Add comprehensive logging for all state transitions

**Checkpoint**: State machine is fully functional with audit trail

---

## Phase 8: User Story 6 - Admin Views Subscription Analytics (Priority: P2)

**Goal**: Provide admins with subscription metrics including active subscriptions by plan, churn rate, and subscription trends

**Independent Test**: Admin can retrieve subscription count by plan, subscription count by status, calculate churn rate for a period, and view subscription growth trends

### Tests for User Story 6

- [ ] T093 [P] [US6] Create AnalyticsServiceTest in src/test/java/org/gb/billing/service/AnalyticsServiceTest.java
- [ ] T094 [P] [US6] Create AnalyticsControllerTest in src/test/java/org/gb/billing/controller/AnalyticsControllerTest.java

### DTOs for User Story 6

- [ ] T095 [P] [US6] Create SubscriptionCountByPlan DTO in src/main/java/org/gb/billing/dto/response/SubscriptionCountByPlan.java
- [ ] T096 [P] [US6] Create SubscriptionCountByStatus DTO in src/main/java/org/gb/billing/dto/response/SubscriptionCountByStatus.java
- [ ] T097 [P] [US6] Create ChurnRateResponse DTO in src/main/java/org/gb/billing/dto/response/ChurnRateResponse.java
- [ ] T098 [P] [US6] Create SubscriptionGrowthData DTO in src/main/java/org/gb/billing/dto/response/SubscriptionGrowthData.java
- [ ] T099 [P] [US6] Create RevenueSummaryResponse DTO in src/main/java/org/gb/billing/dto/response/RevenueSummaryResponse.java
- [ ] T100 [P] [US6] Create RevenueByPlan DTO in src/main/java/org/gb/billing/dto/response/RevenueByPlan.java

### Repository Queries for User Story 6

- [ ] T101 [P] [US6] Add countActiveSubscriptionsByPlan() query to SubscriptionRepository
- [ ] T102 [P] [US6] Add countSubscriptionsByStatus() query to SubscriptionRepository
- [ ] T103 [P] [US6] Add calculateChurnRate() native query to SubscriptionRepository
- [ ] T104 [P] [US6] Add findSubscriptionGrowthData() query to SubscriptionRepository

### Implementation for User Story 6

- [ ] T105 [US6] Implement AnalyticsService.getSubscriptionCountByPlan() in src/main/java/org/gb/billing/service/AnalyticsService.java
- [ ] T106 [US6] Implement AnalyticsService.getSubscriptionCountByStatus() in src/main/java/org/gb/billing/service/AnalyticsService.java
- [ ] T107 [US6] Implement AnalyticsService.calculateChurnRate() in src/main/java/org/gb/billing/service/AnalyticsService.java
- [ ] T108 [US6] Implement AnalyticsService.getSubscriptionGrowth() in src/main/java/org/gb/billing/service/AnalyticsService.java
- [ ] T109 [US6] Implement AnalyticsService.getRevenueSummary() with MRR/ARR calculations in src/main/java/org/gb/billing/service/AnalyticsService.java
- [ ] T110 [US6] Create AnalyticsController with GET /api/v1/admin/analytics/subscriptions/by-plan endpoint in src/main/java/org/gb/billing/controller/AnalyticsController.java
- [ ] T111 [US6] Add GET /api/v1/admin/analytics/subscriptions/by-status endpoint to AnalyticsController
- [ ] T112 [US6] Add GET /api/v1/admin/analytics/churn-rate endpoint to AnalyticsController
- [ ] T113 [US6] Add GET /api/v1/admin/analytics/subscription-growth endpoint to AnalyticsController
- [ ] T114 [US6] Add GET /api/v1/admin/analytics/revenue-summary endpoint to AnalyticsController
- [ ] T115 [US6] Add Swagger/OpenAPI annotations to all AnalyticsController endpoints
- [ ] T116 [US6] Add ROLE_ADMIN authorization to all analytics endpoints
- [ ] T117 [US6] Add caching to analytics queries with 1-minute TTL

**Checkpoint**: All user stories complete - full feature functionality delivered

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T118 [P] Add comprehensive Javadoc comments to all public service methods
- [ ] T119 [P] Add database indexes per data-model.md (idx_billing_plan_deleted, idx_subscription_tenant_status, etc.)
- [ ] T120 [P] Create sample data SQL script for testing in src/main/resources/db/migration/V007\_\_insert_sample_plans.sql
- [ ] T121 [P] Update README.md with feature documentation and API endpoints
- [ ] T122 Run all integration tests and verify quickstart.md workflow
- [ ] T123 Performance testing: Verify plan caching reduces DB queries
- [ ] T124 Performance testing: Verify analytics queries complete in <3 seconds for 10,000 subscriptions
- [ ] T125 Security audit: Verify tenant isolation prevents cross-tenant access
- [ ] T126 Security audit: Verify ROLE_ADMIN enforcement on admin endpoints
- [ ] T127 Code review: Verify all exceptions are handled by GlobalExceptionHandler
- [ ] T128 Code review: Verify all DTOs have validation annotations
- [ ] T129 [P] Add actuator metrics for subscription operations (create, upgrade, cancel counts)
- [ ] T130 Final validation: Run complete quickstart.md workflow end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-8)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (US1 → US2 → US3 → US4 → US5 → US6)
- **Polish (Phase 9)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Depends on US1 (needs plans to exist)
- **User Story 3 (P1)**: Can start after US2 (needs subscriptions to upgrade)
- **User Story 4 (P1)**: Can start after US2 (needs subscriptions to cancel)
- **User Story 5 (P2)**: Can start after US2 (needs subscriptions for state transitions)
- **User Story 6 (P2)**: Can start after US2 (needs subscription data for analytics)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- DTOs before services (services use DTOs)
- Repository queries before services (services use repositories)
- Services before controllers (controllers use services)
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T007) can run in parallel
- All Foundational entity tasks (T008-T013) can run in parallel
- All Foundational repository tasks (T014-T016) can run in parallel
- All Foundational exception tasks (T017-T022) can run in parallel
- Within each user story:
  - All test tasks marked [P] can run in parallel
  - All DTO tasks marked [P] can run in parallel
  - All repository query tasks marked [P] can run in parallel
- Once US1 completes, US3, US4, US5, US6 can start in parallel (all depend on US2 for subscription data)
- All Polish tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task T025: "Create PlanRepositoryTest in src/test/java/org/gb/billing/repository/PlanRepositoryTest.java"
Task T026: "Create PlanServiceTest in src/test/java/org/gb/billing/service/PlanServiceTest.java"
Task T027: "Create PlanControllerTest in src/test/java/org/gb/billing/controller/PlanControllerTest.java"

# Launch all DTOs for User Story 1 together:
Task T028: "Create CreatePlanRequest DTO in src/main/java/org/gb/billing/dto/request/CreatePlanRequest.java"
Task T029: "Create UpdatePlanRequest DTO in src/main/java/org/gb/billing/dto/request/UpdatePlanRequest.java"
Task T030: "Create FeatureLimitRequest DTO in src/main/java/org/gb/billing/dto/request/FeatureLimitRequest.java"
Task T031: "Create PlanResponse DTO in src/main/java/org/gb/billing/dto/response/PlanResponse.java"
Task T032: "Create FeatureLimitResponse DTO in src/main/java/org/gb/billing/dto/response/FeatureLimitResponse.java"
Task T033: "Create PlanListResponse DTO in src/main/java/org/gb/billing/dto/response/PlanListResponse.java"
```

---

## Parallel Example: User Story 6 (Analytics)

```bash
# Launch all DTOs for User Story 6 together:
Task T095: "Create SubscriptionCountByPlan DTO"
Task T096: "Create SubscriptionCountByStatus DTO"
Task T097: "Create ChurnRateResponse DTO"
Task T098: "Create SubscriptionGrowthData DTO"
Task T099: "Create RevenueSummaryResponse DTO"
Task T100: "Create RevenueByPlan DTO"

# Launch all repository queries together:
Task T101: "Add countActiveSubscriptionsByPlan() query"
Task T102: "Add countSubscriptionsByStatus() query"
Task T103: "Add calculateChurnRate() native query"
Task T104: "Add findSubscriptionGrowthData() query"
```

---

## Implementation Strategy

### MVP First (User Stories 1-2 Only)

1. Complete Phase 1: Setup (T001-T007)
2. Complete Phase 2: Foundational (T008-T024) - CRITICAL
3. Complete Phase 3: User Story 1 (T025-T046) - Plan management
4. Complete Phase 4: User Story 2 (T047-T063) - Subscription creation
5. **STOP and VALIDATE**: Test US1 and US2 independently
6. Deploy/demo if ready - customers can subscribe to plans

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Admins can manage plans
3. Add User Story 2 → Test independently → Customers can subscribe (MVP!)
4. Add User Story 3 → Test independently → Customers can upgrade
5. Add User Story 4 → Test independently → Customers can cancel
6. Add User Story 5 → Test independently → Automated state management
7. Add User Story 6 → Test independently → Analytics dashboard
8. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T024)
2. Once Foundational is done:
   - Developer A: User Story 1 (T025-T046) - Plan management
   - Developer B: User Story 2 (T047-T063) - Subscription creation
3. After US2 completes:
   - Developer A: User Story 3 (T064-T073) - Upgrades
   - Developer B: User Story 4 (T074-T081) - Cancellations
   - Developer C: User Story 5 (T082-T092) - State machine
   - Developer D: User Story 6 (T093-T117) - Analytics
4. Stories complete and integrate independently

---

## Task Summary

**Total Tasks**: 131

**Tasks by Phase**:

- Phase 1 (Setup): 8 tasks
- Phase 2 (Foundational): 17 tasks
- Phase 3 (US1 - Plan Management): 22 tasks
- Phase 4 (US2 - Subscription Creation): 17 tasks
- Phase 5 (US3 - Upgrades): 10 tasks
- Phase 6 (US4 - Cancellations): 8 tasks
- Phase 7 (US5 - State Transitions): 11 tasks
- Phase 8 (US6 - Analytics): 25 tasks
- Phase 9 (Polish): 13 tasks

**Parallel Opportunities**: 67 tasks marked [P] can run in parallel within their phase

**Independent Test Criteria**:

- US1: Admin can create, read, update, and delete plans
- US2: Customer can subscribe to a plan and view subscription
- US3: Customer can upgrade subscription to higher tier
- US4: Customer can cancel active subscription
- US5: System can transition subscription states with audit trail
- US6: Admin can view subscription metrics and analytics

**Suggested MVP Scope**: User Stories 1-2 (Plan management + Subscription creation) = 46 tasks

---

## Notes

- [P] tasks = different files, no dependencies within phase
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD approach per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Follow constitution: 80%+ test coverage, layered architecture, security-first
- All monetary values use BigDecimal per research.md
- All state transitions use SubscriptionStateMachine per research.md
- All plan queries use Caffeine cache per research.md
- All subscriptions use optimistic locking (@Version) per research.md
