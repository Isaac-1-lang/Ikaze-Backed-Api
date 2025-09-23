package com.ecommerce.repository;

import com.ecommerce.entity.StockBatchLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockBatchLockRepository extends JpaRepository<StockBatchLock, Long> {

    List<StockBatchLock> findBySessionId(String sessionId);

    @Modifying
    @Query("DELETE FROM StockBatchLock sbl WHERE sbl.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT sbl FROM StockBatchLock sbl WHERE sbl.expiresAt < :now")
    List<StockBatchLock> findByExpiresAtBefore(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM StockBatchLock sbl WHERE sbl.expiresAt < :now")
    void deleteExpiredLocks(@Param("now") LocalDateTime now);

    @Query("SELECT sbl FROM StockBatchLock sbl WHERE sbl.stockBatch.id = :batchId")
    List<StockBatchLock> findByStockBatchId(@Param("batchId") Long batchId);

    @Query("SELECT sbl FROM StockBatchLock sbl WHERE sbl.stockBatch.id = :batchId AND sbl.sessionId = :sessionId")
    List<StockBatchLock> findByStockBatchIdAndSessionId(@Param("batchId") Long batchId, @Param("sessionId") String sessionId);

    @Query("SELECT sbl FROM StockBatchLock sbl WHERE sbl.warehouseId = :warehouseId")
    List<StockBatchLock> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COALESCE(SUM(sbl.lockedQuantity), 0) FROM StockBatchLock sbl WHERE sbl.stockBatch.id = :batchId")
    Integer getTotalLockedQuantityForBatch(@Param("batchId") Long batchId);
}
