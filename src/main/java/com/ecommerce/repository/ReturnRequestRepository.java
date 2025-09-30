package com.ecommerce.repository;

import com.ecommerce.entity.ReturnRequest;
import com.ecommerce.entity.User;

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
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

       /**
        * Find return requests by order ID
        */
       List<ReturnRequest> findByOrderId(Long orderId);

       /**
        * Find return requests by customer ID
        */
       List<ReturnRequest> findByCustomerId(UUID customerId);

       /**
        * Find return requests by customer ID with pagination (avoiding
        * MultipleBagFetchException)
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
                     "LEFT JOIN FETCH rr.order o " +
                     "LEFT JOIN FETCH o.orderCustomerInfo oci " +
                     "LEFT JOIN FETCH rr.customer u " +
                     "WHERE rr.customerId = :customerId")
       Page<ReturnRequest> findByCustomerIdWithDetails(@Param("customerId") UUID customerId, Pageable pageable);

       Page<ReturnRequest> findByCustomerId(UUID customerId, Pageable pageable);

       /**
        * Find return requests by status
        */
       List<ReturnRequest> findByStatus(ReturnRequest.ReturnStatus status);

       /**
        * Find return requests by status with pagination (avoiding
        * MultipleBagFetchException)
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
                     "LEFT JOIN FETCH rr.order o " +
                     "LEFT JOIN FETCH o.orderCustomerInfo oci " +
                     "LEFT JOIN FETCH rr.customer u " +
                     "WHERE rr.status = :status")
       Page<ReturnRequest> findByStatusWithDetails(@Param("status") ReturnRequest.ReturnStatus status,
                     Pageable pageable);

       Page<ReturnRequest> findByStatus(ReturnRequest.ReturnStatus status, Pageable pageable);

       /**
        * Find return requests by customer ID and status
        */
       List<ReturnRequest> findByCustomerIdAndStatus(UUID customerId, ReturnRequest.ReturnStatus status);

       /**
        * Find return requests by customer ID and status with pagination
        */
       Page<ReturnRequest> findByCustomerIdAndStatus(UUID customerId, ReturnRequest.ReturnStatus status,
                     Pageable pageable);

       /**
        * Find return requests submitted within a date range
        */
       List<ReturnRequest> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

       /**
        * Find return requests submitted within a date range with pagination
        */
       Page<ReturnRequest> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

       /**
        * Find return requests that can be appealed (denied status and no existing
        * appeal)
        */
       @Query("SELECT rr FROM ReturnRequest rr WHERE rr.status = 'DENIED' AND rr.returnAppeal IS NULL")
       List<ReturnRequest> findAppealableRequests();

       /**
        * Find return requests that can be appealed by customer ID
        */
       @Query("SELECT rr FROM ReturnRequest rr WHERE rr.customerId = :customerId AND rr.status = 'DENIED' AND rr.returnAppeal IS NULL")
       List<ReturnRequest> findAppealableRequestsByCustomerId(@Param("customerId") UUID customerId);

       /**
        * Find return requests with their media loaded
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr LEFT JOIN FETCH rr.returnMedia WHERE rr.id = :id")
       Optional<ReturnRequest> findByIdWithMedia(@Param("id") Long id);

       /**
        * Find return requests with their appeal loaded
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr LEFT JOIN FETCH rr.returnAppeal WHERE rr.id = :id")
       Optional<ReturnRequest> findByIdWithAppeal(@Param("id") Long id);

       /**
        * Find return request with basic data (no collections to avoid
        * MultipleBagFetchException)
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
                     "LEFT JOIN FETCH rr.order o " +
                     "LEFT JOIN FETCH o.orderCustomerInfo " +
                     "WHERE rr.id = :id")
       Optional<ReturnRequest> findByIdWithBasicData(@Param("id") Long id);

       @Query("""
                         SELECT rr.customer
                         FROM ReturnRequest rr
                         WHERE rr.id = :id
                     """)
       Optional<User> findCustomerByReturnRequestId(@Param("id") Long id);

       /**
        * Find return request with return items loaded
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
                     "LEFT JOIN FETCH rr.returnItems " +
                     "WHERE rr.id = :id")
       Optional<ReturnRequest> findByIdWithItems(@Param("id") Long id);

       /**
        * Find return request with appeal data loaded separately
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
                     "LEFT JOIN FETCH rr.returnAppeal ra " +
                     "LEFT JOIN FETCH ra.appealMedia " +
                     "WHERE rr.id = :id")
       Optional<ReturnRequest> findByIdWithAppealData(@Param("id") Long id);

       /**
        * Count return requests by status
        */
       long countByStatus(ReturnRequest.ReturnStatus status);

       /**
        * Count return requests by customer ID
        */
       long countByCustomerId(UUID customerId);

       /**
        * Count return requests by customer ID and status
        */
       long countByCustomerIdAndStatus(UUID customerId, ReturnRequest.ReturnStatus status);

       /**
        * Find pending return requests older than specified date
        */
       List<ReturnRequest> findByStatusAndSubmittedAtBefore(ReturnRequest.ReturnStatus status, LocalDateTime date);

       /**
        * Check if customer has any return request for specific order
        */
       boolean existsByOrderIdAndCustomerId(Long orderId, UUID customerId);

       /**
        * Find return requests by order ID and customer ID
        */
       Optional<ReturnRequest> findByOrderIdAndCustomerId(Long orderId, UUID customerId);

       /**
        * Find recent return requests by customer (last 30 days)
        */
       @Query("SELECT rr FROM ReturnRequest rr WHERE rr.customerId = :customerId AND rr.submittedAt >= :thirtyDaysAgo ORDER BY rr.submittedAt DESC")
       List<ReturnRequest> findRecentByCustomerId(@Param("customerId") UUID customerId,
                     @Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

       /**
        * Find all return requests - basic query without complex joins
        */
       @Query("SELECT rr FROM ReturnRequest rr ORDER BY rr.submittedAt DESC")
       List<ReturnRequest> findAllReturnRequestsBasic();

       /**
        * Find guest return requests (where customerId is null) - avoiding
        * MultipleBagFetchException
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
                     "LEFT JOIN FETCH rr.order o " +
                     "LEFT JOIN FETCH o.orderCustomerInfo oci " +
                     "WHERE rr.customerId IS NULL")
       Page<ReturnRequest> findGuestReturnRequests(Pageable pageable);

       /**
        * Find all return requests with basic relationships fetched (avoiding
        * MultipleBagFetchException)
        */
       @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
                     "LEFT JOIN FETCH rr.order o " +
                     "LEFT JOIN FETCH o.orderCustomerInfo oci " +
                     "LEFT JOIN FETCH rr.customer u")
       Page<ReturnRequest> findAllWithDetails(Pageable pageable);
}
