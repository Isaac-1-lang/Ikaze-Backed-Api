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

    @Query("SELECT oib FROM OrderItemBatch oib JOIN FETCH oib.warehouse WHERE oib.orderItem.order.orderId = :orderId")
    List<OrderItemBatch> findByOrderIdWithWarehouse(@Param("orderId") Long orderId);

    @Query("SELECT SUM(oib.quantityUsed) FROM OrderItemBatch oib WHERE oib.stockBatch.id = :stockBatchId")
    Integer getTotalQuantityUsedFromBatch(@Param("stockBatchId") Long stockBatchId);

    /**
     * Find all batches used for a specific return item (via OrderItem)
     */
    @Query("SELECT oib FROM OrderItemBatch oib " +
           "JOIN oib.orderItem oi " +
           "JOIN ReturnItem ri ON ri.orderItem.orderItemId = oi.orderItemId " +
           "WHERE ri.id = :returnItemId")
    List<OrderItemBatch> findByReturnItemId(@Param("returnItemId") Long returnItemId);

    /**
     * Find batches for multiple return items
     */
    @Query("SELECT oib FROM OrderItemBatch oib " +
           "JOIN oib.orderItem oi " +
           "JOIN ReturnItem ri ON ri.orderItem.orderItemId = oi.orderItemId " +
           "WHERE ri.id IN :returnItemIds")
    List<OrderItemBatch> findByReturnItemIds(@Param("returnItemIds") List<Long> returnItemIds);
}
