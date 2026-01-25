# Quickstart Guide: Product & Subscription Engine

**Feature**: Product & Subscription Engine  
**Branch**: `002-product-subscription-engine`  
**Date**: 2026-01-25  
**Purpose**: Setup and testing guide for local development

## Prerequisites

Before starting, ensure you have completed Phase 1 (Authentication & Multi-Tenancy):

- ✅ PostgreSQL database running
- ✅ User authentication working (JWT tokens)
- ✅ Multi-tenant isolation configured
- ✅ Admin and user accounts created

**Required Software**:

- Java 17+ (JDK)
- Maven 3.8+
- PostgreSQL 14+
- Postman or curl (for API testing)
- Git

## Project Setup

### 1. Checkout Feature Branch

```bash
git checkout 002-product-subscription-engine
```

### 2. Database Migration

The feature includes 4 new database migrations:

```bash
# Migrations will run automatically on application startup via Flyway
# V003__create_billing_plan_table.sql
# V004__create_feature_limit_table.sql
# V005__create_subscription_table.sql
# V006__create_subscription_transition_log_table.sql
```

**Verify migrations**:

```sql
-- Connect to PostgreSQL
psql -U postgres -d billing_db

-- Check migration status
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- Verify tables exist
\dt
-- Should show: billing_plan, feature_limit, subscription, subscription_transition_log
```

### 3. Build and Run Application

```bash
# Clean build
mvn clean install

# Run application
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Expected output**:

```
Started BillingApplication in 5.234 seconds (JVM running for 5.678)
Flyway migration completed successfully: 6 migrations applied
Caffeine cache initialized: plans
```

### 4. Verify Application Health

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

## API Testing Workflow

### Step 1: Authenticate as Admin

First, obtain an admin JWT token:

```bash
# Login as admin
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin123"
  }'

# Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "role": "ROLE_ADMIN",
  "tenantId": null
}

# Save the token for subsequent requests
export ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Step 2: Create Billing Plans (Admin Only)

```bash
# Create Free plan
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Free",
    "description": "Basic features for individuals",
    "price": 0.00,
    "billingCycle": "MONTHLY",
    "featureLimits": [
      {"limitType": "api_calls", "limitValue": 100},
      {"limitType": "max_users", "limitValue": 1}
    ]
  }'

# Create Pro plan
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pro",
    "description": "Advanced features for professionals",
    "price": 29.99,
    "billingCycle": "MONTHLY",
    "featureLimits": [
      {"limitType": "api_calls", "limitValue": 10000},
      {"limitType": "max_users", "limitValue": 10}
    ]
  }'

# Create Enterprise plan
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enterprise",
    "description": "Full features for organizations",
    "price": 299.99,
    "billingCycle": "YEARLY",
    "featureLimits": [
      {"limitType": "api_calls", "limitValue": 1000000},
      {"limitType": "max_users", "limitValue": 100}
    ]
  }'
```

### Step 3: List All Plans (Public)

```bash
# Get all plans (no authentication required for listing)
curl http://localhost:8080/api/v1/plans

# Expected response:
{
  "content": [
    {
      "id": 1,
      "name": "Free",
      "description": "Basic features for individuals",
      "price": 0.00,
      "billingCycle": "MONTHLY",
      "featureLimits": [
        {"limitType": "api_calls", "limitValue": 100},
        {"limitType": "max_users", "limitValue": 1}
      ],
      "createdDate": "2026-01-25T10:00:00Z",
      "lastModifiedDate": "2026-01-25T10:00:00Z"
    },
    ...
  ],
  "totalElements": 3,
  "totalPages": 1,
  "pageNumber": 0,
  "pageSize": 20
}
```

### Step 4: Authenticate as User

```bash
# Login as regular user
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "user123"
  }'

# Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "role": "ROLE_USER",
  "tenantId": 1
}

# Save the token
export USER_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Step 5: Create Subscription (User)

```bash
# Subscribe to Pro plan (planId = 2)
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "planId": 2
  }'

