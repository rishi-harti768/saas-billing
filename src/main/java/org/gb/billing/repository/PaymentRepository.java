package org.gb.billing.repository;

import org.gb.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    List<Payment> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);
    
    List<Payment> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
    
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
}
