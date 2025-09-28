package com.ecommerce.repository;

import com.ecommerce.entity.OrderItemBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemBatchRepository extends JpaRepository<OrderItemBatch, Long> {

    List<OrderItemBatch> findByOrderItem_OrderItemId(Long orderItemId);

    List<OrderItemBatch> findByStockBatch_Id(Long stockBatchId);

    List<OrderItemBatch> findByWarehouse_Id(Long warehouseId);

    @Query("SELECT oib FROM OrderItemBatch oib WHERE oib.orderItem.order.orderId = :orderId")
    List<OrderItemBatch> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT SUM(oib.quantityUsed) FROM OrderItemBatch oib WHERE oib.stockBatch.id = :stockBatchId")
    Integer getTotalQuantityUsedFromBatch(@Param("stockBatchId") Long stockBatchId);
}
