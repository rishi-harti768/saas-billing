# billing Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-24

## Active Technologies
<<<<<<< HEAD
- Java 17 + Spring Boot 3.2.2, Bucket4j 8.7.0, Caffeine (existing), Spring Security 6.x (003-performance-advanced-utilities)
- PostgreSQL (existing, no schema changes required) (003-performance-advanced-utilities)
=======
- Java 17 + Spring Boot 3.2.2, Spring Security 6.x, Spring Data JPA, PostgreSQL, Stripe Java SDK (NEEDS CLARIFICATION: version), Resilience4j (for circuit breaker pattern) (001-payment-integration)
- PostgreSQL (existing database with Flyway migrations) (001-payment-integration)
>>>>>>> sameer

- Java 17+ + Spring Boot 3.x, Spring Security 6.x, Spring Data JPA, jjwt (JWT library), Hibernate Validator (001-auth-multi-tenancy)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 17+

## Code Style

Java 17+: Follow standard conventions

## Recent Changes
<<<<<<< HEAD
- 003-performance-advanced-utilities: Added Java 17 + Spring Boot 3.2.2, Bucket4j 8.7.0, Caffeine (existing), Spring Security 6.x
=======
- 001-payment-integration: Added Java 17 + Spring Boot 3.2.2, Spring Security 6.x, Spring Data JPA, PostgreSQL, Stripe Java SDK (NEEDS CLARIFICATION: version), Resilience4j (for circuit breaker pattern)
>>>>>>> sameer

- 001-auth-multi-tenancy: Added Java 17+ + Spring Boot 3.x, Spring Security 6.x, Spring Data JPA, jjwt (JWT library), Hibernate Validator

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
