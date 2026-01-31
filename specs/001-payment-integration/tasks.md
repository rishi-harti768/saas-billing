# Tasks: External Payment Integration

**Input**: Design documents from `/specs/001-payment-integration/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/payment-api.yaml, quickstart.md

**Tests**: Tests are included based on the Test-First Development constitutional principle (NON-NEGOTIABLE).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: Spring Boot application at repository root
- Paths: `src/main/java/org/gb/billing/`, `src/main/resources/`, `src/test/java/org/gb/billing/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency configuration

- [ ] T001 Add Stripe Java SDK 26.13.0 dependency to pom.xml
- [ ] T002 [P] Add Resilience4j Spring Boot 3 Starter 2.2.0 dependencies to pom.xml
- [ ] T003 [P] Add Testcontainers 1.19.3 and WireMock 3.3.1 test dependencies to pom.xml
- [ ] T004 [P] Configure Stripe API keys in src/main/resources/application.yml
- [ ] T005 [P] Configure Resilience4j circuit breaker settings in src/main/resources/application.yml
- [ ] T006 [P] Configure async executor for email notifications in src/main/java/org/gb/billing/config/AsyncConfig.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T007 Create Flyway migration V3__create_payment_tables.sql in src/main/resources/db/migration/
- [ ] T008 [P] Create PaymentStatus enum in src/main/java/org/gb/billing/entity/PaymentStatus.java
- [ ] T009 [P] Create PaymentMethod enum in src/main/java/org/gb/billing/entity/PaymentMethod.java
- [ ] T010 [P] Create WebhookStatus enum in src/main/java/org/gb/billing/entity/WebhookStatus.java
- [ ] T011 [P] Update SubscriptionStatus enum with PAST_DUE and PAYMENT_FAILED in src/main/java/org/gb/billing/entity/SubscriptionStatus.java
- [ ] T012 [P] Create PaymentTransaction entity in src/main/java/org/gb/billing/entity/PaymentTransaction.java
- [ ] T013 [P] Create WebhookEvent entity in src/main/java/org/gb/billing/entity/WebhookEvent.java
- [ ] T014 Update Subscription entity with payment gateway fields in src/main/java/org/gb/billing/entity/Subscription.java
- [ ] T015 [P] Create PaymentTransactionRepository in src/main/java/org/gb/billing/repository/PaymentTransactionRepository.java
- [ ] T016 [P] Create WebhookEventRepository in src/main/java/org/gb/billing/repository/WebhookEventRepository.java
- [ ] T017 [P] Create StripeConfig in src/main/java/org/gb/billing/config/StripeConfig.java
- [ ] T018 [P] Create ResilienceConfig in src/main/java/org/gb/billing/config/ResilienceConfig.java
- [ ] T019 [P] Create PaymentProcessingException in src/main/java/org/gb/billing/exception/PaymentProcessingException.java
- [ ] T020 [P] Create WebhookVerificationException in src/main/java/org/gb/billing/exception/WebhookVerificationException.java
- [ ] T021 [P] Create IdempotencyViolationException in src/main/java/org/gb/billing/exception/IdempotencyViolationException.java
- [ ] T022 [P] Create WebhookSignatureValidator utility in src/main/java/org/gb/billing/security/WebhookSignatureValidator.java
- [ ] T023 Run Flyway migration to create payment tables (mvn flyway:migrate)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Subscriber Initiates Payment (Priority: P1) 🎯 MVP

**Goal**: Enable subscribers to initiate payment for a billing plan, redirect to Stripe checkout, and automatically activate subscription upon successful payment via webhook.

**Independent Test**: Create a test subscription, complete payment in Stripe sandbox, verify subscription status changes to "Active" in database.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T024 [P] [US1] Unit test for PaymentService.initiatePayment() in src/test/java/org/gb/billing/service/PaymentServiceTest.java
- [ ] T025 [P] [US1] Unit test for WebhookService.processWebhook() in src/test/java/org/gb/billing/service/WebhookServiceTest.java
- [ ] T026 [P] [US1] Integration test for POST /api/v1/payments/initiate in src/test/java/org/gb/billing/controller/PaymentControllerTest.java
- [ ] T027 [P] [US1] Integration test for POST /api/v1/webhooks/stripe with success event in src/test/java/org/gb/billing/controller/WebhookControllerTest.java
- [ ] T028 [P] [US1] Repository test for PaymentTransactionRepository in src/test/java/org/gb/billing/repository/PaymentTransactionRepositoryTest.java
- [ ] T029 [P] [US1] Repository test for WebhookEventRepository in src/test/java/org/gb/billing/repository/WebhookEventRepositoryTest.java

