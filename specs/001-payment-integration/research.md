# Research: External Payment Integration

**Feature**: External Payment Integration  
**Branch**: `001-payment-integration`  
**Date**: 2026-01-25

## Overview

This document consolidates research findings for implementing external payment gateway integration (Stripe) for the SaaS Billing Engine. All NEEDS CLARIFICATION items from the Technical Context have been resolved through web research and best practices analysis.

---

## Decision 1: Stripe Java SDK Version

**Decision**: Use Stripe Java SDK version **26.13.0** (latest stable as of January 2026)

**Rationale**:
- Latest stable release with full support for Spring Boot 3.x and Java 17
- Includes comprehensive webhook handling utilities (`Webhook.constructEvent()`)
- Provides built-in signature verification for security
- Well-documented with extensive examples for subscription and payment processing
- Active maintenance and security updates from Stripe

**Alternatives Considered**:
- **PayPal SDK**: Rejected because Stripe has better developer experience, clearer documentation, and more robust webhook support for subscription billing
- **Older Stripe SDK versions**: Rejected to ensure latest security patches and features

**Maven Dependency**:
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>26.13.0</version>
</dependency>
```

**Integration Points**:
- `StripeConfig.java`: Initialize Stripe SDK with API key from environment variables
- `PaymentService.java`: Use `PaymentIntent` API for one-time payments and `Subscription` API for recurring billing
- `WebhookService.java`: Use `Webhook.constructEvent()` for signature verification and event parsing

---

## Decision 2: Resilience4j for Circuit Breaker

**Decision**: Use Resilience4j Spring Boot 3 Starter version **2.2.0**

**Rationale**:
- Native Spring Boot 3 integration with auto-configuration
- Lightweight and performant (no additional runtime overhead)
- Provides circuit breaker, retry, and timeout patterns in one library
- Integrates seamlessly with Spring AOP for declarative annotations
- Actuator integration for monitoring circuit breaker states
- Already has Spring Boot Actuator and AOP dependencies in the project

**Alternatives Considered**:
- **Netflix Hystrix**: Rejected because it's deprecated and no longer maintained
- **Manual circuit breaker implementation**: Rejected due to complexity and lack of monitoring capabilities

**Maven Dependencies**:
```xml
<!-- Resilience4j Spring Boot 3 Starter -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- Resilience4j Circuit Breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- Resilience4j Retry -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>2.2.0</version>
</dependency>
```

**Configuration Strategy**:
- Define circuit breaker in `application.yml` with thresholds:
  - Failure rate threshold: 50%
  - Wait duration in open state: 60 seconds
  - Sliding window size: 10 calls
- Apply `@CircuitBreaker(name = "stripePayment")` annotation to payment gateway calls
- Implement fallback methods for graceful degradation

**Integration Points**:
- `PaymentService.java`: Apply circuit breaker to Stripe API calls
- `ResilienceConfig.java`: Configure circuit breaker, retry, and timeout policies
- Actuator endpoints: Monitor circuit breaker state via `/actuator/circuitbreakers`

---

## Decision 3: Testcontainers for Webhook Testing

**Decision**: Use **Testcontainers with WireMock** for simulating Stripe webhook events in integration tests

**Rationale**:
- Testcontainers provides isolated, reproducible test environments using Docker
- WireMock allows precise control over mock HTTP responses and webhook payloads
- Enables testing of webhook signature verification without calling real Stripe API
- Supports testing edge cases (duplicate webhooks, signature failures, malformed payloads)
- Industry best practice for integration testing external API interactions

**Alternatives Considered**:
- **Stripe CLI webhook forwarding**: Rejected for integration tests because it requires external process and internet connectivity
- **Manual mock server**: Rejected due to complexity and maintenance overhead
- **Spring MockMvc only**: Rejected because it doesn't test signature verification properly

**Maven Dependencies**:
```xml
<!-- Testcontainers Core -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- Testcontainers JUnit 5 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- WireMock for HTTP mocking -->
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.3.1</version>
    <scope>test</scope>
