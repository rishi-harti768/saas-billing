# Tasks: Authentication & Multi-Tenancy Security

**Input**: Design documents from `/specs/001-auth-multi-tenancy/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-api.yaml

**Tests**: Test tasks included per Constitution VI (Test-First Development - NON-NEGOTIABLE). All tests MUST be written before implementation following TDD cycle.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- Single Spring Boot project structure
- Source: `src/main/java/com/company/billing/`
- Resources: `src/main/resources/`
- Tests: `src/test/java/com/company/billing/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic Spring Boot structure

- [x] T001 Verify Spring Boot project structure exists with Java 17+ and Spring Boot 3.x dependencies in pom.xml
- [x] T002 [P] Add Spring Security 6.x dependency to pom.xml
- [x] T003 [P] Add Spring Data JPA and PostgreSQL driver dependencies to pom.xml
- [x] T004 [P] Add JJWT library dependencies (jjwt-api, jjwt-impl, jjwt-jackson version 0.12.3) to pom.xml
- [x] T005 [P] Add Hibernate Validator dependency to pom.xml
- [x] T006 [P] Add Flyway migration dependency to pom.xml
- [x] T007 Configure application.yml with database connection settings, JWT properties (secret placeholder, 24-hour expiration), and logging configuration in src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T008 Create Flyway migration V001\_\_create_tenant_table.sql in src/main/resources/db/migration/ with tenants table (id, name UNIQUE, active, created_date, last_modified_date) and indexes
- [x] T009 Create Flyway migration V002\_\_create_user_table.sql in src/main/resources/db/migration/ with users table (id, email UNIQUE, password, role, tenant_id FK, first_name, last_name, active, created_date, last_modified_date, last_login_date, version) with CHECK constraints and indexes
- [x] T010 [P] Create Role enum in src/main/java/org/gb/billing/entity/Role.java with ROLE_ADMIN and ROLE_USER values
- [x] T011 [P] Create Tenant entity in src/main/java/org/gb/billing/entity/Tenant.java with JPA annotations, audit fields, and validation constraints
- [x] T012 [P] Create User entity in src/main/java/org/gb/billing/entity/User.java with JPA annotations, Hibernate filter definition, Many-to-One tenant relationship, audit fields, and optimistic locking
- [x] T013 [P] Create TenantRepository interface in src/main/java/org/gb/billing/repository/TenantRepository.java extending JpaRepository with findByName method
- [x] T014 [P] Create UserRepository interface in src/main/java/org/gb/billing/repository/UserRepository.java extending JpaRepository with findByEmail method
- [x] T015 [P] Create ErrorResponse DTO in src/main/java/org/gb/billing/dto/response/ErrorResponse.java with timestamp, status, error, message, path, and optional errors list fields
- [x] T016 [P] Create DuplicateEmailException in src/main/java/org/gb/billing/exception/DuplicateEmailException.java extending RuntimeException
- [x] T017 [P] Create InvalidCredentialsException in src/main/java/org/gb/billing/exception/InvalidCredentialsException.java extending RuntimeException
- [x] T018 [P] Create ExpiredTokenException in src/main/java/org/gb/billing/exception/ExpiredTokenException.java extending RuntimeException
- [x] T019 Create GlobalExceptionHandler in src/main/java/org/gb/billing/exception/GlobalExceptionHandler.java with @ControllerAdvice handling DuplicateEmailException (409), InvalidCredentialsException (401), ExpiredTokenException (401), MethodArgumentNotValidException (400), and generic exceptions (500)
- [x] T020 [P] Create JwtProperties configuration class in src/main/java/org/gb/billing/config/JwtProperties.java with @ConfigurationProperties binding jwt.secret and jwt.expiration from application.yml
- [x] T021 [P] Create TenantContext utility class in src/main/java/org/gb/billing/security/TenantContext.java with ThreadLocal storage for tenantId (setTenantId, getTenantId, clear methods)
- [x] T022 Create JwtTokenProvider in src/main/java/org/gb/billing/security/JwtTokenProvider.java with methods to generate JWT (with claims: sub, email, role, tenantId), validate JWT, extract claims, and handle expiration using JJWT library with HS256 algorithm
- [x] T023 Create CustomUserDetails in src/main/java/org/gb/billing/security/CustomUserDetails.java implementing UserDetails with additional tenantId field
- [x] T024 Create CustomUserDetailsService in src/main/java/org/gb/billing/security/CustomUserDetailsService.java implementing UserDetailsService to load user by email from UserRepository
- [x] T025 Create JwtAuthenticationFilter in src/main/java/org/gb/billing/security/JwtAuthenticationFilter.java extending OncePerRequestFilter to extract JWT from Authorization header, validate token, set SecurityContext authentication, and populate TenantContext with tenantId
- [x] T026 Create TenantFilterAspect in src/main/java/org/gb/billing/security/TenantFilterAspect.java with @Aspect to enable Hibernate tenantFilter before repository method execution using tenantId from TenantContext
- [x] T027 Create SecurityConfig in src/main/java/org/gb/billing/config/SecurityConfig.java with @EnableMethodSecurity, configure HttpSecurity to permit /api/v1/auth/\*\* endpoints, require authentication for others, disable CSRF (stateless), add JwtAuthenticationFilter, and define BCryptPasswordEncoder bean with work factor 12
- [x] T028 Create JwtSecretValidator in src/main/java/org/gb/billing/config/JwtSecretValidator.java implementing ApplicationListener to validate JWT secret is not default placeholder and is at least 32 characters on application startup

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - SaaS Owner Account Creation and Login (Priority: P1) 🎯 MVP