### DTOs for User Story 1

- [ ] T030 [P] [US1] Create PaymentInitiationRequest DTO in src/main/java/org/gb/billing/dto/request/PaymentInitiationRequest.java
- [ ] T031 [P] [US1] Create PaymentResponse DTO in src/main/java/org/gb/billing/dto/response/PaymentResponse.java
- [ ] T032 [P] [US1] Create WebhookPayload DTO in src/main/java/org/gb/billing/dto/request/WebhookPayload.java

### Services for User Story 1

- [ ] T033 [US1] Implement PaymentService.initiatePayment() in src/main/java/org/gb/billing/service/PaymentService.java
- [ ] T034 [US1] Implement WebhookService.processWebhook() in src/main/java/org/gb/billing/service/WebhookService.java
- [ ] T035 [US1] Implement WebhookService.verifySignature() in src/main/java/org/gb/billing/service/WebhookService.java
- [ ] T036 [US1] Implement WebhookService.handlePaymentSuccess() in src/main/java/org/gb/billing/service/WebhookService.java
- [ ] T037 [US1] Update SubscriptionService.updateSubscriptionStatus() in src/main/java/org/gb/billing/service/SubscriptionService.java

### Controllers for User Story 1

- [ ] T038 [US1] Implement POST /api/v1/payments/initiate in src/main/java/org/gb/billing/controller/PaymentController.java
- [ ] T039 [US1] Implement POST /api/v1/webhooks/stripe in src/main/java/org/gb/billing/controller/WebhookController.java

### Integration and Validation for User Story 1

- [ ] T040 [US1] Add @CircuitBreaker annotation to Stripe API calls in PaymentService
- [ ] T041 [US1] Add @Transactional with SERIALIZABLE isolation to webhook processing in WebhookService
- [ ] T042 [US1] Add validation annotations to PaymentInitiationRequest DTO
- [ ] T043 [US1] Add Swagger/OpenAPI annotations to PaymentController endpoints
- [ ] T044 [US1] Add Swagger/OpenAPI annotations to WebhookController endpoint
- [ ] T045 [US1] Add structured logging to PaymentService and WebhookService
- [ ] T046 [US1] Verify all tests pass (mvn test)

**Checkpoint**: At this point, User Story 1 should be fully functional - users can initiate payment and subscriptions activate on successful payment

---

## Phase 4: User Story 2 - Handle Payment Failures (Priority: P2)

**Goal**: Gracefully handle payment failures by updating subscription status to "Past Due" or "Failed" and notifying users via email.

**Independent Test**: Use Stripe test card that simulates payment failure, verify subscription status changes to "Past Due" and user receives email notification.

### Tests for User Story 2

- [ ] T047 [P] [US2] Unit test for WebhookService.handlePaymentFailure() in src/test/java/org/gb/billing/service/WebhookServiceTest.java
- [ ] T048 [P] [US2] Unit test for EmailService.sendPaymentFailureEmail() in src/test/java/org/gb/billing/service/EmailServiceTest.java
- [ ] T049 [P] [US2] Integration test for POST /api/v1/webhooks/stripe with failure event in src/test/java/org/gb/billing/controller/WebhookControllerTest.java

### DTOs for User Story 2

- [ ] T050 [P] [US2] Create PaymentFailureNotification DTO in src/main/java/org/gb/billing/dto/notification/PaymentFailureNotification.java

### Services for User Story 2

- [ ] T051 [US2] Implement WebhookService.handlePaymentFailure() in src/main/java/org/gb/billing/service/WebhookService.java
- [ ] T052 [US2] Implement EmailService.sendPaymentFailureEmail() with @Async in src/main/java/org/gb/billing/service/EmailService.java
- [ ] T053 [US2] Update SubscriptionService to handle PAST_DUE status transitions in src/main/java/org/gb/billing/service/SubscriptionService.java

### Integration and Validation for User Story 2

- [ ] T054 [US2] Add error handling for email service failures in WebhookService
- [ ] T055 [US2] Add logging for payment failure events
- [ ] T056 [US2] Verify all tests pass (mvn test)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - payment failures are handled gracefully

---

## Phase 5: User Story 3 - Recurring Payment Processing (Priority: P3)

