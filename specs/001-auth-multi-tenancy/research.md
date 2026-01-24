# Research: Authentication & Multi-Tenancy Security

**Feature**: 001-auth-multi-tenancy  
**Date**: 2026-01-24  
**Purpose**: Resolve technical unknowns and validate technology choices for JWT authentication and multi-tenant security

## Research Tasks

### 1. JWT Library Selection for Spring Boot

**Question**: Which JWT library should be used for token generation and validation in Spring Boot 3.x?

**Decision**: Use `io.jsonwebtoken:jjwt-api:0.12.x` (JJWT library)

**Rationale**:

- **Industry Standard**: JJWT is the most widely adopted JWT library in the Spring ecosystem
- **Spring Boot 3 Compatible**: Fully supports Jakarta EE namespace (javax → jakarta migration)
- **Type Safety**: Provides fluent API with compile-time type checking
- **Security**: Actively maintained with regular security updates
- **Feature Complete**: Supports all JWT algorithms (HS256, RS256, etc.), claims validation, expiration
- **Documentation**: Excellent documentation and Spring Boot integration examples

**Alternatives Considered**:

- **Auth0 java-jwt**: Good library but less Spring-specific integration
- **Nimbus JOSE+JWT**: More complex, overkill for basic JWT needs
- **Spring Security OAuth2 Resource Server**: Too heavyweight for simple JWT, better for full OAuth2 flows

**Implementation Details**:

```xml
<!-- Maven Dependencies -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

**Key Configuration Parameters**:

- **Algorithm**: HS256 (HMAC with SHA-256) for symmetric signing
- **Secret Key**: Minimum 256-bit (32 bytes) random key stored in environment variable
- **Token Expiration**: 24 hours for access tokens (configurable via application.yml)
- **Claims**: sub (user ID), email, role, tenantId (for ROLE_USER), iat, exp

---

### 2. BCrypt Work Factor Configuration

**Question**: What BCrypt work factor (cost) should be used for password hashing to balance security and performance?

**Decision**: Use BCrypt work factor of **12**

**Rationale**:

- **Security Standard**: OWASP recommends work factor of 10-12 for 2024+
- **Performance**: Work factor 12 takes ~250-350ms on modern hardware, acceptable for login operations
- **Future-Proof**: Computational cost doubles with each increment, providing runway for future increases
- **Spring Security Default**: Spring Security 6.x uses 10 by default, but 12 is recommended for financial applications
- **Attack Resistance**: Makes brute-force attacks computationally infeasible (2^12 = 4,096 iterations)

**Alternatives Considered**:

- **Work Factor 10**: Faster (~60-100ms) but less secure, not recommended for financial data
- **Work Factor 14**: More secure (~1-1.5s) but too slow for user-facing login operations
- **Argon2**: Modern alternative, but BCrypt is more established in Spring ecosystem

**Implementation**:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // Work factor 12
}
```

**Performance Impact**:

- Registration: ~300ms for password hashing (one-time cost, acceptable)
- Login: ~300ms for password verification (within 2-second SLA)
- Concurrent logins: CPU-bound, but 100 concurrent requests still manageable on modern servers

---

### 3. Multi-Tenant Data Isolation Strategy

**Question**: How should tenant isolation be enforced at the data access layer to prevent cross-tenant data leaks?

**Decision**: Use **Hibernate Filter + Custom Aspect** approach

**Rationale**:

- **Automatic Enforcement**: Hibernate filters automatically add WHERE tenantId = ? to all queries
- **Defense in Depth**: Aspect-oriented programming (AOP) validates tenantId is set before any repository call
- **Performance**: Filters are applied at SQL generation time, no runtime overhead
- **Maintainability**: Centralized tenant filtering logic, no need to add WHERE clause to every query
- **Spring Security Integration**: TenantId extracted from JWT and stored in SecurityContext

**Alternatives Considered**:

- **Manual WHERE Clauses**: Error-prone, easy to forget, not recommended
- **Separate Databases per Tenant**: Over-engineered for this scale, increases operational complexity
- **Row-Level Security (PostgreSQL)**: Database-specific, harder to test, less portable

**Implementation Strategy**:

1. **Hibernate Filter Definition**:

```java
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User {
    // ... entity fields
}
```

2. **Tenant Context Holder** (ThreadLocal storage):

```java
public class TenantContext {
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        currentTenant.set(tenantId);
    }

    public static Long getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
```

3. **JWT Filter Integration**:

```java
// In JwtAuthenticationFilter, after validating token:
Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);
if (tenantId != null) {
    TenantContext.setTenantId(tenantId);
}
```

4. **Hibernate Filter Activation** (via AOP or EntityManager listener):

```java
@Aspect
@Component
public class TenantFilterAspect {
    @Before("execution(* com.company.billing.repository.*.*(..))")
    public void enableTenantFilter(JoinPoint joinPoint) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
        }
    }
}
```

**Security Validation**:

- Unit tests verify filter is applied to all queries
- Integration tests verify cross-tenant data access is blocked
- Security audit logs all tenant context changes

