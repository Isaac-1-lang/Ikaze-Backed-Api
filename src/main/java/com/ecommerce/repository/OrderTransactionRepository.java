package com.ecommerce.repository;

import com.ecommerce.entity.OrderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Long> {
    Optional<OrderTransaction> findByStripeSessionId(String sessionId);
    
    /**
     * Find OrderTransaction by Stripe session ID with only basic order info (no collections)
     * This avoids MultipleBagFetchException by not loading nested collections
     */
    @Query("SELECT ot FROM OrderTransaction ot JOIN FETCH ot.order o WHERE ot.stripeSessionId = :sessionId")
    Optional<OrderTransaction> findByStripeSessionIdWithOrder(String sessionId);
    
    /**
     * Sum all completed transaction amounts
     */
    @Query("SELECT COALESCE(SUM(ot.orderAmount), 0) FROM OrderTransaction ot WHERE ot.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenue();
}
