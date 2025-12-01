package com.ecommerce.repository;

import com.ecommerce.entity.OrderActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderActivityLogRepository extends JpaRepository<OrderActivityLog, Long> {

    /**
     * Find all activity logs for a specific order, ordered by timestamp
     */
    List<OrderActivityLog> findByOrderIdOrderByTimestampAsc(Long orderId);

    /**
     * Find all activity logs for a specific order within a date range
     */
    @Query("SELECT oal FROM OrderActivityLog oal WHERE oal.orderId = :orderId " +
           "AND oal.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY oal.timestamp ASC")
    List<OrderActivityLog> findByOrderIdAndDateRange(
            @Param("orderId") Long orderId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find activity logs by order ID and activity type
     */
    List<OrderActivityLog> findByOrderIdAndActivityTypeOrderByTimestampAsc(
            Long orderId, 
            OrderActivityLog.ActivityType activityType
    );

    /**
     * Find recent activity logs for an order (last N entries)
     */
    @Query("SELECT oal FROM OrderActivityLog oal WHERE oal.orderId = :orderId " +
           "ORDER BY oal.timestamp DESC")
    List<OrderActivityLog> findRecentByOrderId(@Param("orderId") Long orderId);

    /**
     * Count total activities for an order
     */
    long countByOrderId(Long orderId);

    /**
     * Find activities by reference (e.g., all activities related to a specific return request)
     */
    List<OrderActivityLog> findByReferenceIdAndReferenceTypeOrderByTimestampAsc(
            String referenceId, 
            String referenceType
    );

    /**
     * Delete all logs for a specific order (for cleanup/GDPR)
     */
    void deleteByOrderId(Long orderId);
}