**Goal**: Enable SaaS platform owners to create admin accounts and securely log in to manage the billing system

**Independent Test**: Create an admin account through registration endpoint, log in with credentials, and receive a valid JWT token with ROLE_ADMIN that grants admin-level access

### Tests for User Story 1 (TDD - Write FIRST) ⚠️

> **CRITICAL**: These tests MUST be written FIRST and FAIL before implementing US1

- [x] T029 [P] [US1] Write unit tests for UserService.registerUser in src/test/java/org/gb/billing/service/UserServiceTest.java covering: successful admin registration, duplicate email throws DuplicateEmailException, password is BCrypt-hashed, ROLE_ADMIN validation (no tenantName required)
- [x] T030 [P] [US1] Write unit tests for AuthenticationService.login in src/test/java/org/gb/billing/service/AuthenticationServiceTest.java covering: successful login returns JWT, invalid email throws InvalidCredentialsException, wrong password throws InvalidCredentialsException, inactive account throws exception, lastLoginDate updated
- [x] T031 [P] [US1] Write unit tests for JwtTokenProvider in src/test/java/org/gb/billing/security/JwtTokenProviderTest.java covering: token generation with correct claims (sub, email, role), token validation accepts valid tokens, token validation rejects expired tokens, token validation rejects malformed tokens, claim extraction
- [x] T032 [P] [US1] Write integration tests for AuthController endpoints in src/test/java/org/gb/billing/controller/AuthControllerTest.java using MockMvc and @SpringBootTest covering: POST /register (201 success, 409 duplicate, 400 validation), POST /login (200 success, 401 invalid, 400 validation)
- [x] T033 [P] [US1] Write repository tests for UserRepository in src/test/java/org/gb/billing/repository/UserRepositoryTest.java using @DataJpaTest covering: findByEmail returns user when exists, findByEmail returns empty when not found, save enforces unique email constraint, optimistic locking increments version on update

### Implementation for User Story 1

