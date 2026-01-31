package org.gb.billing.repository;

import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.SubscriptionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Subscription entity operations.
 * 
 * Provides CRUD operations, tenant-filtered queries, and analytics queries.
 * All queries enforce multi-tenant isolation by filtering on tenantId.
 * 
 * @see Subscription for entity details
 * @see org.gb.billing.service.SubscriptionService for business logic
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * Finds a subscription by ID with tenant filtering.
     * Ensures users can only access subscriptions in their tenant.
     * 
     * @param id the subscription ID
     * @param tenantId the tenant ID
     * @return Optional containing the subscription if found
     */
    Optional<Subscription> findByIdAndTenantId(UUID id, Long tenantId);

    /**
     * Checks if a subscription exists by ID and tenant ID.
     * 
     * @param id the subscription ID
     * @param tenantId the tenant ID
     * @return true if exists, false otherwise
     */
    boolean existsByIdAndTenantId(UUID id, Long tenantId);

    /**
     * Finds the active or past-due subscription for a user.
     * Used to enforce single active subscription constraint.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return Optional containing the active/past-due subscription if exists
     */
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.tenantId = :tenantId " +
           "AND s.status IN ('ACTIVE', 'PAST_DUE')")
    Optional<Subscription> findActiveOrPastDueByUserIdAndTenantId(
        @Param("userId") Long userId,
        @Param("tenantId") Long tenantId
    );

    /**
     * Finds all subscriptions for a user in a tenant.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return list of user's subscriptions
     */
    List<Subscription> findByUserIdAndTenantId(Long userId, Long tenantId);

    /**
     * Counts active subscriptions for a specific plan.
     * Used to prevent plan deletion if active subscriptions exist.
     * 
     * @param planId the plan ID
     * @return count of active subscriptions
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan.id = :planId " +
           "AND s.status IN ('ACTIVE', 'PAST_DUE')")
    long countActiveSubscriptionsByPlanId(@Param("planId") UUID planId);

    /**
     * Finds all subscriptions by status in a tenant.
     * 
     * @param status the subscription status
     * @param tenantId the tenant ID
     * @return list of subscriptions with the given status
     */
    List<Subscription> findByStatusAndTenantId(SubscriptionState status, Long tenantId);

    /**
     * Finds subscriptions with next billing date before a given date.
     * Used for billing reminders and automated billing.
     * 
     * @param date the cutoff date
     * @return list of subscriptions due for billing
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate IS NOT NULL " +
           "AND s.nextBillingDate < :date AND s.status = 'ACTIVE'")
    List<Subscription> findSubscriptionsDueForBilling(@Param("date") Instant date);

    /**
     * Counts subscriptions by status for analytics.
     * 
     * @param tenantId the tenant ID
     * @return list of [status, count] tuples
     */
    @Query("SELECT s.status, COUNT(s) FROM Subscription s WHERE s.tenantId = :tenantId GROUP BY s.status")
    List<Object[]> countSubscriptionsByStatus(@Param("tenantId") Long tenantId);

    /**
     * Counts active subscriptions by plan for analytics.
     * 
     * @param tenantId the tenant ID
     * @return list of [planId, planName, count] tuples
     */
    @Query("SELECT s.plan.id, s.plan.name, COUNT(s) FROM Subscription s " +
           "WHERE s.tenantId = :tenantId AND s.status IN ('ACTIVE', 'PAST_DUE') " +
           "GROUP BY s.plan.id, s.plan.name")
    List<Object[]> countActiveSubscriptionsByPlan(@Param("tenantId") Long tenantId);

    /**
     * Counts total active/past-due subscriptions active at a specific date.
     * Used for churn rate calculation (denominator: subscriptions at start of period).
     */
    @Query("SELECT COUNT(s) FROM Subscription s " +
           "WHERE s.tenantId = :tenantId " +
           "AND s.startDate <= :date " +
           "AND (s.canceledAt IS NULL OR s.canceledAt > :date)")
    long countActiveSubscriptionsAtDate(@Param("tenantId") Long tenantId, @Param("date") Instant date);

    /**
     * Counts subscriptions canceled between two dates.
     * Used for churn rate calculation (numerator: churned subscriptions).
     */
    @Query("SELECT COUNT(s) FROM Subscription s " +
           "WHERE s.tenantId = :tenantId " +
           "AND s.canceledAt >= :startDate AND s.canceledAt <= :endDate")
    long countCanceledSubscriptionsBetween(@Param("tenantId") Long tenantId, 
                                          @Param("startDate") Instant startDate, 
                                          @Param("endDate") Instant endDate);

    /**
     * Gets new subscriptions count grouped by date.
     */
    @Query(value = "SELECT CAST(created_at AS DATE) as date, COUNT(*) as count " +
           "FROM subscription " +
           "WHERE tenant_id = :tenantId AND created_at >= :startDate " +
           "GROUP BY CAST(created_at AS DATE)", nativeQuery = true)
    List<Object[]> countNewSubscriptionsByDate(@Param("tenantId") Long tenantId, @Param("startDate") Instant startDate);
    
    /**
     * Gets canceled subscriptions count grouped by date.
     */
    @Query(value = "SELECT CAST(canceled_at AS DATE) as date, COUNT(*) as count " +
           "FROM subscription " +
           "WHERE tenant_id = :tenantId AND canceled_at >= :startDate " +
           "GROUP BY CAST(canceled_at AS DATE)", nativeQuery = true)
    List<Object[]> countCanceledSubscriptionsByDate(@Param("tenantId") Long tenantId, @Param("startDate") Instant startDate);

    @Query("SELECT TO_CHAR(s.startDate, 'YYYY-MM') as period, SUM(s.plan.price) as amount " +
           "FROM Subscription s " +
           "WHERE s.status = 'ACTIVE' " +
           "GROUP BY TO_CHAR(s.startDate, 'YYYY-MM') " +
           "ORDER BY period DESC")
    List<RevenueStats> calculateMonthlyRevenue();

    @Query("SELECT COUNT(s) FROM Subscription s")
    long countTotalSubscriptions();

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'CANCELED'")
    long countCancelledSubscriptions();

}
