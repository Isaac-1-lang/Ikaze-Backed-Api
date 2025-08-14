package com.ecommerce.repository;

import com.ecommerce.entity.OrderTransaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Long> {
    Optional<OrderTransaction> findByStripeSessionId(String sessionId);

    @Query("select coalesce(sum(ot.orderAmount), 0) from OrderTransaction ot where ot.status = 'COMPLETED'")
    java.math.BigDecimal sumCompletedRevenue();
}
