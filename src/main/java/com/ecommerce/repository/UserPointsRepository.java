package com.ecommerce.repository;

import com.ecommerce.entity.UserPoints;
import com.ecommerce.entity.UserPoints.PointsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {

    /**
     * Find all points transactions for a user
     */
    List<UserPoints> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find points transactions for a user with pagination
     */
    Page<UserPoints> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find points transactions by points type for a user
     */
    List<UserPoints> findByUserIdAndPointsTypeOrderByCreatedAtDesc(UUID userId, PointsType pointsType);

    /**
     * Find points transactions by order
     */
    List<UserPoints> findByOrderId(Long orderId);

    /**
     * Calculate total points earned by a user
     */
    @Query("SELECT COALESCE(SUM(up.points), 0) FROM UserPoints up WHERE up.user.id = :userId AND up.points > 0")
    Integer calculateTotalPointsEarned(@Param("userId") UUID userId);

    /**
     * Calculate total points spent by a user
     */
    @Query("SELECT COALESCE(ABS(SUM(up.points)), 0) FROM UserPoints up WHERE up.user.id = :userId AND up.points < 0")
    Integer calculateTotalPointsSpent(@Param("userId") UUID userId);

    /**
     * Calculate current point balance for a user - DEPRECATED
     * Use User.points field instead for better performance
     */
    @Deprecated
    @Query("SELECT COALESCE(SUM(up.points), 0) FROM UserPoints up WHERE up.user.id = :userId")
    Integer calculateCurrentBalance(@Param("userId") UUID userId);

    /**
     * Calculate current point balance for a user scoped to a specific shop
     */
    @Query("SELECT COALESCE(SUM(up.points), 0) FROM UserPoints up WHERE up.user.id = :userId AND up.shop.shopId = :shopId")
    Integer calculateCurrentBalanceByShop(@Param("userId") UUID userId, @Param("shopId") UUID shopId);

    /**
     * Calculate total points earned by a user for a specific shop
     */
    @Query("SELECT COALESCE(SUM(up.points), 0) FROM UserPoints up WHERE up.user.id = :userId AND up.shop.shopId = :shopId AND up.points > 0")
    Integer calculateTotalPointsEarnedByShop(@Param("userId") UUID userId, @Param("shopId") UUID shopId);

    /**
     * Calculate total points spent by a user for a specific shop
     */
    @Query("SELECT COALESCE(ABS(SUM(up.points)), 0) FROM UserPoints up WHERE up.user.id = :userId AND up.shop.shopId = :shopId AND up.points < 0")
    Integer calculateTotalPointsSpentByShop(@Param("userId") UUID userId, @Param("shopId") UUID shopId);

    /**
     * Find points transactions within a date range
     */
    List<UserPoints> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate);
}