- [x] T034 [P] [US1] Create RegisterRequest DTO in src/main/java/org/gb/billing/dto/request/RegisterRequest.java with validation annotations (@NotBlank, @Email, @Size for password 8-100 chars) for email, password, role, tenantName (optional), firstName (optional), lastName (optional)
- [x] T035 [P] [US1] Create LoginRequest DTO in src/main/java/org/gb/billing/dto/request/LoginRequest.java with validation annotations (@NotBlank, @Email) for email and password
- [x] T036 [P] [US1] Create AuthResponse DTO in src/main/java/org/gb/billing/dto/response/AuthResponse.java with token, expiresIn, role, tenantId (nullable), and email fields
- [x] T037 [US1] Create UserService in src/main/java/org/gb/billing/service/UserService.java with @Transactional registerUser method that validates ROLE_ADMIN has no tenantName, hashes password with BCrypt, creates User entity, saves to UserRepository, catches DataIntegrityViolationException and throws DuplicateEmailException
- [x] T038 [US1] Create AuthenticationService in src/main/java/org/gb/billing/service/AuthenticationService.java with login method that finds user by email, validates active status, verifies password with BCrypt, updates lastLoginDate, generates JWT token with JwtTokenProvider, and returns AuthResponse
- [x] T039 [US1] Create AuthController in src/main/java/org/gb/billing/controller/AuthController.java with POST /api/v1/auth/register endpoint (returns 201) calling UserService.registerUser and POST /api/v1/auth/login endpoint (returns 200) calling AuthenticationService.login, both with @Valid request body validation
- [x] T040 [US1] Add SLF4J logging to UserService for registration events (success, duplicate email) without logging sensitive data (passwords, tokens)
- [x] T041 [US1] Add SLF4J logging to AuthenticationService for login events (success, invalid credentials, account disabled) without logging passwords

**Checkpoint**: At this point, User Story 1 should be fully functional - admin users can register and login with JWT tokens

---

## Phase 4: User Story 2 - Subscriber Account Management (Priority: P1)

**Goal**: Enable subscribers (customers) to create accounts with tenant association and log in to access billing services with proper data isolation

**Independent Test**: Create a subscriber account with tenant information, log in, and verify the returned JWT token contains ROLE_USER and correct tenantId for data isolation

### Tests for User Story 2 (TDD - Write FIRST) ⚠️

> **CRITICAL**: These tests MUST be written FIRST and FAIL before implementing US2

- [x] T042 [P] [US2] Write unit tests for UserService.registerUser ROLE_USER path in src/test/java/org/gb/billing/service/UserServiceTest.java covering: successful subscriber registration with new tenant, registration with existing tenant, validation error when tenantName missing for ROLE_USER, tenant association in User entity
- [x] T043 [P] [US2] Write unit tests for AuthenticationService.login tenant handling in src/test/java/org/gb/billing/service/AuthenticationServiceTest.java covering: JWT token includes tenantId for ROLE_USER, JWT token has null tenantId for ROLE_ADMIN, AuthResponse includes correct tenantId
- [x] T044 [P] [US2] Write integration tests for tenant isolation in src/test/java/org/gb/billing/security/TenantFilterAspectTest.java using @DataJpaTest covering: Hibernate filter applied when tenantId in context, cross-tenant queries blocked, admin users bypass filter
- [x] T045 [P] [US2] Write repository tests for TenantRepository in src/test/java/org/gb/billing/repository/TenantRepositoryTest.java using @DataJpaTest covering: findByName returns tenant when exists, save with unique constraint on tenant name, tenant creation with audit fields

### Implementation for User Story 2

- [x] T046 [US2] Update UserService.registerUser in src/main/java/org/gb/billing/service/UserService.java to handle ROLE_USER registration by validating tenantName is provided, finding or creating Tenant via TenantRepository, associating User with Tenant, and saving both entities in a single transaction
- [x] T047 [US2] Update AuthenticationService.login in src/main/java/org/gb/billing/service/AuthenticationService.java to include tenantId in JWT token claims for ROLE_USER (null for ROLE_ADMIN) and populate AuthResponse.tenantId field
- [x] T048 [US2] Update JwtAuthenticationFilter in src/main/java/org/gb/billing/security/JwtAuthenticationFilter.java to extract tenantId from JWT token and call TenantContext.setTenantId for ROLE_USER requests
- [x] T049 [US2] Test TenantFilterAspect integration by running T044 tests and manually verifying tenant isolation works correctly in integration environment
- [x] T050 [US2] Add validation to RegisterRequest processing in UserService to return clear error message when ROLE_USER is provided without tenantName
- [x] T051 [US2] Add logging to UserService for tenant creation and association during ROLE_USER registration

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - both admin and subscriber accounts can register and login with proper tenant isolation

