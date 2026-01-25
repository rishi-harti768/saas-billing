# Quickstart Guide: Authentication & Multi-Tenancy

**Feature**: 001-auth-multi-tenancy  
**Date**: 2026-01-24  
**Purpose**: Setup and testing guide for authentication and multi-tenant security implementation

## Prerequisites

- **Java**: JDK 17 or higher
- **Maven**: 3.8+
- **PostgreSQL**: 14+ (local or Docker)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **API Client**: Postman, cURL, or HTTPie for testing

## Quick Setup (5 minutes)

### 1. Database Setup

**Option A: Docker (Recommended)**

```bash
# Start PostgreSQL container
docker run --name billing-postgres \
  -e POSTGRES_DB=billing_db \
  -e POSTGRES_USER=billing_user \
  -e POSTGRES_PASSWORD=billing_pass \
  -p 5432:5432 \
  -d postgres:15-alpine

# Verify database is running
docker ps | grep billing-postgres
```

**Option B: Local PostgreSQL**

```sql
-- Connect to PostgreSQL as superuser
psql -U postgres

-- Create database and user
CREATE DATABASE billing_db;
CREATE USER billing_user WITH PASSWORD 'billing_pass';
GRANT ALL PRIVILEGES ON DATABASE billing_db TO billing_user;
```

### 2. Generate JWT Secret

```bash
# Generate a secure 256-bit random key
openssl rand -base64 32

# Example output: 8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb
```

### 3. Configure Application

Create or update `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: billing-service

  datasource:
    url: jdbc:postgresql://localhost:5432/billing_db
    username: billing_user
    password: billing_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate # Use Flyway for migrations
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:changeme-use-env-variable-in-production}
  expiration: 86400000 # 24 hours in milliseconds

# Logging
logging:
  level:
    org.gb.billing: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### 4. Set Environment Variable

```bash
# Linux/Mac
export JWT_SECRET="8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb"

# Windows (PowerShell)
$env:JWT_SECRET="8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb"

# Windows (CMD)
set JWT_SECRET=8Zz5tw0Ionm3XPZZfN0NOml3z9FMfmpgXwovR9fp6ryDIoGRM8EPHAB6iHsc0fb
```

### 5. Run Database Migrations

```bash
# Maven
./mvnw flyway:migrate

# Gradle
./gradlew flywayMigrate
```

### 6. Build and Run

```bash
# Maven
./mvnw clean install
./mvnw spring-boot:run

# Gradle
./gradlew clean build
./gradlew bootRun
```

Application should start on `http://localhost:8080`

---

## Testing the API

### 1. Register Admin User

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "AdminPass123",
    "role": "ROLE_ADMIN",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Expected Response (201 Created):**

```json
{
  "id": 1,
  "email": "admin@example.com",
  "role": "ROLE_ADMIN",
  "tenantId": null,
  "firstName": "John",
  "lastName": "Doe"
}
```

### 2. Register Subscriber User

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@acme.com",
    "password": "UserPass456",
    "role": "ROLE_USER",
    "tenantName": "Acme Corp",
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

**Expected Response (201 Created):**

```json
{
  "id": 2,
  "email": "user@acme.com",
  "role": "ROLE_USER",
  "tenantId": 1,
  "firstName": "Jane",
  "lastName": "Smith"
}
```

### 3. Login

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@acme.com",
    "password": "UserPass456"
  }'
```

**Expected Response (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000
}
```

### 4. Test Validation Errors

**Invalid Email:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "Pass123",
    "role": "ROLE_USER"
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "timestamp": "2026-01-24T22:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid email format",
  "path": "/api/v1/auth/register"
}
```

**Weak Password:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@test.com",
    "password": "123",
    "role": "ROLE_USER"
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "timestamp": "2026-01-24T22:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Password must be between 8 and 100 characters",
  "path": "/api/v1/auth/register"
}
```

### 5. Test Duplicate Email

**Register same email twice:**

