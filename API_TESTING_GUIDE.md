# 🧪 API Testing Guide - Billing Service

## Quick Start Testing

This guide provides curl commands to test all implemented endpoints.

---

## 🔑 **Prerequisites**

1. **Start the application**:

```bash
mvn spring-boot:run
```

2. **Get JWT Token** (assuming you have authentication endpoint):

```bash
# Login as ADMIN
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin123"
  }'

# Save the token
export ADMIN_TOKEN="eyJhbGc..."

# Login as USER
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "user123"
  }'

# Save the token
export USER_TOKEN="eyJhbGc..."
```

---

## 📋 **Plan Management API (ADMIN)**

### **1. Create a Free Plan**

```bash
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Free",
    "description": "Free tier for individuals",
    "price": 0.00,
    "billingCycle": "MONTHLY",
    "isActive": true,
    "featureLimits": [
      {"limitType": "max_users", "limitValue": 1},
      {"limitType": "max_projects", "limitValue": 3},
      {"limitType": "max_storage_gb", "limitValue": 5}
    ]
  }'
```

### **2. Create a Pro Plan**

```bash
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pro",
    "description": "Professional tier for teams",
    "price": 29.99,
    "billingCycle": "MONTHLY",
    "isActive": true,
    "featureLimits": [
      {"limitType": "max_users", "limitValue": 10},
      {"limitType": "max_projects", "limitValue": 50},
      {"limitType": "max_storage_gb", "limitValue": 100},
      {"limitType": "api_calls_per_month", "limitValue": 100000}
    ]
  }'
```

### **3. Create an Enterprise Plan**

```bash
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enterprise",
    "description": "Enterprise tier with unlimited features",
    "price": 299.99,
    "billingCycle": "YEARLY",
    "isActive": true,
    "featureLimits": [
      {"limitType": "max_users", "limitValue": -1},
      {"limitType": "max_projects", "limitValue": -1},
      {"limitType": "max_storage_gb", "limitValue": -1},
      {"limitType": "api_calls_per_month", "limitValue": -1}
    ]
  }'
```

### **4. List All Plans**

```bash
curl -X GET http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### **5. Get Plan by ID**

```bash
# Replace {plan_id} with actual UUID
curl -X GET http://localhost:8080/api/v1/plans/{plan_id} \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### **6. Update Plan**

```bash
# Replace {plan_id} with actual UUID
curl -X PUT http://localhost:8080/api/v1/plans/{plan_id} \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Updated professional tier",
    "price": 39.99,
    "isActive": true
  }'
```

### **7. Delete Plan (Soft Delete)**

```bash
# Replace {plan_id} with actual UUID
curl -X DELETE http://localhost:8080/api/v1/plans/{plan_id} \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 💳 **Subscription Management API (USER)**

### **1. Subscribe to a Plan**

```bash
# Replace {plan_id} with actual UUID from "List All Plans"
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "planId": "{plan_id}"
  }'
```

### **2. Get My Current Subscription**

```bash
curl -X GET http://localhost:8080/api/v1/subscriptions/my-subscription \
  -H "Authorization: Bearer $USER_TOKEN"
```

### **3. Get Subscription by ID**

```bash
# Replace {subscription_id} with actual UUID
curl -X GET http://localhost:8080/api/v1/subscriptions/{subscription_id} \
  -H "Authorization: Bearer $USER_TOKEN"
```

### **4. Upgrade Subscription**

```bash
# Replace {subscription_id} and {new_plan_id} with actual UUIDs
curl -X PUT http://localhost:8080/api/v1/subscriptions/{subscription_id}/upgrade \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newPlanId": "{new_plan_id}"
  }'
```

### **5. Cancel Subscription**

```bash
# Replace {subscription_id} with actual UUID
curl -X DELETE http://localhost:8080/api/v1/subscriptions/{subscription_id} \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

## 📊 **Analytics API (ADMIN)**

### **1. Get Analytics Dashboard**

