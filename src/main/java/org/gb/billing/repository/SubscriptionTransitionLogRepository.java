package org.gb.billing.repository;

import org.gb.billing.entity.SubscriptionTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for SubscriptionTransitionLog entity operations.
 * 
 * Provides audit trail queries for subscription state transitions.
 * Used for compliance, debugging, and analytics.
 * 
 * @see SubscriptionTransitionLog for entity details
 * @see org.gb.billing.service.SubscriptionService for business logic
 */
@Repository
public interface SubscriptionTransitionLogRepository extends JpaRepository<SubscriptionTransitionLog, UUID> {

    /**
     * Finds all transition logs for a subscription, ordered by transition time (newest first).
     * 
     * @param subscriptionId the subscription ID
     * @return list of transition logs
     */
    @Query("SELECT t FROM SubscriptionTransitionLog t WHERE t.subscriptionId = :subscriptionId " +
           "ORDER BY t.transitionedAt DESC")
    List<SubscriptionTransitionLog> findBySubscriptionIdOrderByTransitionedAtDesc(
        @Param("subscriptionId") UUID subscriptionId
    );

    /**
     * Finds the most recent transition log for a subscription.
     * 
     * @param subscriptionId the subscription ID
     * @return the most recent transition log
     */
    @Query("SELECT t FROM SubscriptionTransitionLog t WHERE t.subscriptionId = :subscriptionId " +
           "ORDER BY t.transitionedAt DESC LIMIT 1")
    SubscriptionTransitionLog findMostRecentBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    /**
     * Counts transitions to a specific status.
     * Used for analytics (e.g., how many subscriptions were canceled).
     * 
     * @param toStatus the target status
     * @return count of transitions
     */
    @Query("SELECT COUNT(t) FROM SubscriptionTransitionLog t WHERE t.toStatus = :toStatus")
    long countByToStatus(@Param("toStatus") String toStatus);
}