---

## Phase 5: User Story 3 - Secure Password Management (Priority: P2)

**Goal**: Ensure passwords are securely stored using BCrypt and validated correctly to protect user accounts from unauthorized access

**Independent Test**: Register a user, verify password is not stored in plain text in database, and confirm login only succeeds with exact password provided during registration

### Tests for User Story 3 (TDD - Write FIRST) ⚠️

> **CRITICAL**: These tests MUST verify password security before implementing US3

- [x] T052 [P] [US3] Write security tests for BCrypt password handling in src/test/java/org/gb/billing/security/PasswordSecurityTest.java covering: password never stored in plain text (verify BCrypt hash format), BCrypt work factor is 12, password verification succeeds with correct password, password verification fails with incorrect password, timing attack resistance
- [x] T053 [P] [US3] Write integration tests for password validation in src/test/java/org/gb/billing/controller/AuthControllerTest.java covering: registration rejects passwords <8 chars, login error messages don't reveal whether email or password was wrong, password validation errors don't expose password values

### Implementation for User Story 3

- [x] T054 [US3] Verify BCryptPasswordEncoder bean in SecurityConfig uses work factor 12 as specified in research.md
- [x] T055 [US3] Verify UserService.registerUser in src/main/java/org/gb/billing/service/UserService.java uses PasswordEncoder.encode to hash password before saving User entity
- [x] T056 [US3] Verify AuthenticationService.login in src/main/java/org/gb/billing/service/AuthenticationService.java uses PasswordEncoder.matches to validate password against BCrypt hash
- [x] T057 [US3] Update AuthenticationService.login to throw InvalidCredentialsException with generic message "Invalid credentials" (not revealing whether email or password was incorrect) when password verification fails
- [x] T058 [US3] Add validation to RegisterRequest in src/main/java/org/gb/billing/dto/request/RegisterRequest.java to enforce minimum password length of 8 characters using @Size annotation
- [x] T059 [US3] Update GlobalExceptionHandler to ensure password validation errors return user-friendly messages without exposing password values in error responses

**Checkpoint**: All password operations now use secure BCrypt hashing with proper validation and error handling

---

## Phase 6: User Story 4 - Input Validation and Error Handling (Priority: P2)

**Goal**: Provide clear, immediate feedback when users provide invalid input during registration or login to ensure data quality and user experience

**Independent Test**: Submit various invalid inputs (malformed emails, blank fields, short passwords) and verify appropriate error messages are returned with 400 status code

### Tests for User Story 4 (TDD - Write FIRST) ⚠️

> **CRITICAL**: These tests MUST verify validation before implementing US4

- [x] T060 [P] [US4] Write integration tests for input validation in src/test/java/org/gb/billing/controller/AuthControllerTest.java covering: malformed email returns 400 with clear message, blank email returns 400, blank password returns 400, password <8 chars returns 400, missing required fields return 400 with field-level errors
- [x] T061 [P] [US4] Write unit tests for GlobalExceptionHandler in src/test/java/org/gb/billing/exception/GlobalExceptionHandlerTest.java covering: MethodArgumentNotValidException returns ErrorResponse with 400, DuplicateEmailException returns 409, InvalidCredentialsException returns 401, consistent ErrorResponse format (timestamp, status, error, message, path)

### Implementation for User Story 4