```bash
# First registration (succeeds)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "duplicate@test.com",
    "password": "Pass12345",
    "role": "ROLE_USER",
    "tenantName": "Test Corp"
  }'

# Second registration (fails)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "duplicate@test.com",
    "password": "DifferentPass",
    "role": "ROLE_USER",
    "tenantName": "Another Corp"
  }'
```

**Expected Response (409 Conflict):**

```json
{
  "timestamp": "2026-01-24T22:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Email already registered",
  "path": "/api/v1/auth/register"
}
```

### 6. Test Invalid Credentials

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@acme.com",
    "password": "WrongPassword"
  }'
```

**Expected Response (401 Unauthorized):**

```json
{
  "timestamp": "2026-01-24T22:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "path": "/api/v1/auth/login"
}
```

---

## Verify JWT Token

### Decode Token (Online Tool)

1. Copy the JWT token from login/register response
2. Visit https://jwt.io
3. Paste token in "Encoded" section
4. Verify payload contains:
   - `sub`: User ID
   - `email`: User email
   - `role`: ROLE_ADMIN or ROLE_USER
   - `tenantId`: Tenant ID (for ROLE_USER) or null (for ROLE_ADMIN)
   - `iat`: Issued at timestamp
   - `exp`: Expiration timestamp

**Example Decoded Payload:**

```json
{
  "sub": "1",
  "email": "user@acme.com",
  "role": "ROLE_USER",
  "tenantId": 1,
  "iat": 1706134800,
  "exp": 1706221200
}
```

### Verify Token Programmatically

```bash
# Install jq for JSON parsing
# Linux: sudo apt-get install jq
# Mac: brew install jq

# Extract and decode token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq
```

---

## Database Verification

### Check Created Users

```sql
-- Connect to database
psql -U billing_user -d billing_db

-- View all users
SELECT id, email, role, tenant_id, active, created_date
FROM users
ORDER BY created_date DESC;

-- View all tenants
SELECT id, name, active, created_date
FROM tenants
ORDER BY created_date DESC;

-- Verify admin has no tenant
SELECT email, role, tenant_id
FROM users
WHERE role = 'ROLE_ADMIN';

-- Verify users have tenants
SELECT u.email, u.role, t.name as tenant_name
FROM users u
LEFT JOIN tenants t ON u.tenant_id = t.id
WHERE u.role = 'ROLE_USER';
```

### Verify Password Hashing

```sql
-- Check password is BCrypt-hashed (starts with $2a$ or $2b$)
SELECT email, password
FROM users
LIMIT 1;

-- Expected output:
-- email: user@acme.com
-- password: $2a$12$abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRS
```

---

## Swagger UI

Once the application is running, access the interactive API documentation:

**URL**: http://localhost:8080/swagger-ui.html

**Features**:

- Interactive API testing
- Request/response examples
- Schema documentation
- Try-it-out functionality

---

## Running Tests

### Unit Tests

```bash
# Maven
./mvnw test

# Gradle
./gradlew test

# Run specific test class
./mvnw test -Dtest=UserServiceTest
./mvnw test -Dtest=AuthenticationServiceTest
```

### Integration Tests

```bash
# Maven
./mvnw verify

# Gradle
./gradlew integrationTest

# Run with Testcontainers (PostgreSQL)
./mvnw verify -Dspring.profiles.active=test
```

### Test Coverage Report

```bash
# Maven (JaCoCo)
./mvnw clean test jacoco:report
# Report: target/site/jacoco/index.html

# Gradle
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

---

## Troubleshooting

### Issue: Database connection failed

**Error**: `org.postgresql.util.PSQLException: Connection refused`

**Solution**:

```bash
# Check PostgreSQL is running
docker ps | grep postgres
# or
sudo systemctl status postgresql

# Verify connection details in application.yml match database
# Check port (default 5432), username, password, database name
```

### Issue: JWT secret validation failed

**Error**: `IllegalStateException: JWT_SECRET must be set in production!`

**Solution**:

```bash
# Ensure JWT_SECRET environment variable is set
echo $JWT_SECRET  # Linux/Mac
echo %JWT_SECRET%  # Windows CMD
$env:JWT_SECRET   # Windows PowerShell

# Set if missing (see step 4 in Quick Setup)
```