---

### 4. JWT Token Expiration and Refresh Strategy

**Question**: Should we implement refresh tokens in Phase 1, or use short-lived access tokens only?

**Decision**: **Short-lived access tokens only** (24-hour expiration) for Phase 1

**Rationale**:

- **Simplicity**: Refresh tokens add significant complexity (storage, rotation, revocation)
- **Stateless Goal**: Refresh tokens require server-side storage, violating stateless principle
- **Phase 1 Scope**: MVP focuses on core authentication, refresh tokens are enhancement
- **User Experience**: 24-hour expiration balances security and UX (users re-login daily)
- **Future Extension**: Architecture supports adding refresh tokens in Phase 2 without breaking changes

**Alternatives Considered**:

- **Refresh Tokens (Phase 1)**: More secure but adds complexity, deferred to future phase
- **Long-lived Tokens (7+ days)**: Security risk if token is compromised, not recommended
- **Short-lived Tokens (1 hour)**: More secure but poor UX, requires refresh token implementation

**Implementation**:

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:changeme-use-env-variable}
  expiration: 86400000 # 24 hours in milliseconds
```

**Future Enhancement Path** (Phase 2+):

- Add refresh token endpoint: POST /api/v1/auth/refresh
- Store refresh tokens in Redis with TTL (7-30 days)
- Implement token rotation (new refresh token on each use)
- Add revocation list for compromised tokens

---

### 5. Email Uniqueness and Concurrent Registration Handling

**Question**: How should we handle race conditions when two users try to register with the same email simultaneously?

**Decision**: Use **Database UNIQUE Constraint + Optimistic Locking**

**Rationale**:

- **Database Enforcement**: UNIQUE constraint on User.email ensures atomicity at DB level
- **Race Condition Safety**: Database handles concurrent inserts correctly (one succeeds, one fails)
- **Error Handling**: Catch DataIntegrityViolationException and return user-friendly error
- **No Distributed Locks**: Avoids complexity of distributed locking (Redis, etc.)
- **Performance**: No additional overhead, database handles concurrency efficiently

**Alternatives Considered**:

- **Application-Level Locking**: Requires distributed lock (Redis), over-engineered
- **SELECT-then-INSERT**: Race condition window, not safe
- **Pessimistic Locking**: Unnecessary overhead for this use case

**Implementation**:

```java
// Entity
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email")
})
public class User {
    @Column(nullable = false, unique = true)
    private String email;
}

// Service
@Transactional
public User register(RegisterRequest request) {
    try {
        User user = new User();
        user.setEmail(request.getEmail());
        // ... set other fields
        return userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
        throw new DuplicateEmailException("Email already registered: " + request.getEmail());
    }
}
```

**Database Migration**:

```sql
-- V002__create_user_table.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    tenant_id BIGINT REFERENCES tenants(id),
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email); -- Implicit via UNIQUE, but explicit for clarity
```

---

### 6. Password Validation Rules

**Question**: What password strength requirements should be enforced beyond minimum length?

**Decision**: **Minimum 8 characters only** for Phase 1 (complexity rules deferred)

**Rationale**:

- **User Experience**: Overly complex rules frustrate users and lead to password reuse
- **NIST Guidelines**: NIST SP 800-63B recommends length over complexity
- **BCrypt Protection**: Strong hashing makes brute-force infeasible even for simple passwords
- **Phase 1 Scope**: MVP focuses on core functionality, advanced validation is enhancement
- **Future Extension**: Can add complexity rules (uppercase, numbers, symbols) in Phase 2

**Alternatives Considered**:

- **Complex Rules (uppercase, lowercase, number, symbol)**: Better security but worse UX, deferred
- **Minimum 12 characters**: More secure but may deter users, 8 is industry standard
- **Password strength meter**: Good UX enhancement but out of scope for Phase 1

**Implementation**:

```java
// DTO Validation
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    // ... other fields
}
```

**Future Enhancement** (Phase 2+):

- Add password strength estimation (zxcvbn library)
- Implement common password blacklist (top 10k passwords)
- Add password history (prevent reuse of last 3 passwords)
- Implement password expiration policy (90 days for admin accounts)

---

### 7. JWT Secret Key Management

**Question**: How should the JWT signing secret be generated and stored securely?

**Decision**: Use **Environment Variable with 256-bit Random Key**

**Rationale**:

- **Security**: Secret never committed to source control
- **12-Factor App**: Configuration via environment variables (cloud-native best practice)
- **Key Strength**: 256-bit (32 bytes) meets HMAC-SHA256 security requirements
- **Rotation Support**: Can rotate key by updating environment variable and redeploying
- **Development**: Use placeholder in application.yml, override in production

**Alternatives Considered**:

- **Hardcoded Secret**: Security risk, never acceptable
- **Config File**: Risk of accidental commit, not cloud-native
- **Key Management Service (AWS KMS, Vault)**: Over-engineered for Phase 1, good for Phase 3+

**Implementation**:

1. **Generate Secret** (one-time setup):

```bash
# Generate 256-bit random key (base64 encoded)
openssl rand -base64 32
# Example output: 8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb
```

2. **Configuration**:

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:changeme-use-env-variable-in-production}
  expiration: 86400000 # 24 hours
```