- [x] T062 [US4] Verify RegisterRequest DTO in src/main/java/org/gb/billing/dto/request/RegisterRequest.java has @NotBlank on email and password, @Email on email, @Size(min=8, max=100) on password
- [x] T063 [US4] Verify LoginRequest DTO in src/main/java/org/gb/billing/dto/request/LoginRequest.java has @NotBlank on email and password, @Email on email
- [x] T064 [US4] Update GlobalExceptionHandler in src/main/java/org/gb/billing/exception/GlobalExceptionHandler.java to handle MethodArgumentNotValidException and return ErrorResponse with 400 status, including detailed field-level validation errors in errors array
- [x] T065 [US4] Verify AuthController in src/main/java/org/gb/billing/controller/AuthController.java uses @Valid annotation on RegisterRequest and LoginRequest parameters to trigger validation
- [x] T066 [US4] Add custom validation messages to RegisterRequest and LoginRequest DTO annotations for better user experience (e.g., "Email is required", "Invalid email format", "Password must be between 8 and 100 characters")
- [x] T067 [US4] Update GlobalExceptionHandler to ensure all error responses follow consistent ErrorResponse format with timestamp, status, error, message, and path fields
- [x] T068 [US4] Add logging to GlobalExceptionHandler for validation errors (log field errors for debugging without exposing sensitive data)

**Checkpoint**: All user stories should now have comprehensive input validation with clear, user-friendly error messages

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and production readiness

- [x] T056 [P] Add OpenAPI/Swagger annotations to AuthController in src/main/java/org/gb/billing/controller/AuthController.java to match contracts/auth-api.yaml specification
- [x] T057 [P] Configure Springdoc OpenAPI dependency in pom.xml and application.yml to serve API documentation at /swagger-ui.html
- [x] T058 [P] Add Spring Boot Actuator dependency to pom.xml and configure health check endpoints in application.yml
- [x] T059 [P] Create application-dev.yml in src/main/resources/ with development-specific settings (H2 database for local testing, verbose logging)
- [x] T060 [P] Create application-prod.yml in src/main/resources/ with production-specific settings (PostgreSQL, INFO logging, JWT secret validation)
- [x] T061 Add CORS configuration to SecurityConfig in src/main/java/org/gb/billing/config/SecurityConfig.java to allow frontend integration (configurable allowed origins)
- [x] T062 Add database indexes verification by reviewing Flyway migrations to ensure idx_users_email, idx_users_tenant_id, idx_users_role, idx_users_active, idx_tenants_name, idx_tenants_active are created
- [x] T063 Add README.md section documenting JWT secret generation using openssl rand -base64 32 and environment variable configuration
- [x] T064 Verify quickstart.md in specs/001-auth-multi-tenancy/quickstart.md is accurate and can be followed to run the application locally
- [x] T065 Add .gitignore entry to ensure application-local.yml (for local JWT secrets) is never committed to source control
- [x] T066 Code review and refactoring pass for consistent code style, proper exception handling, and removal of any TODO comments
- [x] T067 Security audit to verify no passwords or JWT tokens are logged, all endpoints have proper authorization, and sensitive configuration uses environment variables
- [x] T068 Run test coverage report and verify minimum 80% coverage for service and controller layers per Constitution VI requirement using JaCoCo or similar tool
- [x] T069 [P] Add Testcontainers PostgreSQL configuration in src/test/resources/application-test.yml for integration tests to use real database instead of H2
- [x] T070 [P] Create load test script using JMeter or Gatling to verify SC-009 (100 concurrent login requests with p95 <2s) and document results in specs/001-auth-multi-tenancy/performance-results.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase completion
- **User Story 2 (Phase 4)**: Depends on Foundational phase completion AND User Story 1 (extends registration/login logic)
- **User Story 3 (Phase 5)**: Depends on User Story 1 and 2 (validates existing password handling)
- **User Story 4 (Phase 6)**: Depends on User Story 1 and 2 (validates existing input handling)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Foundation for all authentication - MUST be completed first
- **User Story 2 (P1)**: Extends User Story 1 with tenant support - depends on US1 implementation
- **User Story 3 (P2)**: Validates and enhances password security from US1 and US2
- **User Story 4 (P2)**: Validates and enhances input handling from US1 and US2

