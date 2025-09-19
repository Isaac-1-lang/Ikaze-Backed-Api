package com.ecommerce.service.impl;

import com.ecommerce.dto.StockBatchDTO;
import com.ecommerce.dto.CreateStockBatchRequest;
import com.ecommerce.dto.UpdateStockBatchRequest;
import com.ecommerce.entity.StockBatch;
import com.ecommerce.entity.Stock;
import com.ecommerce.entity.Product;
import com.ecommerce.enums.BatchStatus;
import com.ecommerce.repository.StockBatchRepository;
import com.ecommerce.repository.StockRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockBatchServiceImpl implements StockBatchService {

        private final StockBatchRepository stockBatchRepository;
        private final StockRepository stockRepository;
        private final ProductRepository productRepository;

        @Override
        public StockBatchDTO createStockBatch(CreateStockBatchRequest request) {
                log.info("Creating stock batch for stock ID: {}", request.getStockId());

                // Validate stock exists
                Stock stock = stockRepository.findById(request.getStockId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Stock not found with ID: " + request.getStockId()));

                // Check if batch number already exists for this stock
                if (stockBatchRepository.findByStockAndBatchNumber(stock, request.getBatchNumber()).isPresent()) {
                        throw new IllegalArgumentException(
                                        "Batch number '" + request.getBatchNumber()
                                                        + "' already exists for this stock");
                }

                // Create new stock batch
                StockBatch stockBatch = new StockBatch();
                stockBatch.setStock(stock);
                stockBatch.setBatchNumber(request.getBatchNumber());
                stockBatch.setManufactureDate(request.getManufactureDate());
                stockBatch.setExpiryDate(request.getExpiryDate());
                stockBatch.setQuantity(request.getQuantity());
                stockBatch.setSupplierName(request.getSupplierName());
                stockBatch.setSupplierBatchNumber(request.getSupplierBatchNumber());

                // Save the batch
                StockBatch savedBatch = stockBatchRepository.save(stockBatch);

                // Update stock quantity
                updateStockQuantity(stock);

                log.info("Successfully created stock batch with ID: {}", savedBatch.getId());
                return mapToDTO(savedBatch);
        }

        @Override
        @Transactional(readOnly = true)
        public List<StockBatchDTO> getStockBatchesByStockId(Long stockId) {
                log.info("Retrieving stock batches for stock ID: {}", stockId);

                Stock stock = stockRepository.findById(stockId)
                                .orElseThrow(() -> new IllegalArgumentException("Stock not found with ID: " + stockId));

                List<StockBatch> batches = stockBatchRepository.findByStockOrderByCreatedAtDesc(stock);
                return batches.stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<StockBatchDTO> getStockBatchesByProductId(UUID productId) {
                log.info("Retrieving stock batches for product ID: {}", productId);

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Product not found with ID: " + productId));

                List<StockBatch> batches = stockBatchRepository.findByWarehouseAndProduct(null, productId);
                return batches.stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public StockBatchDTO getStockBatchById(Long batchId) {
                log.info("Retrieving stock batch with ID: {}", batchId);

                StockBatch stockBatch = stockBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Stock batch not found with ID: " + batchId));

                return mapToDTO(stockBatch);
        }

        @Override
        public StockBatchDTO updateStockBatch(Long batchId, UpdateStockBatchRequest request) {
                log.info("Updating stock batch with ID: {}", batchId);

                StockBatch stockBatch = stockBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Stock batch not found with ID: " + batchId));

                // Check if batch number already exists for this stock (excluding current batch)
                if (!stockBatch.getBatchNumber().equals(request.getBatchNumber())) {
                        if (stockBatchRepository
                                        .findByStockAndBatchNumber(stockBatch.getStock(), request.getBatchNumber())
                                        .isPresent()) {
                                throw new IllegalArgumentException(
                                                "Batch number '" + request.getBatchNumber()
                                                                + "' already exists for this stock");
                        }
                }

                // Update fields
                stockBatch.setBatchNumber(request.getBatchNumber());
                stockBatch.setManufactureDate(request.getManufactureDate());
                stockBatch.setExpiryDate(request.getExpiryDate());
                if (request.getQuantity() != null) {
                        stockBatch.setQuantity(request.getQuantity());
                }
                stockBatch.setSupplierName(request.getSupplierName());
                stockBatch.setSupplierBatchNumber(request.getSupplierBatchNumber());

                // Save the updated batch
                StockBatch savedBatch = stockBatchRepository.save(stockBatch);

                // Update stock quantity
                updateStockQuantity(stockBatch.getStock());

                log.info("Successfully updated stock batch with ID: {}", savedBatch.getId());
                return mapToDTO(savedBatch);
        }

        @Override
        public void deleteStockBatch(Long batchId) {
                log.info("Deleting stock batch with ID: {}", batchId);

                StockBatch stockBatch = stockBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Stock batch not found with ID: " + batchId));

                Stock stock = stockBatch.getStock();

                // Delete the batch
                stockBatchRepository.delete(stockBatch);

                // Update stock quantity
                updateStockQuantity(stock);

                log.info("Successfully deleted stock batch with ID: {}", batchId);
        }

        @Override
        public StockBatchDTO recallStockBatch(Long batchId, String reason) {
                log.info("Recalling stock batch with ID: {}", batchId);

                StockBatch stockBatch = stockBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Stock batch not found with ID: " + batchId));

                // Recall the batch
                stockBatch.recall(reason);

                // Save the updated batch
                StockBatch savedBatch = stockBatchRepository.save(stockBatch);

                // Update stock quantity
                updateStockQuantity(stockBatch.getStock());

                log.info("Successfully recalled stock batch with ID: {}", savedBatch.getId());
                return mapToDTO(savedBatch);
        }

        @Override
        @Transactional(readOnly = true)
        public List<StockBatchDTO> getBatchesExpiringSoon(int daysThreshold) {
                log.info("Retrieving batches expiring within {} days", daysThreshold);

                LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);

                List<StockBatch> batches = stockBatchRepository.findAll().stream()
                                .filter(batch -> batch.isExpiringSoon(daysThreshold))
                                .collect(Collectors.toList());

                return batches.stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Update the stock quantity based on active batches
         */
        private void updateStockQuantity(Stock stock) {
                Integer totalActiveQuantity = stockBatchRepository.getTotalActiveQuantityByStock(stock);
                stock.setQuantity(totalActiveQuantity != null ? totalActiveQuantity : 0);
                stockRepository.save(stock);
                log.debug("Updated stock quantity for stock ID {} to {}", stock.getId(), stock.getQuantity());
        }

        /**
         * Map StockBatch entity to StockBatchDTO
         */
        private StockBatchDTO mapToDTO(StockBatch stockBatch) {
                return StockBatchDTO.builder()
                                .id(stockBatch.getId())
                                .stockId(stockBatch.getStock().getId())
                                .batchNumber(stockBatch.getBatchNumber())
                                .manufactureDate(stockBatch.getManufactureDate())
                                .expiryDate(stockBatch.getExpiryDate())
                                .quantity(stockBatch.getQuantity())
                                .status(stockBatch.getStatus())
                                .supplierName(stockBatch.getSupplierName())
                                .supplierBatchNumber(stockBatch.getSupplierBatchNumber())
                                .createdAt(stockBatch.getCreatedAt())
                                .updatedAt(stockBatch.getUpdatedAt())
                                .productName(stockBatch.getEffectiveProductName())
                                .warehouseName(stockBatch.getWarehouseName())
                                .warehouseId(stockBatch.getStock().getWarehouse().getId())
                                .productId(stockBatch.getStock().getEffectiveProduct() != null
                                                ? stockBatch.getStock().getEffectiveProduct().getProductId().toString()
                                                : null)
                                .variantId(stockBatch.getStock().getProductVariant() != null
                                                ? stockBatch.getStock().getProductVariant().getId().toString()
                                                : null)
                                .variantName(stockBatch.getStock().getProductVariant() != null
                                                ? stockBatch.getStock().getProductVariant().getVariantName()
                                                : null)
                                .isExpired(stockBatch.isExpired())
                                .isExpiringSoon(stockBatch.isExpiringSoon(30))
                                .isEmpty(stockBatch.isEmpty())
                                .isRecalled(stockBatch.isRecalled())
                                .isAvailable(stockBatch.isAvailable())
                                .build();
        }
}