```bash
curl -X GET http://localhost:8080/api/v1/analytics \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected Response:**

```json
{
  "tenantMetrics": {
    "totalSubscriptions": 10,
    "activeSubscriptions": 7,
    "pastDueSubscriptions": 1,
    "canceledSubscriptions": 2,
    "churnRate": 20.0
  },
  "planMetrics": [
    {
      "planId": "uuid-1",
      "planName": "Pro",
      "activeSubscriptions": 5,
      "monthlyRecurringRevenue": 149.95,
      "yearlyRecurringRevenue": 0.0
    },
    {
      "planId": "uuid-2",
      "planName": "Enterprise",
      "activeSubscriptions": 2,
      "monthlyRecurringRevenue": 0.0,
      "yearlyRecurringRevenue": 599.98
    }
  ],
  "revenueMetrics": {
    "totalMonthlyRecurringRevenue": 199.93,
    "totalYearlyRecurringRevenue": 2999.16,
    "averageRevenuePerUser": 28.56
  }
}
```

---

## 🧪 **Complete Test Flow**

### **Scenario: Complete Subscription Lifecycle**

```bash
# 1. Admin creates a Pro plan
PLAN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pro",
    "description": "Professional tier",
    "price": 29.99,
    "billingCycle": "MONTHLY",
    "featureLimits": [
      {"limitType": "max_users", "limitValue": 10}
    ]
  }')

# Extract plan ID
PLAN_ID=$(echo $PLAN_RESPONSE | jq -r '.id')
echo "Created Plan ID: $PLAN_ID"

# 2. User subscribes to the plan
SUB_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"planId\": \"$PLAN_ID\"}")

# Extract subscription ID
SUB_ID=$(echo $SUB_RESPONSE | jq -r '.id')
echo "Created Subscription ID: $SUB_ID"

# 3. User views their subscription
curl -X GET http://localhost:8080/api/v1/subscriptions/my-subscription \
  -H "Authorization: Bearer $USER_TOKEN"

# 4. Admin creates Enterprise plan
ENTERPRISE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Enterprise",
    "description": "Enterprise tier",
    "price": 99.99,
    "billingCycle": "MONTHLY",
    "featureLimits": [
      {"limitType": "max_users", "limitValue": -1}
    ]
  }')

ENTERPRISE_ID=$(echo $ENTERPRISE_RESPONSE | jq -r '.id')
echo "Created Enterprise Plan ID: $ENTERPRISE_ID"

# 5. User upgrades to Enterprise
curl -X PUT http://localhost:8080/api/v1/subscriptions/$SUB_ID/upgrade \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"newPlanId\": \"$ENTERPRISE_ID\"}"

# 6. Admin views analytics
curl -X GET http://localhost:8080/api/v1/analytics \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 7. User cancels subscription
curl -X DELETE http://localhost:8080/api/v1/subscriptions/$SUB_ID \
  -H "Authorization: Bearer $USER_TOKEN"

# 8. Verify cancellation
curl -X GET http://localhost:8080/api/v1/subscriptions/my-subscription \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

## ⚠️ **Error Scenarios**

### **1. Duplicate Subscription**

```bash
# Try to subscribe twice (should fail with 409 Conflict)
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"planId": "{plan_id}"}'
```

### **2. Invalid Plan ID**

```bash
# Non-existent plan (should fail with 404 Not Found)
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"planId": "00000000-0000-0000-0000-000000000000"}'
```

### **3. Unauthorized Access**

```bash
# Try to access admin endpoint as user (should fail with 403 Forbidden)
curl -X POST http://localhost:8080/api/v1/plans \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test"}'
```

### **4. Delete Plan with Active Subscriptions**

```bash
# Should fail with 409 Conflict
curl -X DELETE http://localhost:8080/api/v1/plans/{plan_id_with_subscriptions} \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 📝 **Response Codes**

| Code | Meaning      | Example                   |
| ---- | ------------ | ------------------------- |
| 200  | OK           | Successful GET, PUT       |
| 201  | Created      | Successful POST           |
| 204  | No Content   | Successful DELETE         |
| 400  | Bad Request  | Invalid request data      |
| 401  | Unauthorized | Missing/invalid JWT token |
| 403  | Forbidden    | Insufficient permissions  |
| 404  | Not Found    | Resource doesn't exist    |
| 409  | Conflict     | Business rule violation   |
| 500  | Server Error | Unexpected error          |

---

## 🔍 **Swagger UI**

For interactive API testing, visit:

```
http://localhost:8080/swagger-ui.html
```

---

## 💡 **Tips**

1. **Save UUIDs**: Store plan and subscription IDs for reuse
2. **Use jq**: Parse JSON responses easily
3. **Check logs**: Monitor application logs for debugging
4. **Test edge cases**: Try invalid inputs to verify validation
5. **Cache testing**: Call endpoints multiple times to see caching in action

---

**Happy Testing!** 🎉