### Issue: Flyway migration failed

**Error**: `FlywayException: Validate failed: Migrations have failed validation`

**Solution**:

```bash
# Clean and re-run migrations
./mvnw flyway:clean flyway:migrate

# Or drop and recreate database
psql -U postgres -c "DROP DATABASE billing_db;"
psql -U postgres -c "CREATE DATABASE billing_db;"
./mvnw flyway:migrate
```

### Issue: BCrypt password too long

**Error**: `IllegalArgumentException: Encoded password does not look like BCrypt`

**Solution**:

- Ensure password field in database is VARCHAR(60) or larger
- BCrypt hashes are always 60 characters
- Check migration script V002\_\_create_user_table.sql

### Issue: Duplicate email not detected

**Error**: No 409 Conflict when registering duplicate email

**Solution**:

```sql
-- Verify UNIQUE constraint exists
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'users' AND constraint_type = 'UNIQUE';

-- If missing, add constraint
ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE (email);
```

---

## Next Steps

After successfully testing authentication:

1. **Implement Protected Endpoints** (Phase 2):
   - Add `@PreAuthorize("hasRole('ADMIN')")` to admin endpoints
   - Add `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")` to user endpoints
   - Test role-based access control

2. **Test Multi-Tenant Isolation** (Phase 2):
   - Create multiple tenants with users
   - Verify users can only access their tenant's data
   - Test Hibernate filter enforcement

3. **Add Refresh Tokens** (Phase 2+):
   - Implement POST /api/v1/auth/refresh endpoint
   - Store refresh tokens in Redis
   - Implement token rotation

4. **Production Hardening** (Phase 3+):
   - Enable HTTPS
   - Configure CORS
   - Add rate limiting
   - Implement token revocation
   - Add password reset flow
   - Enable audit logging

---

## Performance Testing

### Load Test with Apache Bench

```bash
# Install Apache Bench
# Linux: sudo apt-get install apache2-utils
# Mac: brew install httpd

# Test login endpoint (100 requests, 10 concurrent)
ab -n 100 -c 10 -p login.json -T application/json \
  http://localhost:8080/api/v1/auth/login

# Create login.json file
echo '{"email":"user@acme.com","password":"UserPass456"}' > login.json
```

**Expected Results**:

- Mean response time: 300-400ms (BCrypt verification)
- Requests per second: 25-30 req/s (single instance)
- No failed requests

### Monitor Performance

```bash
# View application logs
tail -f logs/spring-boot-application.log

# Monitor database connections
psql -U billing_user -d billing_db -c "SELECT count(*) FROM pg_stat_activity;"

# Check JVM memory usage
jps -l  # Find Java process ID
jstat -gc <PID> 1000  # GC stats every 1 second
```

---

## Security Checklist

Before deploying to production:

- [ ] JWT_SECRET is set via environment variable (not hardcoded)
- [ ] JWT_SECRET is at least 32 characters (256-bit)
- [ ] Database credentials are not in source control
- [ ] HTTPS is enabled (Spring Security can enforce)
- [ ] CORS is properly configured for frontend domain
- [ ] Rate limiting is enabled (Phase 4)
- [ ] Actuator endpoints are secured (only accessible to admins)
- [ ] SQL injection prevention (JPA parameterized queries)
- [ ] XSS prevention (Spring Security default headers)
- [ ] CSRF protection (disabled for stateless JWT, but verify)
- [ ] Sensitive data not logged (passwords, tokens)
- [ ] Database backups configured
- [ ] Monitoring and alerting configured

---

## Additional Resources

- **API Contract**: See `contracts/auth-api.yaml` for OpenAPI specification
- **Data Model**: See `data-model.md` for entity relationships and schema
- **Research**: See `research.md` for technology decisions and best practices
- **Implementation Plan**: See `plan.md` for architecture and constitution compliance

---

## Support

For issues or questions:

- Check troubleshooting section above
- Review logs in `logs/spring-boot-application.log`
- Verify database state with SQL queries
- Test with Swagger UI for interactive debugging
- Review test failures for specific error messages