### Within Each User Story

- DTOs before Services (services use DTOs)
- Services before Controllers (controllers call services)
- Core implementation before logging/validation enhancements
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1 (Setup)**: Tasks T002-T006 can run in parallel (different dependencies in pom.xml)
- **Phase 2 (Foundational)**:
  - T010-T012 (entities) can run in parallel
  - T013-T014 (repositories) can run in parallel after entities
  - T015-T018 (exceptions and DTOs) can run in parallel
  - T020-T021 (config classes) can run in parallel
- **Phase 3 (US1)**:
  - T029-T033 (all test tasks) can run in parallel - **WRITE THESE FIRST**
  - T034-T036 (DTOs) can run in parallel after tests written
- **Phase 4 (US2)**:
  - T042-T045 (test tasks) can run in parallel - **WRITE THESE FIRST**
- **Phase 5 (US3)**:
  - T052-T053 (test tasks) can run in parallel - **WRITE THESE FIRST**
- **Phase 6 (US4)**:
  - T060-T061 (test tasks) can run in parallel - **WRITE THESE FIRST**
- **Phase 7 (Polish)**: T056-T060, T069-T070 (documentation, config, and performance testing) can run in parallel

---

## Parallel Example: Foundational Phase

```bash
# Launch all entity classes together:
Task T010: "Create Role enum in src/main/java/com/company/billing/entity/Role.java"
Task T011: "Create Tenant entity in src/main/java/com/company/billing/entity/Tenant.java"
Task T012: "Create User entity in src/main/java/com/company/billing/entity/User.java"

# After entities complete, launch repositories together:
Task T013: "Create TenantRepository in src/main/java/com/company/billing/repository/TenantRepository.java"
Task T014: "Create UserRepository in src/main/java/com/company/billing/repository/UserRepository.java"

# Launch exception classes together:
Task T015: "Create ErrorResponse DTO"
Task T016: "Create DuplicateEmailException"
Task T017: "Create InvalidCredentialsException"
Task T018: "Create ExpiredTokenException"
```

---

## Implementation Strategy

### MVP First (User Stories 1 & 2 Only)

Both User Story 1 and User Story 2 are marked as P1 (highest priority) because they represent the core authentication functionality:

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Admin authentication)
4. Complete Phase 4: User Story 2 (Subscriber authentication with multi-tenancy)
5. **STOP and VALIDATE**: Test both admin and subscriber flows independently
6. Deploy/demo if ready

### Full Feature Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test admin authentication → Checkpoint
3. Add User Story 2 → Test subscriber authentication and tenant isolation → Checkpoint (MVP!)
4. Add User Story 3 → Validate password security → Checkpoint
5. Add User Story 4 → Validate input handling → Checkpoint
6. Add Polish phase → Production-ready feature

### Incremental Testing

After each user story phase:

- **US1**: Test admin registration and login, verify JWT contains ROLE_ADMIN
- **US2**: Test subscriber registration with tenant, verify JWT contains ROLE_USER and tenantId, verify tenant isolation
- **US3**: Verify passwords are BCrypt-hashed in database, test password validation
- **US4**: Test various invalid inputs, verify error messages are clear and consistent

---

## Notes

- **TDD Workflow**: All test tasks MUST be written FIRST and FAIL before implementing corresponding features
- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label maps task to specific user story (US1, US2, US3, US4) for traceability
- Each user story builds upon previous stories (US2 extends US1, US3/US4 validate US1/US2)
- User Stories 1 and 2 together form the MVP (both are P1 priority)
- User Stories 3 and 4 are P2 enhancements for security and UX
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All file paths are absolute from repository root
- Follow Spring Boot layered architecture: controller → service → repository → entity
- Never log passwords, JWT tokens, or other sensitive data
- Use environment variables for JWT secret in production
- Target: 80% minimum test coverage per Constitution VI requirement
