package com.ecommerce.repository;

import com.ecommerce.entity.StockBatch;
import com.ecommerce.entity.Stock;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for StockBatch entity
 * Provides data access methods for batch management operations
 */
@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    /**
     * Find all batches for a specific stock entry
     * 
     * @param stock The stock entry
     * @return List of stock batches
     */
    List<StockBatch> findByStock(Stock stock);

    /**
     * Find all batches for a specific stock entry with pagination
     * 
     * @param stock    The stock entry
     * @param pageable Pagination information
     * @return Page of stock batches
     */
    Page<StockBatch> findByStock(Stock stock, Pageable pageable);

    /**
     * Find batches by batch number
     * 
     * @param batchNumber The batch number
     * @return Optional stock batch
     */
    Optional<StockBatch> findByBatchNumber(String batchNumber);

    /**
     * Find batches by status
     * 
     * @param status The batch status
     * @return List of stock batches with the specified status
     */
    List<StockBatch> findByStatus(BatchStatus status);

    /**
     * Find batches by status with pagination
     * 
     * @param status   The batch status
     * @param pageable Pagination information
     * @return Page of stock batches with the specified status
     */
    Page<StockBatch> findByStatus(BatchStatus status, Pageable pageable);

    /**
     * Find batches that are expiring soon
     * 
     * @param expiryDate The threshold expiry date
     * @return List of batches expiring before the given date
     */
    List<StockBatch> findByExpiryDateBeforeAndStatus(LocalDate expiryDate, BatchStatus status);

    /**
     * Find batches that have expired
     * 
     * @param currentDate Current date
     * @return List of expired batches
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.expiryDate < :currentDate AND sb.status = :status")
    List<StockBatch> findExpiredBatches(@Param("currentDate") LocalDate currentDate,
            @Param("status") BatchStatus status);

    /**
     * Find batches by warehouse
     * 
     * @param warehouse The warehouse
     * @return List of stock batches in the warehouse
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.stock.warehouse = :warehouse")
    List<StockBatch> findByWarehouse(@Param("warehouse") Warehouse warehouse);

    /**
     * Find batches by warehouse with pagination
     * 
     * @param warehouse The warehouse
     * @param pageable  Pagination information
     * @return Page of stock batches in the warehouse
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.stock.warehouse = :warehouse")
    Page<StockBatch> findByWarehouse(@Param("warehouse") Warehouse warehouse, Pageable pageable);

    /**
     * Find batches by product
     * 
     * @param product The product
     * @return List of stock batches for the product
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.stock.product = :product")
    List<StockBatch> findByProduct(@Param("product") Product product);

    /**
     * Find batches by product variant
     * 
     * @param productVariant The product variant
     * @return List of stock batches for the product variant
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.stock.productVariant = :productVariant")
    List<StockBatch> findByProductVariant(@Param("productVariant") ProductVariant productVariant);

    /**
     * Find active batches for a specific product/variant in a warehouse
     * 
     * @param product        The product (can be null)
     * @param productVariant The product variant (can be null)
     * @param warehouse      The warehouse
     * @return List of active stock batches
     */
    @Query("SELECT sb FROM StockBatch sb WHERE " +
            "(:product IS NULL OR sb.stock.product = :product) AND " +
            "(:productVariant IS NULL OR sb.stock.productVariant = :productVariant) AND " +
            "sb.stock.warehouse = :warehouse AND sb.status = :status")
    List<StockBatch> findActiveBatchesByProductAndWarehouse(
            @Param("product") Product product,
            @Param("productVariant") ProductVariant productVariant,
            @Param("warehouse") Warehouse warehouse,
            @Param("status") BatchStatus status);

    /**
     * Find batches expiring within a specific number of days
     * 
     * @param startDate Start date (today)
     * @param endDate   End date (today + days)
     * @param status    Batch status to filter by
     * @return List of batches expiring within the date range
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.expiryDate BETWEEN :startDate AND :endDate AND sb.status = :status")
    List<StockBatch> findBatchesExpiringBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") BatchStatus status);

    /**
     * Find batches by manufacture date range
     * 
     * @param startDate Start manufacture date
     * @param endDate   End manufacture date
     * @return List of batches manufactured within the date range
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.manufactureDate BETWEEN :startDate AND :endDate")
    List<StockBatch> findByManufactureDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count batches by status
     * 
     * @param status The batch status
     * @return Number of batches with the specified status
     */
    long countByStatus(BatchStatus status);

    /**
     * Count batches by warehouse
     * 
     * @param warehouse The warehouse
     * @return Number of batches in the warehouse
     */
    @Query("SELECT COUNT(sb) FROM StockBatch sb WHERE sb.stock.warehouse = :warehouse")
    long countByWarehouse(@Param("warehouse") Warehouse warehouse);

    /**
     * Find batches with low quantity (quantity <= threshold)
     * 
     * @param threshold The quantity threshold
     * @param status    The batch status
     * @return List of batches with low quantity
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.quantity <= :threshold AND sb.status = :status")
    List<StockBatch> findBatchesWithLowQuantity(@Param("threshold") Integer threshold,
            @Param("status") BatchStatus status);

    /**
     * Find batches by batch number containing the given text (case insensitive)
     * 
     * @param batchNumber The batch number text to search for
     * @return List of batches with matching batch numbers
     */
    List<StockBatch> findByBatchNumberContainingIgnoreCase(String batchNumber);

    /**
     * Find batches by supplier name
     * 
     * @param supplierName The supplier name
     * @return List of batches from the supplier
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.supplierName = :supplierName")
    List<StockBatch> findBySupplierName(@Param("supplierName") String supplierName);

    /**
     * Find batches by supplier batch number
     * 
     * @param supplierBatchNumber The supplier batch number
     * @return List of batches with the supplier batch number
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.supplierBatchNumber = :supplierBatchNumber")
    List<StockBatch> findBySupplierBatchNumber(@Param("supplierBatchNumber") String supplierBatchNumber);

    /**
     * Check if a batch number already exists
     * 
     * @param batchNumber The batch number to check
     * @return true if batch number exists
     */
    boolean existsByBatchNumber(String batchNumber);

    /**
     * Find batches that need status update (expired batches that are still marked
     * as active)
     * 
     * @param currentDate Current date
     * @return List of batches that need status update
     */
    @Query("SELECT sb FROM StockBatch sb WHERE sb.expiryDate < :currentDate AND sb.status = 'ACTIVE'")
    List<StockBatch> findBatchesNeedingStatusUpdate(@Param("currentDate") LocalDate currentDate);
}
