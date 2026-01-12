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
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

       /**
        * Find order by ID with all associations eagerly fetched for refund calculation
        * This prevents LazyInitializationException when calculating refunds outside
        * transaction
        */
       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH oi.productVariant pv " +
                     "LEFT JOIN FETCH pv.product p " +
                     "LEFT JOIN FETCH o.orderTransaction " +
                     "LEFT JOIN FETCH o.orderInfo " +
                     "WHERE o.orderId = :orderId")
       Optional<Order> findByIdWithAllAssociations(@Param("orderId") Long orderId);

       /**
        * Calculate total quantity of items in an order without loading collections
        */
       @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
                     "JOIN oi.shopOrder so " +
                     "JOIN so.order o " +
                     "WHERE o.orderId = :orderId")
       int getTotalQuantityByOrderId(@Param("orderId") Long orderId);

       /**
        * Find orders by user ID
        */
       List<Order> findByUser_Id(UUID userId);

       /**
        * Check if there are any orders with the given product variant that are not
        * delivered
        */
       @Query("SELECT COUNT(o) > 0 FROM Order o " +
                     "JOIN o.shopOrders so " +
                     "JOIN so.items oi " +
                     "WHERE oi.productVariant.id = :variantId " +
                     "AND so.status NOT IN ('DELIVERED', 'CANCELLED', 'REFUNDED')")
       boolean existsByProductVariantAndNotDelivered(@Param("variantId") Long variantId);

       @Query("SELECT o FROM Order o " +
                     "JOIN o.shopOrders so " +
                     "JOIN so.items oi " +
                     "WHERE oi.productVariant.id = :variantId")
       List<Order> findByProductVariant(@Param("variantId") Long variantId);

       /**
        * Find all orders between two dates
        */
       @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
       List<Order> findAllBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       /**
        * Find orders by date range
        */
       @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
       List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH o.orderInfo info " +
                     "LEFT JOIN FETCH o.orderAddress addr " +
                     "LEFT JOIN FETCH o.orderTransaction tx " +
                     "WHERE o.user.id = :userId")
       List<Order> findAllForUserWithDetails(@Param("userId") UUID userId);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH o.orderInfo info " +
                     "LEFT JOIN FETCH o.orderAddress addr " +
                     "LEFT JOIN FETCH o.orderTransaction tx " +
                     "WHERE o.user.id = :userId AND o.orderId = :orderId")
       Optional<Order> findByIdForUserWithDetails(@Param("userId") UUID userId, @Param("orderId") Long orderId);

       /**
        * Find order by order code and user ID
        */
       Optional<Order> findByOrderCodeAndUser_Id(String orderCode, UUID userId);

       /**
        * Find order by order code
        */
       Optional<Order> findByOrderCode(String orderCode);

       /**
        * Find all orders with all details for admin (includes all relationships except
        * images)
        */
       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH o.orderInfo info " +
                     "LEFT JOIN FETCH o.orderAddress addr " +
                     "LEFT JOIN FETCH o.orderCustomerInfo customer " +
                     "LEFT JOIN FETCH o.orderTransaction tx")
       List<Order> findAllWithDetailsForAdmin();

       /**
        * Find all orders with all details for admin filtered by shop
        * Orders are filtered by checking if any order item has a product from the
        * specified shop
        */
       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH oi.product p " +
                     "LEFT JOIN FETCH o.orderInfo info " +
                     "LEFT JOIN FETCH o.orderAddress addr " +
                     "LEFT JOIN FETCH o.orderCustomerInfo customer " +
                     "LEFT JOIN FETCH o.orderTransaction tx " +
                     "WHERE p.shop.shopId = :shopId")
       Page<Order> findAllWithDetailsForAdminByShop(@Param("shopId") UUID shopId, Pageable pageable);

       /**
        * Find all orders with all details for admin with pagination
        */
       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH o.orderInfo info " +
                     "LEFT JOIN FETCH o.orderAddress addr " +
                     "LEFT JOIN FETCH o.orderCustomerInfo customer " +
                     "LEFT JOIN FETCH o.orderTransaction tx")
       Page<Order> findAllWithDetailsForAdmin(Pageable pageable);

       /**
        * Find order by ID with all details for admin (includes all relationships
        * except images)
        */
       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH o.orderInfo info " +
                     "LEFT JOIN FETCH o.orderAddress addr " +
                     "LEFT JOIN FETCH o.orderCustomerInfo customer " +
                     "LEFT JOIN FETCH o.orderTransaction tx " +
                     "WHERE o.orderId = :orderId")
       Optional<Order> findByIdWithDetailsForAdmin(@Param("orderId") Long orderId);

       /**
        * Find order by order code with all details for admin
        */
       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.user u " +
                     "LEFT JOIN FETCH o.shopOrders so " +
                     "LEFT JOIN FETCH so.items oi " +
                     "LEFT JOIN FETCH o.orderInfo info " +
                     "LEFT JOIN FETCH o.orderAddress addr " +
                     "LEFT JOIN FETCH o.orderCustomerInfo customer " +
                     "LEFT JOIN FETCH o.orderTransaction tx " +
                     "WHERE o.orderCode = :orderCode")
       Optional<Order> findByOrderCodeWithDetailsForAdmin(@Param("orderCode") String orderCode);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.orderTransaction tx " +
                     "LEFT JOIN FETCH o.user u " +
                     "WHERE tx.status = 'PENDING' " +
                     "AND o.createdAt < :cutoffTime " +
                     "ORDER BY o.createdAt ASC")
       List<Order> findAbandonedPendingOrders(@Param("cutoffTime") LocalDateTime cutoffTime,
                     Pageable pageable);

       /**
        * Count abandoned pending orders older than the specified cutoff time
        */
       @Query("SELECT COUNT(o) FROM Order o " +
                     "LEFT JOIN o.orderTransaction tx " +
                     "WHERE tx.status = 'PENDING' " +
                     "AND o.createdAt < :cutoffTime")
       long countAbandonedPendingOrders(@Param("cutoffTime") LocalDateTime cutoffTime);

       /**
        * Check if a specific order is considered abandoned
        */
       @Query("SELECT COUNT(o) > 0 FROM Order o " +
                     "LEFT JOIN o.orderTransaction tx " +
                     "WHERE o.orderId = :orderId " +
                     "AND tx.status = 'PENDING' " +
                     "AND o.createdAt < :cutoffTime")
       boolean isOrderAbandoned(@Param("orderId") Long orderId, @Param("cutoffTime") LocalDateTime cutoffTime);

       /**
        * Find orders by customer email
        */
       @Query("SELECT o FROM Order o JOIN o.orderCustomerInfo ci WHERE LOWER(ci.email) = LOWER(:email)")
       List<Order> findByCustomerInfoEmail(@Param("email") String email);

       /**
        * Find orders by customer email with pagination
        */
       @Query("SELECT o FROM Order o JOIN o.orderCustomerInfo ci WHERE LOWER(ci.email) = LOWER(:email) ORDER BY o.createdAt DESC")
       Page<Order> findByCustomerInfoEmailWithPagination(@Param("email") String email, Pageable pageable);

       /**
        * Count orders for a specific shop (orders containing products from that shop)
        */
       @Query("SELECT COUNT(DISTINCT o) FROM Order o " +
                     "JOIN o.shopOrders so " +
                     "JOIN so.items oi " +
                     "JOIN oi.productVariant pv " +
                     "JOIN pv.product p " +
                     "WHERE p.shop.shopId = :shopId")
       long countByShopId(@Param("shopId") UUID shopId);

       /**
        * Count distinct customers who have placed orders for a specific shop
        */
       @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o " +
                     "JOIN o.shopOrders so " +
                     "JOIN so.items oi " +
                     "JOIN oi.productVariant pv " +
                     "JOIN pv.product p " +
                     "WHERE p.shop.shopId = :shopId AND o.user IS NOT NULL")
       long countDistinctCustomersByShopId(@Param("shopId") UUID shopId);

       /**
        * Count pending orders for a specific shop
        */
       @Query("SELECT COUNT(DISTINCT o) FROM Order o " +
                     "JOIN o.shopOrders so " +
                     "JOIN so.items oi " +
                     "JOIN oi.productVariant pv " +
                     "JOIN pv.product p " +
                     "WHERE p.shop.shopId = :shopId AND so.status = 'PENDING'")
       long countPendingOrdersByShopId(@Param("shopId") UUID shopId);

       /**
        * Find recent orders for a specific shop
        */
       @Query("SELECT DISTINCT o FROM Order o " +
                     "JOIN FETCH o.user u " +
                     "JOIN FETCH o.orderTransaction tx " +
                     "JOIN FETCH o.orderInfo info " +
                     "JOIN o.shopOrders so " +
                     "JOIN so.items oi " +
                     "JOIN oi.productVariant pv " +
                     "JOIN pv.product p " +
                     "WHERE p.shop.shopId = :shopId " +
                     "ORDER BY o.createdAt DESC")
       List<Order> findRecentOrdersByShopId(@Param("shopId") UUID shopId, Pageable pageable);

}
