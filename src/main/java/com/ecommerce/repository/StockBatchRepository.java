package com.ecommerce.repository;

import com.ecommerce.entity.StockBatch;
import com.ecommerce.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

        List<StockBatch> findByStock(Stock stock);

        List<StockBatch> findByStockOrderByCreatedAtDesc(Stock stock);

        @Query("SELECT sb FROM StockBatch sb WHERE sb.stock = :stock AND sb.status = 'ACTIVE'")
        List<StockBatch> findActiveBatchesByStock(@Param("stock") Stock stock);

        @Query("SELECT sb FROM StockBatch sb WHERE sb.stock = :stock AND sb.status = 'ACTIVE' ORDER BY sb.expiryDate ASC")
        List<StockBatch> findActiveBatchesByStockOrderByExpiryDate(@Param("stock") Stock stock);

        @Query("SELECT COALESCE(SUM(sb.quantity), 0) FROM StockBatch sb WHERE sb.stock = :stock AND sb.status = 'ACTIVE'")
        Integer getTotalActiveQuantityByStock(@Param("stock") Stock stock);

        @Query("SELECT COALESCE(SUM(sb.quantity), 0) FROM StockBatch sb WHERE sb.stock = :stock AND sb.status NOT IN ('RECALLED', 'EXPIRED')")
        Integer getTotalAvailableQuantityByStock(@Param("stock") Stock stock);

        @Query("SELECT sb FROM StockBatch sb WHERE sb.stock = :stock AND sb.batchNumber = :batchNumber")
        Optional<StockBatch> findByStockAndBatchNumber(@Param("stock") Stock stock,
                        @Param("batchNumber") String batchNumber);

        @Query("SELECT sb FROM StockBatch sb WHERE sb.stock.warehouse.id = :warehouseId AND sb.stock.product.productId = :productId")
        List<StockBatch> findByWarehouseAndProduct(@Param("warehouseId") Long warehouseId,
                        @Param("productId") java.util.UUID productId);

        @Query("SELECT sb FROM StockBatch sb WHERE sb.stock.warehouse.id = :warehouseId AND sb.stock.productVariant.id = :variantId")
        List<StockBatch> findByWarehouseAndVariant(@Param("warehouseId") Long warehouseId,
                        @Param("variantId") Long variantId);

        List<StockBatch> findByStockInOrderByCreatedAtDesc(List<Stock> stocks);

        @Query("SELECT sb FROM StockBatch sb WHERE sb.stock.id = :stockId AND sb.status = 'ACTIVE' AND sb.quantity > 0 ORDER BY sb.expiryDate ASC NULLS LAST")
        List<StockBatch> findActiveByStockIdOrderByExpiryDateAsc(@Param("stockId") Long stockId);

        void deleteByStock(Stock stock);
}