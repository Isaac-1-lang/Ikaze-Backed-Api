package com.ecommerce.repository;

import com.ecommerce.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long>, ShopOrderRepositoryCustom {
        Optional<ShopOrder> findByShopOrderCode(String shopOrderCode);

        Optional<ShopOrder> findByPickupToken(String pickupToken);

        // Find all shop orders for a specific shop
        @Query("SELECT so FROM ShopOrder so WHERE so.shop.shopId = :shopId")
        List<ShopOrder> findByShopId(@Param("shopId") UUID shopId);

        @Query("SELECT DISTINCT so FROM ShopOrder so " +
                        "LEFT JOIN FETCH so.items oi " +
                        "LEFT JOIN FETCH oi.product p " +
                        "LEFT JOIN FETCH so.order o " +
                        "LEFT JOIN FETCH o.user u " +
                        "LEFT JOIN FETCH o.orderInfo info " +
                        "LEFT JOIN FETCH o.orderAddress addr " +
                        "LEFT JOIN FETCH o.orderCustomerInfo customer " +
                        "LEFT JOIN FETCH o.orderTransaction tx " +
                        "WHERE o.orderId = :orderId AND so.shop.shopId = :shopId")
        Optional<ShopOrder> findByOrderIdAndShopIdWithDetails(@Param("orderId") Long orderId,
                        @Param("shopId") UUID shopId);

        List<ShopOrder> findByStatus(com.ecommerce.entity.ShopOrder.ShopOrderStatus status);

        long countByStatus(com.ecommerce.entity.ShopOrder.ShopOrderStatus status);

        long countByStatusAndReadyForDeliveryGroupIsNull(com.ecommerce.entity.ShopOrder.ShopOrderStatus status);

        @Query("SELECT so FROM ShopOrder so WHERE so.shop.shopId = :shopId AND so.createdAt BETWEEN :start AND :end")
        List<ShopOrder> findByShopIdAndCreatedAtBetween(@Param("shopId") UUID shopId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM ShopOrder so WHERE so.shop.shopId = :shopId AND so.createdAt BETWEEN :start AND :end")
        BigDecimal sumTotalAmountByShopIdAndCreatedAtBetween(@Param("shopId") UUID shopId,
                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(DISTINCT so.order.user.id) FROM ShopOrder so WHERE so.shop.shopId = :shopId AND so.createdAt BETWEEN :start AND :end AND so.order.user IS NOT NULL")
        long countDistinctCustomersByShopIdAndCreatedAtBetween(@Param("shopId") UUID shopId,
                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(so) FROM ShopOrder so WHERE so.shop.shopId = :shopId AND so.createdAt BETWEEN :start AND :end")
        long countByShopIdAndCreatedAtBetween(@Param("shopId") UUID shopId, @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(so) FROM ShopOrder so WHERE so.shop.shopId = :shopId")
        long countByShopId(@Param("shopId") UUID shopId);

        @Query("SELECT COUNT(DISTINCT so.order.user.id) FROM ShopOrder so WHERE so.shop.shopId = :shopId")
        long countDistinctCustomersByShopId(@Param("shopId") UUID shopId);

        @Query("SELECT COUNT(so) FROM ShopOrder so WHERE so.shop.shopId = :shopId AND so.status = :status")
        long countByShopIdAndStatus(@Param("shopId") UUID shopId,
                        @Param("status") com.ecommerce.entity.ShopOrder.ShopOrderStatus status);

        @Query("SELECT so FROM ShopOrder so WHERE so.shop.shopId = :shopId ORDER BY so.createdAt DESC")
        org.springframework.data.domain.Page<ShopOrder> findRecentOrdersByShopId(@Param("shopId") UUID shopId,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM ShopOrder so WHERE so.shop.shopId = :shopId")
        BigDecimal sumTotalRevenueByShopId(@Param("shopId") UUID shopId);

        // Fetch shop orders by delivery group with pagination
        org.springframework.data.domain.Page<ShopOrder> findByReadyForDeliveryGroup_DeliveryGroupId(
                        Long deliveryGroupId,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT COUNT(so) FROM ShopOrder so WHERE so.shop.shopId = :shopId AND so.status = :status AND so.readyForDeliveryGroup IS NULL")
        long countByShopIdAndStatusAndReadyForDeliveryGroupIsNull(@Param("shopId") UUID shopId,
                        @Param("status") com.ecommerce.entity.ShopOrder.ShopOrderStatus status);

        @Query("SELECT DISTINCT so FROM ShopOrder so " +
                        "LEFT JOIN FETCH so.items oi " +
                        "LEFT JOIN FETCH oi.product p " +
                        "LEFT JOIN FETCH so.order o " +
                        "LEFT JOIN FETCH o.user u " +
                        "LEFT JOIN FETCH o.orderInfo info " +
                        "LEFT JOIN FETCH o.orderAddress addr " +
                        "LEFT JOIN FETCH o.orderCustomerInfo customer " +
                        "LEFT JOIN FETCH o.orderTransaction tx " +
                        "WHERE so.shop.shopId = :shopId")
        org.springframework.data.domain.Page<ShopOrder> findAllWithDetailsByShopId(@Param("shopId") UUID shopId,
                        org.springframework.data.domain.Pageable pageable);
}
