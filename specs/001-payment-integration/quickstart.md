# Quickstart Guide: External Payment Integration

**Feature**: External Payment Integration  
**Branch**: `001-payment-integration`  
**Date**: 2026-01-25

## Overview

This guide provides step-by-step instructions for developers to set up, develop, test, and deploy the external payment integration feature. Follow these instructions to get the payment system running locally and in production.

---

## Prerequisites

Before starting, ensure you have:

- ✅ Java 17 or higher installed
- ✅ Maven 3.8+ installed
- ✅ PostgreSQL 14+ running locally or accessible
- ✅ Docker installed (for Testcontainers)
- ✅ Stripe account (free tier for testing)
- ✅ Git configured with access to the repository

---

## 1. Environment Setup

### 1.1 Clone and Checkout Feature Branch

```bash
git clone <repository-url>
cd saas-billing
git checkout 001-payment-integration
```

### 1.2 Configure Environment Variables

Create or update `src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/saas_billing
    username: postgres
    password: your_password
  
stripe:
  api-key: sk_test_YOUR_STRIPE_TEST_KEY
  webhook-secret: whsec_YOUR_WEBHOOK_SECRET
  
resilience4j:
  circuitbreaker:
    instances:
      stripePayment:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
```

**Security Note**: Never commit real API keys. Use environment variables in production:

```bash
export STRIPE_API_KEY=sk_test_YOUR_KEY
export STRIPE_WEBHOOK_SECRET=whsec_YOUR_SECRET
```

### 1.3 Get Stripe Test Credentials

1. Sign up at [stripe.com](https://stripe.com)
2. Navigate to **Developers → API Keys**
3. Copy your **Test Mode** secret key (starts with `sk_test_`)
4. Navigate to **Developers → Webhooks**
5. Click **Add endpoint** and use: `http://localhost:8080/api/v1/webhooks/stripe`
6. Select events: `payment_intent.succeeded`, `payment_intent.payment_failed`, `invoice.payment_succeeded`
7. Copy the **Signing secret** (starts with `whsec_`)

---

## 2. Database Setup

### 2.1 Create Database

```bash
psql -U postgres
CREATE DATABASE saas_billing;
\q
```

### 2.2 Run Flyway Migrations

Migrations will run automatically on application startup. To run manually:

```bash
mvn flyway:migrate
```

Verify tables were created:

```sql
\c saas_billing
\dt
-- Should see: payment_transactions, webhook_events, subscriptions (updated)
```

---

## 3. Build and Run

### 3.1 Install Dependencies

```bash
mvn clean install
```

### 3.2 Run Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Application should start on `http://localhost:8080`

### 3.3 Verify Swagger UI

Open browser to: `http://localhost:8080/swagger-ui.html`

You should see the Payment Integration API endpoints documented.

---

## 4. Testing the Payment Flow

### 4.1 Create Test User and Subscription

Use existing auth endpoints to create a user and subscription:

```bash
# Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "role": "ROLE_USER"
  }'

# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'

# Save the JWT token from response
export JWT_TOKEN="<your_jwt_token>"

# Create subscription (use existing subscription endpoint)
curl -X POST http://localhost:8080/api/v1/subscriptions/subscribe \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "planId": "pro-monthly"
  }'

# Save the subscription ID from response
export SUBSCRIPTION_ID="<subscription_id>"
```

### 4.2 Initiate Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments/initiate \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subscriptionId": '$SUBSCRIPTION_ID',
    "planId": "pro-monthly",
    "returnUrl": "http://localhost:3000/payment/success",
    "cancelUrl": "http://localhost:3000/payment/cancel"
  }'
```

Response will include `redirectUrl`. Open this URL in a browser to complete payment.

### 4.3 Test with Stripe Test Cards

Use these test card numbers in the Stripe checkout:

- **Success**: `4242 4242 4242 4242` (any future expiry, any CVC)
- **Decline**: `4000 0000 0000 0002`
- **Insufficient Funds**: `4000 0000 0000 9995`

### 4.4 Trigger Webhook Locally

Install Stripe CLI for local webhook testing:

```bash
# Install Stripe CLI
# macOS: brew install stripe/stripe-cli/stripe
# Windows: scoop install stripe
# Linux: Download from https://github.com/stripe/stripe-cli/releases

# Login to Stripe
stripe login