# Expected response:
{
  "id": 1,
  "userId": 1,
  "plan": {
    "id": 2,
    "name": "Pro",
    "price": 29.99,
    "billingCycle": "MONTHLY"
  },
  "status": "ACTIVE",
  "startDate": "2026-01-25",
  "nextBillingDate": "2026-02-25",
  "canceledAt": null,
  "createdDate": "2026-01-25T14:30:00Z",
  "lastModifiedDate": "2026-01-25T14:30:00Z"
}
```

### Step 6: Get My Subscription (User)

```bash
# Get current subscription
curl http://localhost:8080/api/v1/subscriptions/my-subscription \
  -H "Authorization: Bearer $USER_TOKEN"

# Expected response: Same as Step 5
```

### Step 7: Upgrade Subscription (User)

```bash
# Upgrade to Enterprise plan (planId = 3)
curl -X PUT http://localhost:8080/api/v1/subscriptions/1/upgrade \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newPlanId": 3
  }'

# Expected response:
{
  "id": 1,
  "userId": 1,
  "plan": {
    "id": 3,
    "name": "Enterprise",
    "price": 299.99,
    "billingCycle": "YEARLY"
  },
  "status": "ACTIVE",
  "startDate": "2026-01-25",
  "nextBillingDate": "2027-01-25",  # Recalculated for YEARLY cycle
  "canceledAt": null,
  "createdDate": "2026-01-25T14:30:00Z",
  "lastModifiedDate": "2026-01-25T14:35:00Z"
}
```

### Step 8: View Subscription Transition History (Admin)

```bash
# Get state transition log for subscription
curl http://localhost:8080/api/v1/subscriptions/1/transitions \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected response:
[
  {
    "id": 1,
    "fromState": "ACTIVE",
    "toState": "ACTIVE",
    "reason": "Subscription created",
    "transitionDate": "2026-01-25T14:30:00Z"
  },
  {
    "id": 2,
    "fromState": "ACTIVE",
    "toState": "ACTIVE",
    "reason": "Subscription upgraded to Enterprise plan",
    "transitionDate": "2026-01-25T14:35:00Z"
  }
]
```

### Step 9: Cancel Subscription (User)

```bash
# Cancel subscription
curl -X DELETE http://localhost:8080/api/v1/subscriptions/1/cancel \
  -H "Authorization: Bearer $USER_TOKEN"

# Expected response: 204 No Content

# Verify cancellation
curl http://localhost:8080/api/v1/subscriptions/my-subscription \
  -H "Authorization: Bearer $USER_TOKEN"

# Response shows CANCELED status:
{
  "id": 1,
  "userId": 1,
  "plan": {...},
  "status": "CANCELED",
  "startDate": "2026-01-25",
  "nextBillingDate": null,  # Cleared on cancellation
  "canceledAt": "2026-01-25T14:40:00Z",
  ...
}
```

### Step 10: View Analytics (Admin)

```bash
# Get subscription count by plan
curl http://localhost:8080/api/v1/admin/analytics/subscriptions/by-plan \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Get subscription count by status
curl http://localhost:8080/api/v1/admin/analytics/subscriptions/by-status \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Calculate churn rate
curl "http://localhost:8080/api/v1/admin/analytics/churn-rate?startDate=2026-01-01&endDate=2026-01-31" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Get revenue summary
curl http://localhost:8080/api/v1/admin/analytics/revenue-summary \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Testing Scenarios

### Scenario 1: Prevent Duplicate Active Subscriptions

```bash
# Try to create second subscription while one is active
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"planId": 1}'

# Expected: 409 Conflict
{
  "timestamp": "2026-01-25T14:45:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "User already has an active subscription. Please cancel or upgrade existing subscription.",
  "path": "/api/v1/subscriptions"
}
```

### Scenario 2: Prevent Plan Deletion with Active Subscriptions

```bash
# Try to delete Pro plan while subscriptions exist
curl -X DELETE http://localhost:8080/api/v1/plans/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected: 409 Conflict
{
  "timestamp": "2026-01-25T14:45:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot delete plan 'Pro': 15 active subscriptions exist",
  "path": "/api/v1/plans/2"
}
```

