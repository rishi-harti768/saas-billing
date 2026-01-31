# Data Model: Analytics & Documentation

## Entities (ReadOnly Access)

We will query the existing `Subscription` entity. No new DB tables required.

### Subscription Entity (Ref)
- `id` (Long)
- `plan_price` (BigDecimal)
- `status` (Enum: ACTIVE, CANCELLED, etc)
- `created_at` (LocalDateTime)

## DTOs

### 1. RevenueStats (Interface Projection)
Used for JPQL result mapping.
```java
public interface RevenueStats {
    String getPeriod(); // e.g. "2023-10"
    BigDecimal getAmount();
}
```

### 2. ChurnStats (Class DTO)
Used for JSON response.
```java
public record ChurnStats(
    long totalSubscribers,
    long cancelledSubscribers,
    double churnRatePercentage
) {}
```
**Validation**: None (Output only)

### 3. MrrStats (Class DTO)
Used for JSON response.
```java
public record MrrStats(
    BigDecimal totalMrr,
    List<RevenueStats> monthlyBreakdown
) {}
```
**Validation**: None (Output only)