# Forward webhooks to local server
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```

The Stripe CLI will display a webhook signing secret. Update your `application-local.yml` with this secret.

Now complete a test payment and watch the webhook events arrive in your application logs.

### 4.5 Verify Payment History

```bash
curl -X GET "http://localhost:8080/api/v1/payments/history?page=0&size=20" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## 5. Running Tests

### 5.1 Unit Tests

```bash
mvn test -Dtest=PaymentServiceTest
mvn test -Dtest=WebhookServiceTest
```

### 5.2 Integration Tests

```bash
mvn test -Dtest=PaymentControllerTest
mvn test -Dtest=WebhookControllerTest
```

**Note**: Integration tests use Testcontainers and require Docker to be running.

### 5.3 Run All Tests

```bash
mvn clean verify
```

### 5.4 Check Code Coverage

```bash
mvn clean verify jacoco:report
```

Open `target/site/jacoco/index.html` in a browser to view coverage report.

---

## 6. Debugging

### 6.1 Enable Debug Logging

Add to `application-local.yml`:

```yaml
logging:
  level:
    org.gb.billing.service.PaymentService: DEBUG
    org.gb.billing.service.WebhookService: DEBUG
    com.stripe: DEBUG
```

### 6.2 Common Issues

**Issue**: Webhook signature verification fails  
**Solution**: Ensure `stripe.webhook-secret` matches the secret from Stripe CLI or webhook endpoint configuration

**Issue**: Payment initiation returns 500 error  
**Solution**: Check Stripe API key is valid and in test mode. Check application logs for circuit breaker state.

**Issue**: Database connection fails  
**Solution**: Verify PostgreSQL is running and credentials in `application-local.yml` are correct

**Issue**: Testcontainers tests fail  
**Solution**: Ensure Docker is running and you have permissions to pull images

### 6.3 Monitoring Circuit Breaker

Check circuit breaker state via Actuator:

```bash
curl http://localhost:8080/actuator/circuitbreakers
```

---

## 7. Production Deployment

### 7.1 Environment Variables

Set these environment variables in your production environment:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/saas_billing
SPRING_DATASOURCE_USERNAME=<prod_username>
SPRING_DATASOURCE_PASSWORD=<prod_password>
STRIPE_API_KEY=sk_live_YOUR_LIVE_KEY
STRIPE_WEBHOOK_SECRET=whsec_YOUR_PROD_SECRET
```

### 7.2 Stripe Production Webhook

1. In Stripe Dashboard, switch to **Live Mode**
2. Navigate to **Developers → Webhooks**
3. Add endpoint: `https://api.yourdomain.com/api/v1/webhooks/stripe`
4. Select same events as test mode
5. Copy the signing secret and set as `STRIPE_WEBHOOK_SECRET`

### 7.3 Database Migration

Run Flyway migrations before deploying new code:

```bash
mvn flyway:migrate -Dflyway.url=<prod_db_url> -Dflyway.user=<user> -Dflyway.password=<password>
```

### 7.4 Health Checks

Verify application health:

```bash
curl https://api.yourdomain.com/actuator/health
```

Monitor payment metrics:

```bash
curl https://api.yourdomain.com/actuator/metrics/payment.success.count
curl https://api.yourdomain.com/actuator/metrics/payment.failure.count
```

---

## 8. Troubleshooting Production

### 8.1 Webhook Delivery Issues

Check Stripe Dashboard → Developers → Webhooks → Recent Events

If webhooks are failing:
1. Verify endpoint URL is accessible from internet
2. Check webhook signature verification logic
3. Review application logs for errors
4. Use Stripe's "Resend" feature to retry failed webhooks

### 8.2 Payment Processing Delays

If payments are slow:
1. Check circuit breaker state (may be open due to failures)
2. Verify database connection pool is not exhausted
3. Check Stripe API status: https://status.stripe.com
4. Review application performance metrics

---

## 9. Next Steps

After completing this quickstart:

1. ✅ Review [data-model.md](./data-model.md) for entity relationships
2. ✅ Review [contracts/payment-api.yaml](./contracts/payment-api.yaml) for API specifications
3. ✅ Implement email notifications for payment events (Feature 3.3)
4. ✅ Implement invoice generation (Feature 3.2)
5. ✅ Add rate limiting based on subscription plans (Feature 4.1)

---

## 10. Support and Resources

- **Stripe Documentation**: https://stripe.com/docs
- **Resilience4j Documentation**: https://resilience4j.readme.io
- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Project README**: [README.md](../../README.md)
- **API Documentation**: http://localhost:8080/swagger-ui.html

For questions or issues, contact the billing team or create a GitHub issue.
