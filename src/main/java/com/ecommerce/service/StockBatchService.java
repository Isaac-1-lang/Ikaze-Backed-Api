package com.ecommerce.service;

import com.ecommerce.dto.StockBatchDTO;
import com.ecommerce.dto.CreateStockBatchRequest;
import com.ecommerce.dto.CreateVariantBatchRequest;
import com.ecommerce.dto.UpdateStockBatchRequest;

import java.util.List;
import java.util.UUID;

public interface StockBatchService {

    /**
     * Create a new stock batch
     * 
     * @param request The request containing batch details
     * @return The created stock batch DTO
     * @throws IllegalArgumentException if stock not found or validation fails
     */
    StockBatchDTO createStockBatch(CreateStockBatchRequest request);

    /**
     * Create a new stock batch for a variant and warehouse
     * 
     * @param variantId The variant ID
     * @param warehouseId The warehouse ID
     * @param request The request containing batch details
     * @return The created stock batch DTO
     * @throws IllegalArgumentException if variant or warehouse not found or validation fails
     */
    StockBatchDTO createStockBatchForVariant(Long variantId, Long warehouseId, CreateVariantBatchRequest request);

    /**
     * Get all stock batches for a specific stock entry
     * 
     * @param stockId The stock ID
     * @return List of stock batch DTOs
     * @throws IllegalArgumentException if stock not found
     */
    List<StockBatchDTO> getStockBatchesByStockId(Long stockId);

    /**
     * Get all stock batches for a specific product across all warehouses
     * 
     * @param productId The product ID
     * @return List of stock batch DTOs
     * @throws IllegalArgumentException if product not found
     */
    List<StockBatchDTO> getStockBatchesByProductId(UUID productId);

    /**
     * Get a specific stock batch by ID
     * 
     * @param batchId The batch ID
     * @return The stock batch DTO
     * @throws IllegalArgumentException if batch not found
     */
    StockBatchDTO getStockBatchById(Long batchId);

    /**
     * Update an existing stock batch
     * 
     * @param batchId The batch ID
     * @param request The update request
     * @return The updated stock batch DTO
     * @throws IllegalArgumentException if batch not found or validation fails
     */
    StockBatchDTO updateStockBatch(Long batchId, UpdateStockBatchRequest request);

    /**
     * Delete a stock batch
     * 
     * @param batchId The batch ID
     * @throws IllegalArgumentException if batch not found
     */
    void deleteStockBatch(Long batchId);

    /**
     * Recall a stock batch
     * 
     * @param batchId The batch ID
     * @param reason  The reason for recall (optional)
     * @return The recalled stock batch DTO
     * @throws IllegalArgumentException if batch not found
     */
    StockBatchDTO recallStockBatch(Long batchId, String reason);

    /**
     * Get all stock batches for a specific product variant across all warehouses
     * 
     * @param variantId The product variant ID
     * @return List of stock batch DTOs
     * @throws IllegalArgumentException if variant not found
     */
    List<StockBatchDTO> getStockBatchesByVariantId(Long variantId);

    /**
     * Get batches that are expiring soon
     * 
     * @param daysThreshold Number of days to consider as "soon"
     * @return List of expiring stock batch DTOs
     */
    List<StockBatchDTO> getBatchesExpiringSoon(int daysThreshold);
}