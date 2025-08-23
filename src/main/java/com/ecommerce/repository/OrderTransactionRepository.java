package com.ecommerce.repository;

import com.ecommerce.entity.OrderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Long> {
    Optional<OrderTransaction> findByStripeSessionId(String sessionId);
    
    /**
     * Sum all completed transaction amounts
     */
    @Query("SELECT COALESCE(SUM(ot.orderAmount), 0) FROM OrderTransaction ot WHERE ot.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenue();
}
