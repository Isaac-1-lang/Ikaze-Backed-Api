package com.ecommerce.repository;

import com.ecommerce.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ReturnItem entity
 */
@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

    /**
     * Find all return items for a specific return request
     */
    List<ReturnItem> findByReturnRequestId(Long returnRequestId);

    /**
     * Find all return items for a specific order item
     */
    List<ReturnItem> findByOrderItemOrderItemId(Long orderItemId);

    /**
     * Check if a specific order item has any return requests
     */
    boolean existsByOrderItemOrderItemId(Long orderItemId);

    /**
     * Get total quantity being returned for a specific order item
     */
    @Query("SELECT COALESCE(SUM(ri.returnQuantity), 0) FROM ReturnItem ri WHERE ri.orderItem.orderItemId = :orderItemId")
    Integer getTotalReturnQuantityForOrderItem(@Param("orderItemId") Long orderItemId);

    /**
     * Find return items by product ID
     */
    @Query("SELECT ri FROM ReturnItem ri WHERE ri.product.productId = :productId")
    List<ReturnItem> findByProductId(@Param("productId") Long productId);

    /**
     * Find return items by variant ID
     */
    @Query("SELECT ri FROM ReturnItem ri WHERE ri.productVariant.id = :variantId")
    List<ReturnItem> findByVariantId(@Param("variantId") Long variantId);

    /**
     * Find all return items for a specific return request (alternative method name)
     */
    List<ReturnItem> findByReturnRequest_Id(Long returnRequestId);
    
    /**
     * Count distinct return requests that include a specific order item
     * Used to enforce the maximum 2 return requests per item limit
     */
    @Query("SELECT COUNT(DISTINCT ri.returnRequest.id) FROM ReturnItem ri WHERE ri.orderItem.orderItemId = :orderItemId")
    Long countDistinctReturnRequestsByOrderItemId(@Param("orderItemId") Long orderItemId);
    
    /**
     * Get total returned quantity for an order item across all approved/completed return requests
     */
    @Query("SELECT COALESCE(SUM(ri.returnQuantity), 0) FROM ReturnItem ri " +
           "WHERE ri.orderItem.orderItemId = :orderItemId " +
           "AND ri.returnRequest.status IN ('APPROVED', 'COMPLETED')")
    Integer getTotalApprovedReturnQuantityForOrderItem(@Param("orderItemId") Long orderItemId);
}
