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
    
    @Query("SELECT ot FROM OrderTransaction ot JOIN FETCH ot.order o WHERE ot.stripeSessionId = :sessionId")
    Optional<OrderTransaction> findByStripeSessionIdWithOrder(String sessionId);
    
    @Query("SELECT COALESCE(SUM(ot.orderAmount), 0) FROM OrderTransaction ot WHERE ot.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenue();
    
    @Query("SELECT COALESCE(SUM(ot.orderAmount), 0) FROM OrderTransaction ot " +
           "WHERE ot.status = 'COMPLETED' OR ot.status = 'REFUNDED'")
    BigDecimal sumCompletedAndRefundedRevenue();

    /**
     * Get total revenue for a specific shop (sum of completed transactions for orders containing products from that shop)
     */
    @Query("SELECT COALESCE(SUM(ot.orderAmount), 0) FROM OrderTransaction ot " +
           "JOIN ot.order o " +
           "JOIN o.orderItems oi " +
           "JOIN oi.productVariant pv " +
           "JOIN pv.product p " +
           "WHERE p.shop.shopId = :shopId " +
           "AND ot.status = 'COMPLETED'")
    BigDecimal getTotalRevenueByShopId(java.util.UUID shopId);
}
