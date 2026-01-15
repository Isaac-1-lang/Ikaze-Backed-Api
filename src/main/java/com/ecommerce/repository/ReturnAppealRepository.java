package com.ecommerce.repository;

import com.ecommerce.entity.ReturnAppeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnAppealRepository
        extends JpaRepository<ReturnAppeal, Long>, JpaSpecificationExecutor<ReturnAppeal> {

    /**
     * Find appeal by return request ID
     */
    Optional<ReturnAppeal> findByReturnRequestId(Long returnRequestId);

    /**
     * Find appeals by status
     */
    List<ReturnAppeal> findByStatus(ReturnAppeal.AppealStatus status);

    /**
     * Find appeals by status with pagination
     */
    Page<ReturnAppeal> findByStatus(ReturnAppeal.AppealStatus status, Pageable pageable);

    /**
     * Find appeals by level (should always be 1)
     */
    List<ReturnAppeal> findByLevel(Integer level);

    /**
     * Find appeals submitted within a date range
     */
    List<ReturnAppeal> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find appeals submitted within a date range with pagination
     */
    Page<ReturnAppeal> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find appeals by status and submitted date range
     */
    List<ReturnAppeal> findByStatusAndSubmittedAtBetween(ReturnAppeal.AppealStatus status, LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * Find appeal with media loaded
     */
    @Query("SELECT DISTINCT ra FROM ReturnAppeal ra LEFT JOIN FETCH ra.appealMedia WHERE ra.id = :id")
    Optional<ReturnAppeal> findByIdWithMedia(@Param("id") Long id);

    /**
     * Find appeal with return request loaded
     */
    @Query("SELECT DISTINCT ra FROM ReturnAppeal ra LEFT JOIN FETCH ra.returnRequest WHERE ra.id = :id")
    Optional<ReturnAppeal> findByIdWithReturnRequest(@Param("id") Long id);

    /**
     * Find appeal with basic data and media loaded (avoiding
     * MultipleBagFetchException)
     */
    @Query("SELECT DISTINCT ra FROM ReturnAppeal ra " +
            "LEFT JOIN FETCH ra.appealMedia " +
            "LEFT JOIN FETCH ra.returnRequest " +
            "WHERE ra.id = :id")
    Optional<ReturnAppeal> findByIdWithAllData(@Param("id") Long id);

    /**
     * Find appeals by customer ID (through return request relationship)
     */
    @Query("SELECT ra FROM ReturnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId")
    List<ReturnAppeal> findByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find appeals by customer ID with pagination
     */
    @Query("SELECT ra FROM ReturnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId")
    Page<ReturnAppeal> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find appeals by customer ID and status
     */
    @Query("SELECT ra FROM ReturnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId AND ra.status = :status")
    List<ReturnAppeal> findByCustomerIdAndStatus(@Param("customerId") UUID customerId,
            @Param("status") ReturnAppeal.AppealStatus status);

    @Query("SELECT ra FROM ReturnAppeal ra JOIN ra.returnRequest rr JOIN rr.shopOrder so JOIN so.order o WHERE o.orderId = :orderId")
    List<ReturnAppeal> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Count appeals by status
     */
    long countByStatus(ReturnAppeal.AppealStatus status);

    /**
     * Count appeals by customer ID
     */
    @Query("SELECT COUNT(ra) FROM ReturnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId")
    long countByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Count appeals by customer ID and status
     */
    @Query("SELECT COUNT(ra) FROM ReturnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId AND ra.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") UUID customerId,
            @Param("status") ReturnAppeal.AppealStatus status);

    /**
     * Find pending appeals older than specified date
     */
    List<ReturnAppeal> findByStatusAndSubmittedAtBefore(ReturnAppeal.AppealStatus status, LocalDateTime date);

    /**
     * Check if appeal exists for return request
     */
    boolean existsByReturnRequestId(Long returnRequestId);

    /**
     * Find recent appeals (last 30 days)
     */
    @Query("SELECT ra FROM ReturnAppeal ra WHERE ra.submittedAt >= :thirtyDaysAgo ORDER BY ra.submittedAt DESC")
    List<ReturnAppeal> findRecentAppeals(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

    /**
     * Find recent appeals by customer (last 30 days)
     */
    @Query("SELECT ra FROM ReturnAppeal ra JOIN ra.returnRequest rr WHERE rr.customerId = :customerId AND ra.submittedAt >= :thirtyDaysAgo ORDER BY ra.submittedAt DESC")
    List<ReturnAppeal> findRecentAppealsByCustomerId(@Param("customerId") UUID customerId,
            @Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

    /**
     * Find appeals that need decision (pending for more than specified days)
     */
    @Query("SELECT ra FROM ReturnAppeal ra WHERE ra.status = 'PENDING' AND ra.submittedAt <= :cutoffDate ORDER BY ra.submittedAt ASC")
    List<ReturnAppeal> findAppealsNeedingDecision(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find appeals by status list (for statistics)
     */
    List<ReturnAppeal> findByStatusIn(List<ReturnAppeal.AppealStatus> statuses);
}
