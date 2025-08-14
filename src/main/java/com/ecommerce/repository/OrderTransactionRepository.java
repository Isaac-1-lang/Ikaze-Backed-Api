package com.ecommerce.repository;

import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.OrderTransaction.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Long> {

    long countByStatus(TransactionStatus status);

    @Query("select coalesce(sum(ot.orderAmount), 0) from OrderTransaction ot where ot.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenue();
}
