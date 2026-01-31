# Research: Analytics & Documentation

**Status**: Complete
**Confimed Dependencies**:
- Spring Data JPA (for JPQL)
- SpringDoc OpenAPI (for Documentation)

## Decisions

### 1. Analytics Aggregation Strategy
**Decision**: Use optimized JPQL queries in `SubscriptionRepository`.
**Rationale**: 
- Avoids fetching all records into memory (which would happen if we used `findAll()` and filtered in Java).
- `SUM()` and `GROUP BY` in database are much faster.
- JPA Projections (Interfaces) can be used to map results to DTOs without full Entity overhead.
**Alternatives Considered**:
- **Java-side aggregation**: Rejected due to performance concerns with large datasets (O(n) memory).
- **Native SQL**: Possible, but JPQL is preferred for portability and type safety where possible.

### 2. API Documentation
**Decision**: Use library `springdoc-openapi-starter-webmvc-ui` (already in pom.xml).
**Rationale**: Standard for Spring Boot 3.x. Automatically generates Swagger UI from `@RestController` and `@Schema` annotations.
**Action**: Ensure `OpenApiConfig` class exists to customize the API Info (Title, Version, Description).

### 3. Security for Analytics
**Decision**: Use `PreAuthorize("hasRole('ADMIN')")` on the new `AnalyticsController`.
**Rationale**: Enforces strict RBAC at the method level.

## Validation Strategy
- **Unit**: Verify JPQL queries using `@DataJpaTest`.
- **Integration**: Verify Endpoint security and JSON response structure.