**Goal**: Automatically process recurring payments at the end of each billing cycle through Stripe's subscription management features.

**Independent Test**: Create subscription with short billing cycle in sandbox, wait for renewal date, verify payment is processed automatically and subscription is extended.

### Tests for User Story 3

- [ ] T057 [P] [US3] Unit test for WebhookService.handleRecurringPayment() in src/test/java/org/gb/billing/service/WebhookServiceTest.java
- [ ] T058 [P] [US3] Integration test for POST /api/v1/webhooks/stripe with invoice.payment_succeeded event in src/test/java/org/gb/billing/controller/WebhookControllerTest.java
- [ ] T059 [P] [US3] Integration test for subscription renewal flow in src/test/java/org/gb/billing/integration/RecurringPaymentIntegrationTest.java

### Services for User Story 3

- [ ] T060 [US3] Implement WebhookService.handleRecurringPayment() in src/main/java/org/gb/billing/service/WebhookService.java
- [ ] T061 [US3] Implement SubscriptionService.extendSubscription() in src/main/java/org/gb/billing/service/SubscriptionService.java
- [ ] T062 [US3] Implement PaymentService.handleRecurringPaymentFailure() with retry logic in src/main/java/org/gb/billing/service/PaymentService.java

### Integration and Validation for User Story 3

- [ ] T063 [US3] Add webhook event type handling for invoice.payment_succeeded in WebhookService
- [ ] T064 [US3] Add webhook event type handling for invoice.payment_failed in WebhookService
- [ ] T065 [US3] Add logging for recurring payment events
- [ ] T066 [US3] Verify all tests pass (mvn test)

**Checkpoint**: All user stories should now be independently functional - recurring payments work automatically

---

## Phase 6: Payment History \u0026 Viewing (Additional Feature)

**Goal**: Allow users to view their payment transaction history with pagination and filtering.

**Independent Test**: Make several test payments, retrieve payment history via API, verify correct pagination and filtering.

### Tests for Payment History

- [ ] T067 [P] [US4] Integration test for GET /api/v1/payments/history in src/test/java/org/gb/billing/controller/PaymentControllerTest.java
- [ ] T068 [P] [US4] Integration test for GET /api/v1/payments/{transactionId} in src/test/java/org/gb/billing/controller/PaymentControllerTest.java

### DTOs for Payment History

- [ ] T069 [P] [US4] Create PaymentHistoryResponse DTO in src/main/java/org/gb/billing/dto/response/PaymentHistoryResponse.java
- [ ] T070 [P] [US4] Create PaymentTransactionSummary DTO in src/main/java/org/gb/billing/dto/response/PaymentTransactionSummary.java
- [ ] T071 [P] [US4] Create PaymentTransactionDetails DTO in src/main/java/org/gb/billing/dto/response/PaymentTransactionDetails.java

### Services for Payment History

- [ ] T072 [US4] Implement PaymentService.getPaymentHistory() with pagination in src/main/java/org/gb/billing/service/PaymentService.java
- [ ] T073 [US4] Implement PaymentService.getPaymentDetails() in src/main/java/org/gb/billing/service/PaymentService.java

### Controllers for Payment History

- [ ] T074 [US4] Implement GET /api/v1/payments/history in src/main/java/org/gb/billing/controller/PaymentController.java
- [ ] T075 [US4] Implement GET /api/v1/payments/{transactionId} in src/main/java/org/gb/billing/controller/PaymentController.java

### Integration and Validation for Payment History

- [ ] T076 [US4] Add Swagger/OpenAPI annotations to payment history endpoints
- [ ] T077 [US4] Add pagination validation and error handling
- [ ] T078 [US4] Verify all tests pass (mvn test)

**Checkpoint**: Payment history feature complete and independently testable

---

## Phase 7: Polish \u0026 Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T079 [P] Update README.md with payment integration setup instructions
- [ ] T080 [P] Update API_TESTING_GUIDE.md with payment endpoint examples
- [ ] T081 [P] Add Actuator custom metrics for payment success/failure rates in src/main/java/org/gb/billing/config/MetricsConfig.java
- [ ] T082 [P] Add global exception handling for payment exceptions in existing @ControllerAdvice
- [ ] T083 Code review and refactoring for code quality
- [ ] T084 Security audit for webhook signature verification and API key handling
- [ ] T085 Performance testing with JMeter for 1000 concurrent payment transactions
- [ ] T086 Run full test suite with coverage report (mvn clean verify jacoco:report)
- [ ] T087 Validate quickstart.md instructions by following setup steps
- [ ] T088 Update IMPLEMENTATION_SUMMARY.md with payment integration details

