package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

       /**
        * Find orders by user ID
        */
       List<Order> findByUserId(UUID userId);

       Optional<Order> findByOrderCode(String orderCode);

       Optional<Order> findByOrderTransaction_StripeSessionId(String sessionId);

       /**
        * Find orders by status
        */
       List<Order> findByOrderStatus(Order.OrderStatus orderStatus);

       /**
        * Find orders by user ID and status
        */
       List<Order> findByUserIdAndOrderStatus(UUID userId, Order.OrderStatus orderStatus);

       /**
        * Check if there are any orders with the given product variant that are not
        * delivered
        */
       @Query("SELECT COUNT(o) > 0 FROM Order o " +
                     "JOIN o.orderItems oi " +
                     "WHERE oi.productVariant.id = :variantId " +
                     "AND o.orderStatus NOT IN ('DELIVERED', 'CANCELLED', 'REFUNDED', 'RETURNED')")
       boolean existsByProductVariantAndNotDelivered(@Param("variantId") Long variantId);

       /**
        * Find all orders that contain a specific product variant
        */
       @Query("SELECT o FROM Order o " +
                     "JOIN o.orderItems oi " +
                     "WHERE oi.productVariant.id = :variantId")
       List<Order> findByProductVariant(@Param("variantId") Long variantId);
}
