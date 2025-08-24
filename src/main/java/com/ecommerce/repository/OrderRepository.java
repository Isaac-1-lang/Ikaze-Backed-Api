package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

       /**
        * Find orders by user ID
        */
       List<Order> findByUser_Id(UUID userId);

       /**
        * Find orders by status
        */
       List<Order> findByOrderStatus(Order.OrderStatus orderStatus);

       /**
        * Find orders by user ID and status
        */
       List<Order> findByUser_IdAndOrderStatus(UUID userId, Order.OrderStatus orderStatus);

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

       /**
        * Find all orders between two dates
        */
       @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
       List<Order> findAllBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       @Query("SELECT DISTINCT o FROM Order o " +
               "LEFT JOIN FETCH o.user u " +
               "LEFT JOIN FETCH o.orderItems oi " +
               "LEFT JOIN FETCH oi.productVariant v " +
               "LEFT JOIN FETCH v.product p " +
               "LEFT JOIN FETCH o.orderInfo info " +
               "LEFT JOIN FETCH o.orderAddress addr " +
               "LEFT JOIN FETCH o.orderTransaction tx " +
               "WHERE o.user.id = :userId")
       List<Order> findAllForUserWithDetails(@Param("userId") UUID userId);

               @Query("SELECT o FROM Order o " +
                "LEFT JOIN FETCH o.user u " +
                "LEFT JOIN FETCH o.orderItems oi " +
                "LEFT JOIN FETCH oi.productVariant v " +
                "LEFT JOIN FETCH v.product p " +
                "LEFT JOIN FETCH o.orderInfo info " +
                "LEFT JOIN FETCH o.orderAddress addr " +
                "LEFT JOIN FETCH o.orderTransaction tx " +
                "WHERE o.user.id = :userId AND o.orderId = :orderId")
        Optional<Order> findByIdForUserWithDetails(@Param("userId") UUID userId, @Param("orderId") Long orderId);

        /**
         * Find order by order code and user ID
         */
        Optional<Order> findByOrderCodeAndUser_Id(String orderCode, UUID userId);

}