---

## Dependencies \u0026 Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4)
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Extends US1 webhook handling but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Extends US1/US2 webhook handling but independently testable
- **User Story 4 (Payment History)**: Can start after Foundational (Phase 2) - Uses PaymentTransaction entity from US1 but independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- DTOs before services
- Services before controllers
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- DTOs within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for PaymentService.initiatePayment() in src/test/java/org/gb/billing/service/PaymentServiceTest.java"
Task: "Unit test for WebhookService.processWebhook() in src/test/java/org/gb/billing/service/WebhookServiceTest.java"
Task: "Integration test for POST /api/v1/payments/initiate in src/test/java/org/gb/billing/controller/PaymentControllerTest.java"
Task: "Integration test for POST /api/v1/webhooks/stripe in src/test/java/org/gb/billing/controller/WebhookControllerTest.java"
Task: "Repository test for PaymentTransactionRepository in src/test/java/org/gb/billing/repository/PaymentTransactionRepositoryTest.java"
Task: "Repository test for WebhookEventRepository in src/test/java/org/gb/billing/repository/WebhookEventRepositoryTest.java"

# Launch all DTOs for User Story 1 together:
Task: "Create PaymentInitiationRequest DTO in src/main/java/org/gb/billing/dto/request/PaymentInitiationRequest.java"
Task: "Create PaymentResponse DTO in src/main/java/org/gb/billing/dto/response/PaymentResponse.java"
Task: "Create WebhookPayload DTO in src/main/java/org/gb/billing/dto/request/WebhookPayload.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (6 tasks)
2. Complete Phase 2: Foundational (17 tasks) - CRITICAL - blocks all stories
3. Complete Phase 3: User Story 1 (23 tasks)
4. **STOP and VALIDATE**: Test User Story 1 independently using quickstart.md
5. Deploy/demo if ready

**MVP Scope**: 46 tasks total for basic payment integration

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (23 tasks)
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! - 46 tasks total)
3. Add User Story 2 → Test independently → Deploy/Demo (56 tasks total)
4. Add User Story 3 → Test independently → Deploy/Demo (66 tasks total)
5. Add User Story 4 (Payment History) → Test independently → Deploy/Demo (78 tasks total)
6. Polish phase → Final production-ready release (88 tasks total)

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (23 tasks)
2. Once Foundational is done:
   - Developer A: User Story 1 (23 tasks)
   - Developer B: User Story 2 (10 tasks)
   - Developer C: User Story 3 (10 tasks)
   - Developer D: User Story 4 (12 tasks)
3. Stories complete and integrate independently
4. Team collaborates on Polish phase (10 tasks)

---

## Task Summary

**Total Tasks**: 88

**Task Count by Phase**:
- Phase 1 (Setup): 6 tasks
- Phase 2 (Foundational): 17 tasks
- Phase 3 (User Story 1 - P1 MVP): 23 tasks
- Phase 4 (User Story 2 - P2): 10 tasks
- Phase 5 (User Story 3 - P3): 10 tasks
- Phase 6 (Payment History): 12 tasks
- Phase 7 (Polish): 10 tasks

**Parallel Opportunities**:
- Setup: 5 tasks can run in parallel
- Foundational: 14 tasks can run in parallel
- User Story 1: 9 tasks can run in parallel (6 tests + 3 DTOs)
- User Story 2: 3 tasks can run in parallel (3 tests)
- User Story 3: 3 tasks can run in parallel (3 tests)
- User Story 4: 5 tasks can run in parallel (2 tests + 3 DTOs)
- Polish: 6 tasks can run in parallel

**Independent Test Criteria**:
- **US1**: Create test subscription → Complete payment in Stripe sandbox → Verify subscription status = "Active"
- **US2**: Use Stripe test card for failure → Verify subscription status = "Past Due" → Verify email sent
- **US3**: Create subscription with short billing cycle → Wait for renewal → Verify automatic payment and extension
- **US4**: Make several test payments → Call GET /api/v1/payments/history → Verify pagination and filtering

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1) = 46 tasks

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD approach per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Follow constitutional principles: layered architecture, security-first, transaction management
- Use Stripe test mode and test cards throughout development
- Refer to quickstart.md for detailed setup and testing instructions
