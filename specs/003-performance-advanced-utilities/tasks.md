# Tasks: Performance & Advanced Utilities

**Input**: Design documents from `/specs/003-performance-advanced-utilities/`  
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are included per specification requirements for verification of rate limiting, caching, and exception handling.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/java/org/gb/billing/`, `src/test/java/org/gb/billing/` at repository root
- Paths follow Spring Boot layered architecture per constitution

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency setup

- [x] T001 Add Bucket4j dependencies to pom.xml (bucket4j-core 8.7.0, bucket4j-caffeine 8.7.0)
- [x] T002 [P] Update Maven dependencies and verify build succeeds with ./mvnw clean compile

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Create RateLimitConfig.java in src/main/java/org/gb/billing/config/ (Bucket4j ProxyManager bean)
- [x] T004 [P] Create RateLimitExceededException.java in src/main/java/org/gb/billing/exception/ (custom exception with retryAfterSeconds field)
- [x] T005 [P] Update ErrorResponse.java in src/main/java/org/gb/billing/dto/response/ (add errorCode, timestamp, errorId, details fields)
- [x] T006 Update CacheConfig.java in src/main/java/org/gb/billing/config/ (change plans cache TTL from 10 minutes to 1 hour)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Plan-Based API Access Control (Priority: P1) 🎯 MVP

**Goal**: Enforce tiered API rate limiting based on subscription plans (Free: 10 req/min, Pro: 100 req/min, Enterprise: 1000 req/min)

**Independent Test**: Make 11 API requests from a Free plan user account - first 10 succeed with HTTP 200, 11th returns HTTP 429 with Retry-After header and rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset)

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T007 [P] [US1] Create RateLimitingFilterTest.java in src/test/java/org/gb/billing/security/ (unit tests for rate limit enforcement)
- [x] T008 [P] [US1] Create RateLimitIntegrationTest.java in src/test/java/org/gb/billing/integration/ (end-to-end rate limiting tests with MockMvc)

### Implementation for User Story 1

- [x] T009 [US1] Create RateLimitingFilter.java in src/main/java/org/gb/billing/security/ (OncePerRequestFilter with Bucket4j integration, plan-based limits, rate limit headers)
- [x] T010 [US1] Update SecurityConfig.java in src/main/java/org/gb/billing/config/ (register RateLimitingFilter before JwtAuthenticationFilter)
- [x] T011 [US1] Update GlobalExceptionHandler.java in src/main/java/org/gb/billing/exception/ (add @ExceptionHandler for RateLimitExceededException returning HTTP 429 with Retry-After header)
- [x] T012 [US1] Run RateLimitingFilterTest and verify all tests pass (./mvnw test -Dtest=RateLimitingFilterTest)
- [x] T013 [US1] Run RateLimitIntegrationTest and verify all tests pass (./mvnw test -Dtest=RateLimitIntegrationTest)

**Checkpoint**: At this point, User Story 1 should be fully functional - rate limiting enforces plan-based limits, returns HTTP 429 when exceeded, includes rate limit headers in all responses

---

## Phase 4: User Story 2 - Fast and Consistent Plan Browsing (Priority: P2)

**Goal**: Implement 1-hour caching for billing plans with automatic cache invalidation on plan updates to achieve <50ms response times for cached requests

**Independent Test**: Call GET /api/v1/plans twice - first request takes ~100ms (database query), second request takes <10ms (from cache). Update a plan via admin endpoint, then call GET /api/v1/plans again - should take ~100ms (cache evicted, fresh data loaded)

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T014 [P] [US2] Update PlanServiceTest.java in src/test/java/org/gb/billing/service/ (add cache hit/miss tests, cache eviction tests, cache stampede prevention tests)

### Implementation for User Story 2

- [x] T015 [US2] Update PlanService.java in src/main/java/org/gb/billing/service/ (add sync=true to @Cacheable on getAllActivePlans, add @CacheEvict to createPlan/updatePlan/deletePlan methods)
- [x] T016 [US2] Run PlanServiceTest and verify all caching tests pass (./mvnw test -Dtest=PlanServiceTest)
- [x] T017 [US2] Manual verification: Start application, measure GET /api/v1/plans response time twice (should be fast on second call), update a plan, measure again (should be slow after cache eviction)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - rate limiting enforces limits, caching improves plan browsing performance

---

## Phase 5: User Story 3 - Clear Error Communication (Priority: P3)

**Goal**: Standardize all error responses with error codes, timestamps, and unique error IDs for 500 errors to improve API usability and reduce support burden

**Independent Test**: Trigger various error conditions (404 not found, 400 validation error, 500 server error) and verify all return standardized JSON with errorCode, timestamp, path fields. For 500 errors, verify errorId is present.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T018 [P] [US3] Update GlobalExceptionHandlerTest.java in src/test/java/org/gb/billing/exception/ (add tests for errorCode field, timestamp field, errorId for 500 errors, validation error details with examples)

### Implementation for User Story 3

- [x] T019 [P] [US3] Update ErrorResponse.java constructor and add getters/setters for new fields (errorCode, timestamp, errorId, details)
- [x] T020 [US3] Update GlobalExceptionHandler.java in src/main/java/org/gb/billing/exception/ (update all 10 existing @ExceptionHandler methods to include errorCode and timestamp, update 500 handler to generate and include errorId)
- [x] T021 [US3] Update FieldError.java in ErrorResponse to add example field for validation errors
- [x] T022 [US3] Run GlobalExceptionHandlerTest and verify all tests pass (./mvnw test -Dtest=GlobalExceptionHandlerTest)
- [x] T023 [US3] Manual verification: Trigger 404 error (invalid subscription ID), 400 error (invalid email), 500 error (cause unexpected exception) and verify error response format

**Checkpoint**: All user stories should now be independently functional - rate limiting enforces limits, caching improves performance, error responses are standardized

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T024 [P] Update OpenAPI documentation in contracts/error-handling-api.yaml to reflect new error response fields
- [x] T025 [P] Update quickstart.md with examples of rate limit headers and error response formats
- [x] T026 Run full test suite and verify all tests pass (./mvnw clean test)
- [x] T027 Verify code coverage >80% for new classes (RateLimitingFilter, RateLimitConfig)
- [x] T028 Run application locally and execute manual verification steps from quickstart.md (rate limiting, caching, error handling)
- [x] T029 [P] Monitor cache hit rate via Actuator metrics (curl http://localhost:8080/actuator/metrics/cache.gets?tag=cache:plans)
- [x] T030 Code review: Verify all changes follow constitution principles (layered architecture, security-first, exception handling)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent of US1 (different components)
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of US1/US2 (different components)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Core implementation before integration
- Unit tests before integration tests
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel (T001, T002)
- All Foundational tasks marked [P] can run in parallel (T003, T004, T005, T006)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Create RateLimitingFilterTest.java in src/test/java/org/gb/billing/security/"
Task: "Create RateLimitIntegrationTest.java in src/test/java/org/gb/billing/integration/"

# These tests can be written in parallel (different files, no dependencies)
```