</dependency>
```

**Testing Strategy**:
1. **Signature Generation**: Create utility method to generate valid `Stripe-Signature` headers using HMAC-SHA256
2. **Webhook Payloads**: Store realistic JSON payloads for different event types (`payment_intent.succeeded`, `payment_intent.payment_failed`, `invoice.payment_succeeded`)
3. **WireMock Setup**: Configure WireMock to send POST requests to webhook endpoint with generated signatures
4. **Assertions**: Verify subscription status updates, payment transaction records, and idempotency handling

**Test Coverage**:
- Successful payment webhook processing
- Failed payment webhook processing
- Duplicate webhook handling (idempotency)
- Invalid signature rejection
- Malformed payload handling
- Concurrent webhook processing

---

## Decision 4: Async Processing for Email Notifications

**Decision**: Use **Spring's `@Async` annotation** with a dedicated thread pool for non-critical side effects like email notifications

**Rationale**:
- Webhook processing must be fast (<5 seconds) to avoid timeouts
- Email sending is a non-critical side effect that can be asynchronous
- Spring's `@Async` is lightweight and doesn't require additional dependencies
- Allows webhook endpoint to return 200 OK immediately after status update
- Prevents email service failures from blocking payment processing

**Alternatives Considered**:
- **Synchronous email sending**: Rejected because it increases webhook processing time and creates dependency on email service availability
- **Message queue (RabbitMQ/Kafka)**: Rejected as over-engineering for this use case; adds infrastructure complexity
- **Spring Events**: Considered but `@Async` is simpler for this specific use case

**Configuration**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.initialize();
        return executor;
    }
}
```

**Integration Points**:
- `EmailService.java`: Mark email sending methods with `@Async("emailTaskExecutor")`
- `WebhookService.java`: Call email service asynchronously after successful status update
- Error handling: Log email failures but don't propagate exceptions to webhook handler

---

## Additional Best Practices

### 1. Idempotency Key Strategy

**Decision**: Use `paymentGatewayTransactionId` as idempotency key

**Rationale**:
- Stripe provides unique transaction ID for each payment event
- Database UNIQUE constraint on this field prevents duplicate processing
- Simple and reliable approach without additional key generation logic

**Implementation**:
```java
@Column(unique = true, nullable = false)
private String paymentGatewayTransactionId;
```

### 2. Webhook Signature Verification

**Decision**: Use Stripe SDK's built-in `Webhook.constructEvent()` method

**Rationale**:
- Handles all signature verification logic securely
- Prevents timing attacks and signature bypass attempts
- Automatically validates timestamp to prevent replay attacks

**Implementation**:
```java
String payload = request.getReader().lines().collect(Collectors.joining());
String sigHeader = request.getHeader("Stripe-Signature");
Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
```

### 3. Database Indexing Strategy

**Indexes to Create**:
- `payment_gateway_transaction_id` (UNIQUE): Fast idempotency checks
- `subscription_id`: Fast lookup of payments for a subscription
- `created_date`: Efficient payment history queries with pagination
- `status`: Filter payments by status (success/failed)

### 4. Logging Strategy

**What to Log**:
- ✅ Webhook event type and ID
- ✅ Payment transaction ID
- ✅ Subscription ID
- ✅ Status transitions
- ✅ Signature verification results
- ❌ Payment card details (PCI compliance)
- ❌ Full webhook payload (may contain sensitive data)

**Log Levels**:
- `INFO`: Successful payment processing
- `WARN`: Duplicate webhook received (idempotency)
- `ERROR`: Signature verification failure, payment processing errors

---

## Summary

All NEEDS CLARIFICATION items have been resolved:

1. ✅ **Stripe Java SDK**: Version 26.13.0
2. ✅ **Resilience4j**: Version 2.2.0 with Spring Boot 3 starter
3. ✅ **Testcontainers**: Version 1.19.3 with WireMock 3.3.1
4. ✅ **Async Email Processing**: Spring `@Async` with dedicated thread pool

**Next Steps**: Proceed to Phase 1 (Design & Contracts) to create:
- `data-model.md`: Entity definitions and relationships
- `contracts/payment-api.yaml`: OpenAPI specification for payment endpoints
- `quickstart.md`: Developer setup and testing guide