### Scenario 3: Test Caching

```bash
# First request (cache miss)
time curl http://localhost:8080/api/v1/plans/2

# Second request (cache hit - should be faster)
time curl http://localhost:8080/api/v1/plans/2

# Update plan (evicts cache)
curl -X PUT http://localhost:8080/api/v1/plans/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"price": 39.99}'

# Next request (cache miss again)
time curl http://localhost:8080/api/v1/plans/2
```

### Scenario 4: Test Optimistic Locking

```bash
# Simulate concurrent updates using two terminals

# Terminal 1: Get subscription
SUBSCRIPTION=$(curl http://localhost:8080/api/v1/subscriptions/1 \
  -H "Authorization: Bearer $USER_TOKEN")

# Terminal 2: Upgrade subscription
curl -X PUT http://localhost:8080/api/v1/subscriptions/1/upgrade \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newPlanId": 3}'

# Terminal 1: Try to cancel (version mismatch)
curl -X DELETE http://localhost:8080/api/v1/subscriptions/1/cancel \
  -H "Authorization: Bearer $USER_TOKEN"

# Expected: 409 Conflict (optimistic lock exception)
{
  "timestamp": "2026-01-25T14:50:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Subscription was modified by another process. Please retry.",
  "path": "/api/v1/subscriptions/1/cancel"
}
```

## Swagger UI

Access interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

**Available API Groups**:

- **Plans API**: Plan management (CRUD operations)
- **Subscriptions API**: Subscription lifecycle (subscribe, upgrade, cancel)
- **Analytics API**: Subscription metrics and reporting

## Database Inspection

### View Billing Plans

```sql
SELECT * FROM billing_plan WHERE deleted = false;
```

### View Subscriptions

```sql
SELECT s.id, s.status, s.start_date, s.next_billing_date,
       u.email, p.name AS plan_name, p.price
FROM subscription s
JOIN users u ON s.user_id = u.id
JOIN billing_plan p ON s.plan_id = p.id
ORDER BY s.created_date DESC;
```

### View Subscription Transitions

```sql
SELECT st.*, s.user_id
FROM subscription_transition_log st
JOIN subscription s ON st.subscription_id = s.id
ORDER BY st.transition_date DESC
LIMIT 20;
```

### Calculate MRR

```sql
SELECT
  SUM(CASE
    WHEN p.billing_cycle = 'MONTHLY' THEN p.price
    WHEN p.billing_cycle = 'YEARLY' THEN p.price / 12
  END) AS monthly_recurring_revenue
FROM subscription s
JOIN billing_plan p ON s.plan_id = p.id
WHERE s.status = 'ACTIVE';
```

## Troubleshooting

### Issue: Migrations Not Running

**Solution**:

```bash
# Check Flyway configuration in application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# Manually run migrations
mvn flyway:migrate
```

### Issue: Cache Not Working

**Solution**:

```bash
# Verify Caffeine dependency in pom.xml
# Check cache configuration in application.yml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=5m

# Enable cache logging
logging:
  level:
    org.springframework.cache: DEBUG
```

### Issue: Optimistic Lock Exceptions

**Solution**:

- This is expected behavior for concurrent updates
- Implement retry logic in client
- Use exponential backoff for retries

### Issue: 403 Forbidden on Admin Endpoints

**Solution**:

```bash
# Verify JWT token has ROLE_ADMIN
# Decode token at https://jwt.io
# Check "role" claim in payload

# Ensure user has admin role in database
SELECT email, role FROM users WHERE email = 'admin@example.com';
```

## Next Steps

1. ✅ Feature setup complete
2. → Proceed to `/speckit.tasks` to generate implementation tasks
3. → Implement entities, repositories, services, and controllers
4. → Write unit and integration tests
5. → Deploy to staging environment

## Additional Resources

- **API Contracts**: `specs/002-product-subscription-engine/contracts/`
- **Data Model**: `specs/002-product-subscription-engine/data-model.md`
- **Research**: `specs/002-product-subscription-engine/research.md`
- **Specification**: `specs/002-product-subscription-engine/spec.md`