---

## Parallel Example: Foundational Phase

```bash
# Launch all foundational tasks together (after Phase 1 completes):
Task: "Create RateLimitConfig.java in src/main/java/org/gb/billing/config/"
Task: "Create RateLimitExceededException.java in src/main/java/org/gb/billing/exception/"
Task: "Update ErrorResponse.java in src/main/java/org/gb/billing/dto/response/"
Task: "Update CacheConfig.java in src/main/java/org/gb/billing/config/"

# All different files, can be done in parallel
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T006) - CRITICAL - blocks all stories
3. Complete Phase 3: User Story 1 (T007-T013)
4. **STOP and VALIDATE**: Test User Story 1 independently
   - Make 11 API requests from Free plan user
   - Verify first 10 succeed, 11th returns 429
   - Verify rate limit headers present
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! Rate limiting works)
3. Add User Story 2 → Test independently → Deploy/Demo (Caching improves performance)
4. Add User Story 3 → Test independently → Deploy/Demo (Error responses standardized)
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T006)
2. Once Foundational is done:
   - Developer A: User Story 1 (T007-T013) - Rate limiting
   - Developer B: User Story 2 (T014-T017) - Caching
   - Developer C: User Story 3 (T018-T023) - Exception handling
3. Stories complete and integrate independently

---

## Verification Checklist

### User Story 1 Verification (Rate Limiting)

- [ ] Free plan user blocked after 10 requests/minute with HTTP 429
- [ ] Pro plan user blocked after 100 requests/minute with HTTP 429
- [ ] Enterprise plan user blocked after 1000 requests/minute with HTTP 429
- [ ] All responses include X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset headers
- [ ] HTTP 429 responses include Retry-After header
- [ ] Rate limits reset after 1 minute time window
- [ ] /auth/** endpoints bypass rate limiting
- [ ] Unauthenticated requests return HTTP 401 before rate limiting
- [ ] User plan upgrade immediately enforces new limits (FR-008: upgrade Free→Pro, verify 100 req/min limit applies on next request)

### User Story 2 Verification (Caching)

- [ ] First GET /api/v1/plans request takes ~100ms (cache miss)
- [ ] Second GET /api/v1/plans request takes <10ms (cache hit)
- [x] Phase 4: Performance & Advanced Utilities
    - [x] Standardize Exception Handling
    - [x] Optimize Caching Strategy
    - [x] Data Model Alignment (Long IDs)
    - [x] Subscription Service Caching
    - [x] Rate Limiting Optimization
    - [/] Verification & Testing (Resolving mocking issues on JDK 24)t requests don't cause cache stampede (FR-014: send 10 simultaneous requests on empty cache, verify only 1 DB query via logs)
- [ ] Cache statistics are logged (FR-016: verify Actuator metrics show cache hits/misses at /actuator/metrics/cache.gets)

### User Story 3 Verification (Exception Handling)

- [ ] All error responses include errorCode field
- [ ] All error responses include timestamp field (ISO 8601 format)
- [ ] All error responses include path field
- [ ] HTTP 500 errors include unique errorId (UUID)
- [ ] HTTP 400 validation errors include errors array with field details
- [ ] Validation errors include example field showing valid format
- [ ] Error codes match specification (SUBSCRIPTION_NOT_FOUND, RATE_LIMIT_EXCEEDED, etc.)
- [ ] Error messages are user-friendly (no stack traces exposed)

### Overall Verification

- [ ] All tests pass (./mvnw clean test)
- [ ] Code coverage >80% for new classes
- [ ] No breaking changes to existing APIs
- [ ] Constitution compliance verified (layered architecture, security-first, etc.)
- [ ] Performance targets met (<1ms rate limit overhead, <5ms cache hit latency)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

---

## Task Summary

**Total Tasks**: 30 tasks
- Phase 1 (Setup): 2 tasks
- Phase 2 (Foundational): 4 tasks
- Phase 3 (User Story 1 - Rate Limiting): 7 tasks
- Phase 4 (User Story 2 - Caching): 4 tasks
- Phase 5 (User Story 3 - Exception Handling): 6 tasks
- Phase 6 (Polish): 7 tasks

**Parallel Opportunities**: 15 tasks marked [P] can run in parallel within their phases

**Independent Test Criteria**:
- US1: Make 11 API requests, verify 10 succeed and 11th returns 429 with headers
- US2: Measure response times, verify cache hit <10ms, verify cache eviction on update
- US3: Trigger errors, verify standardized response format with errorCode/timestamp/errorId

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1 only) = 13 tasks
- Delivers core value: Rate limiting enforces plan-based API limits
- Independently testable and deployable
- Can add US2 and US3 incrementally after MVP validation
