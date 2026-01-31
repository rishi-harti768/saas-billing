# Quickstart: Analytics & Documentation

## Prerequisites
- Start the application: `mvn spring-boot:run`
- Ensure database has test data (Active and Cancelled subscriptions).

## Usage

### 1. View Documentation
Navigate to `http://localhost:8080/swagger-ui.html`

### 2. Verify Analytics (Admin Only)

**Get MRR**:
```bash
curl -X GET http://localhost:8080/api/v1/analytics/mrr \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

**Get Churn**:
```bash
curl -X GET http://localhost:8080/api/v1/analytics/churn \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

## Troubleshooting
- If **403 Forbidden**: Check user role in DB (`ROLE_ADMIN` required).
- If **Swagger 404**: Check `application.properties` for custom context path.