3. **Production Deployment**:

```bash
# Docker
docker run -e JWT_SECRET="8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb" ...

# Kubernetes
kubectl create secret generic jwt-secret --from-literal=JWT_SECRET="..."

# Environment variable
export JWT_SECRET="8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb"
```

4. **Validation on Startup**:

```java
@Component
public class JwtSecretValidator implements ApplicationListener<ApplicationReadyEvent> {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if ("changeme-use-env-variable-in-production".equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must be set in production!");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters!");
        }
    }
}
```

---

### 8. Role-Based Access Control Implementation

**Question**: How should role-based authorization be enforced on endpoints?

**Decision**: Use **Spring Security Method Security with @PreAuthorize**

**Rationale**:

- **Declarative**: Annotations make authorization rules explicit and visible
- **Type-Safe**: Compile-time checking of role names
- **Testable**: Easy to test with @WithMockUser in tests
- **Flexible**: Supports complex expressions (hasRole, hasAnyRole, custom SpEL)
- **Spring Standard**: Idiomatic Spring Security approach

**Alternatives Considered**:

- **Manual Role Checking**: Error-prone, not recommended
- **URL-Based Security**: Less flexible, harder to maintain
- **Custom Interceptors**: Reinventing the wheel, Spring Security is battle-tested

**Implementation**:

1. **Enable Method Security**:

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // ... security configuration
}
```

2. **Controller Authorization**:

```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        // Only ROLE_ADMIN can access
    }
}

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/my-subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription() {
        // Both ROLE_ADMIN and ROLE_USER can access
        // Tenant filtering ensures users only see their own data
    }
}
```

3. **Custom Security Expressions** (future enhancement):

```java
@PreAuthorize("@tenantSecurity.canAccessSubscription(#subscriptionId)")
@GetMapping("/subscriptions/{subscriptionId}")
public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable Long subscriptionId) {
    // Custom bean validates user has access to this specific subscription
}
```

---

## Summary of Technology Choices

| Component         | Technology                 | Version        | Rationale                                              |
| ----------------- | -------------------------- | -------------- | ------------------------------------------------------ |
| JWT Library       | JJWT (io.jsonwebtoken)     | 0.12.3         | Industry standard, Spring Boot 3 compatible, type-safe |
| Password Hashing  | BCrypt                     | Work Factor 12 | OWASP recommended, balances security and performance   |
| Tenant Isolation  | Hibernate Filters + AOP    | Hibernate 6.x  | Automatic, performant, defense-in-depth                |
| Token Expiration  | Access Token Only          | 24 hours       | Simplicity for Phase 1, stateless                      |
| Concurrency       | Database UNIQUE Constraint | PostgreSQL     | Atomic, no distributed locks needed                    |
| Password Rules    | Minimum Length             | 8 characters   | NIST-aligned, good UX                                  |
| Secret Management | Environment Variable       | 256-bit key    | 12-factor app, cloud-native                            |
| Authorization     | Spring Method Security     | @PreAuthorize  | Declarative, testable, Spring standard                 |

## Security Considerations

1. **JWT Secret Rotation**: Document process for rotating JWT secret (requires re-login for all users)
2. **Timing Attack Prevention**: BCrypt inherently resistant, but ensure error messages don't leak info
3. **Token Revocation**: Phase 1 has no revocation mechanism (acceptable for 24-hour tokens)
4. **HTTPS Enforcement**: Must be configured in production (Spring Security can enforce)
5. **CORS Configuration**: Must be properly configured for frontend integration
6. **Rate Limiting**: Deferred to Phase 4, but important for production

## Performance Benchmarks

Expected performance on modern server (4 CPU, 8GB RAM):

- **Registration**: 300-400ms (BCrypt hashing dominates)
- **Login**: 300-400ms (BCrypt verification dominates)
- **JWT Validation**: 10-50ms (HMAC verification)
- **Concurrent Logins**: 100+ req/s (CPU-bound by BCrypt)

## Testing Strategy

1. **Unit Tests**:
   - JwtTokenProvider: Token generation, validation, expiration, claims extraction
   - UserService: Registration logic, duplicate email handling
   - AuthenticationService: Login logic, password verification

2. **Integration Tests**:
   - AuthController: Full registration/login flows with MockMvc
   - UserRepository: Database constraints, unique email enforcement
   - Security: Role-based access control, tenant filtering

3. **Security Tests**:
   - Expired token rejection
   - Malformed token rejection
   - Cross-tenant access prevention
   - Password strength validation
   - Duplicate email prevention under concurrency

## References

- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [NIST SP 800-63B Digital Identity Guidelines](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [Hibernate Filters Documentation](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#filters)
