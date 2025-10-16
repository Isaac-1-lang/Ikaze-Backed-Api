package com.ecommerce.repository;

import com.ecommerce.entity.MoneyFlow;
import com.ecommerce.enums.MoneyFlowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoneyFlowRepository extends JpaRepository<MoneyFlow, Long> {

    /**
     * Find all money flows between start and end dates
     */
    List<MoneyFlow> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);

    /**
     * Get the latest money flow to determine current balance
     */
    Optional<MoneyFlow> findTopByOrderByCreatedAtDesc();

    /**
     * Aggregate by minute - PostgreSQL specific
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('minute', created_at), 'YYYY-MM-DD HH24:MI') as period,
            COALESCE(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) as total_inflow,
            COALESCE(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) as total_outflow
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE_TRUNC('minute', created_at)
        ORDER BY DATE_TRUNC('minute', created_at)
        """, nativeQuery = true)
    List<Object[]> aggregateByMinute(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregate by hour - PostgreSQL specific
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('hour', created_at), 'YYYY-MM-DD HH24:00') as period,
            COALESCE(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) as total_inflow,
            COALESCE(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) as total_outflow
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE_TRUNC('hour', created_at)
        ORDER BY DATE_TRUNC('hour', created_at)
        """, nativeQuery = true)
    List<Object[]> aggregateByHour(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregate by day - PostgreSQL specific
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('day', created_at), 'YYYY-MM-DD') as period,
            COALESCE(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) as total_inflow,
            COALESCE(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) as total_outflow
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE_TRUNC('day', created_at)
        ORDER BY DATE_TRUNC('day', created_at)
        """, nativeQuery = true)
    List<Object[]> aggregateByDay(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregate by week - PostgreSQL specific
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('week', created_at), 'IYYY-"W"IW') as period,
            COALESCE(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) as total_inflow,
            COALESCE(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) as total_outflow
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE_TRUNC('week', created_at)
        ORDER BY DATE_TRUNC('week', created_at)
        """, nativeQuery = true)
    List<Object[]> aggregateByWeek(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregate by month - PostgreSQL specific
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') as period,
            COALESCE(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) as total_inflow,
            COALESCE(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) as total_outflow
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE_TRUNC('month', created_at)
        ORDER BY DATE_TRUNC('month', created_at)
        """, nativeQuery = true)
    List<Object[]> aggregateByMonth(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregate by year - PostgreSQL specific
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('year', created_at), 'YYYY') as period,
            COALESCE(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) as total_inflow,
            COALESCE(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) as total_outflow
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE_TRUNC('year', created_at)
        ORDER BY DATE_TRUNC('year', created_at)
        """, nativeQuery = true)
    List<Object[]> aggregateByYear(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Get transactions grouped by minute for detailed view
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('minute', created_at), 'YYYY-MM-DD HH24:MI') as period,
            id, description, type, amount, remaining_balance, created_at
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        ORDER BY created_at
        """, nativeQuery = true)
    List<Object[]> getDetailedTransactionsByMinute(@Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Get transactions grouped by hour for detailed view
     */
    @Query(value = """
        SELECT 
            TO_CHAR(DATE_TRUNC('hour', created_at), 'YYYY-MM-DD HH24:00') as period,
            id, description, type, amount, remaining_balance, created_at
        FROM money_flow
        WHERE created_at BETWEEN :startDate AND :endDate
        ORDER BY created_at
        """, nativeQuery = true)
    List<Object[]> getDetailedTransactionsByHour(@Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
}
