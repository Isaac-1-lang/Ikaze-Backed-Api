package com.ecommerce.service;

import com.ecommerce.dto.CreateStockBatchDTO;
import com.ecommerce.dto.StockBatchDTO;
import com.ecommerce.entity.StockBatch;
import com.ecommerce.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for StockBatch management operations
 */
public interface StockBatchService {

    /**
     * Create a new stock batch
     * 
     * @param createStockBatchDTO The batch creation data
     * @return Created stock batch DTO
     */
    StockBatchDTO createStockBatch(CreateStockBatchDTO createStockBatchDTO);

    /**
     * Get a stock batch by ID
     * 
     * @param id The batch ID
     * @return Optional stock batch DTO
     */
    Optional<StockBatchDTO> getStockBatchById(Long id);

    /**
     * Get all stock batches with pagination
     * 
     * @param pageable Pagination information
     * @return Page of stock batch DTOs
     */
    Page<StockBatchDTO> getAllStockBatches(Pageable pageable);

    /**
     * Get stock batches by stock ID
     * 
     * @param stockId  The stock ID
     * @param pageable Pagination information
     * @return Page of stock batch DTOs
     */
    Page<StockBatchDTO> getStockBatchesByStockId(Long stockId, Pageable pageable);

    /**
     * Get stock batches by status
     * 
     * @param status   The batch status
     * @param pageable Pagination information
     * @return Page of stock batch DTOs
     */
    Page<StockBatchDTO> getStockBatchesByStatus(BatchStatus status, Pageable pageable);

    /**
     * Get batches expiring soon
     * 
     * @param daysThreshold Number of days to consider as "soon"
     * @param pageable      Pagination information
     * @return Page of stock batch DTOs
     */
    Page<StockBatchDTO> getBatchesExpiringSoon(int daysThreshold, Pageable pageable);

    /**
     * Get expired batches
     * 
     * @param pageable Pagination information
     * @return Page of expired stock batch DTOs
     */
    Page<StockBatchDTO> getExpiredBatches(Pageable pageable);

    /**
     * Update batch quantity
     * 
     * @param batchId     The batch ID
     * @param newQuantity The new quantity
     * @return Updated stock batch DTO
     */
    StockBatchDTO updateBatchQuantity(Long batchId, Integer newQuantity);

    /**
     * Reduce batch quantity
     * 
     * @param batchId The batch ID
     * @param amount  The amount to reduce
     * @return Updated stock batch DTO
     */
    StockBatchDTO reduceBatchQuantity(Long batchId, Integer amount);

    /**
     * Increase batch quantity
     * 
     * @param batchId The batch ID
     * @param amount  The amount to increase
     * @return Updated stock batch DTO
     */
    StockBatchDTO increaseBatchQuantity(Long batchId, Integer amount);

    /**
     * Recall a batch
     * 
     * @param batchId The batch ID
     * @param reason  The reason for recall
     * @return Updated stock batch DTO
     */
    StockBatchDTO recallBatch(Long batchId, String reason);

    /**
     * Update batch status
     * 
     * @param batchId The batch ID
     * @param status  The new status
     * @return Updated stock batch DTO
     */
    StockBatchDTO updateBatchStatus(Long batchId, BatchStatus status);

    /**
     * Delete a stock batch
     * 
     * @param batchId The batch ID
     */
    void deleteStockBatch(Long batchId);

    /**
     * Check if batch number exists
     * 
     * @param batchNumber The batch number to check
     * @return true if batch number exists
     */
    boolean batchNumberExists(String batchNumber);

    /**
     * Get batches by batch number (partial match)
     * 
     * @param batchNumber The batch number to search for
     * @param pageable    Pagination information
     * @return Page of stock batch DTOs
     */
    Page<StockBatchDTO> searchBatchesByNumber(String batchNumber, Pageable pageable);

    /**
     * Update expired batch statuses
     * This method should be called periodically to update batch statuses
     */
    void updateExpiredBatchStatuses();

    /**
     * Get batch statistics
     * 
     * @return Batch statistics summary
     */
    BatchStatisticsDTO getBatchStatistics();

    /**
     * DTO for batch statistics
     */
    class BatchStatisticsDTO {
        private long totalBatches;
        private long activeBatches;
        private long expiredBatches;
        private long emptyBatches;
        private long recalledBatches;
        private long batchesExpiringSoon;

        // Constructors, getters, and setters
        public BatchStatisticsDTO() {
        }

        public BatchStatisticsDTO(long totalBatches, long activeBatches, long expiredBatches,
                long emptyBatches, long recalledBatches, long batchesExpiringSoon) {
            this.totalBatches = totalBatches;
            this.activeBatches = activeBatches;
            this.expiredBatches = expiredBatches;
            this.emptyBatches = emptyBatches;
            this.recalledBatches = recalledBatches;
            this.batchesExpiringSoon = batchesExpiringSoon;
        }

        // Getters and setters
        public long getTotalBatches() {
            return totalBatches;
        }

        public void setTotalBatches(long totalBatches) {
            this.totalBatches = totalBatches;
        }

        public long getActiveBatches() {
            return activeBatches;
        }

        public void setActiveBatches(long activeBatches) {
            this.activeBatches = activeBatches;
        }

        public long getExpiredBatches() {
            return expiredBatches;
        }

        public void setExpiredBatches(long expiredBatches) {
            this.expiredBatches = expiredBatches;
        }

        public long getEmptyBatches() {
            return emptyBatches;
        }

        public void setEmptyBatches(long emptyBatches) {
            this.emptyBatches = emptyBatches;
        }

        public long getRecalledBatches() {
            return recalledBatches;
        }

        public void setRecalledBatches(long recalledBatches) {
            this.recalledBatches = recalledBatches;
        }

        public long getBatchesExpiringSoon() {
            return batchesExpiringSoon;
        }

        public void setBatchesExpiringSoon(long batchesExpiringSoon) {
            this.batchesExpiringSoon = batchesExpiringSoon;
        }
    }
}
