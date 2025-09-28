package com.ecommerce.service;

import com.ecommerce.entity.*;
import com.ecommerce.enums.BatchStatus;
import com.ecommerce.repository.StockBatchRepository;
import com.ecommerce.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FEFOStockAllocationService {

    private final StockBatchRepository stockBatchRepository;
    private final StockRepository stockRepository;

    @Transactional
    public List<BatchAllocation> allocateStock(Product product, ProductVariant variant, int requestedQuantity, Long warehouseId) {
        List<BatchAllocation> allocations = new ArrayList<>();
        
        // Find the single stock entry for this product/variant in the specified warehouse
        Optional<Stock> stockOptional;
        if (variant != null) {
            stockOptional = stockRepository.findByProductVariantVariantIdAndWarehouseWarehouseId(variant.getId(), warehouseId);
        } else {
            stockOptional = stockRepository.findByProductProductIdAndWarehouseWarehouseId(product.getProductId(), warehouseId);
        }

        if (stockOptional.isEmpty()) {
            throw new IllegalStateException("No stock entry found for the specified product/variant in warehouse " + warehouseId);
        }

        Stock stock = stockOptional.get();
        int remainingQuantity = requestedQuantity;
        
        List<StockBatch> availableBatches = getAvailableBatchesFEFO(stock);
        
        for (StockBatch batch : availableBatches) {
            if (remainingQuantity <= 0) break;
            
            if (isBatchExpiredOrRecalled(batch)) {
                updateBatchStatus(batch);
                continue;
            }
            
            int availableInBatch = batch.getQuantity();
            int allocatedFromBatch = Math.min(remainingQuantity, availableInBatch);
            
            allocations.add(new BatchAllocation(
                batch,
                stock.getWarehouse(),
                allocatedFromBatch
            ));
            
            remainingQuantity -= allocatedFromBatch;
        }
        
        if (remainingQuantity > 0) {
            throw new IllegalStateException("Insufficient stock available. Missing " + remainingQuantity + " units");
        }
        
        return allocations;
    }

    @Transactional
    public void commitAllocation(List<BatchAllocation> allocations) {
        for (BatchAllocation allocation : allocations) {
            StockBatch batch = allocation.getStockBatch();
            int newQuantity = batch.getQuantity() - allocation.getQuantityAllocated();
            
            if (newQuantity < 0) {
                throw new IllegalStateException("Cannot allocate more than available in batch " + batch.getId());
            }
            
            batch.setQuantity(newQuantity);
            
            if (newQuantity == 0) {
                batch.setStatus(BatchStatus.EMPTY);
            }
            
            stockBatchRepository.save(batch);
        }
    }

    private List<StockBatch> getAvailableBatchesFEFO(Stock stock) {
        return stockBatchRepository.findByStock(stock).stream()
            .filter(batch -> batch.getQuantity() > 0)
            .filter(batch -> batch.getStatus() == BatchStatus.ACTIVE || batch.getStatus() == BatchStatus.EMPTY)
            .sorted((b1, b2) -> {
                // First sort by expiry date (FEFO - First Expired, First Out)
                if (b1.getExpiryDate() == null && b2.getExpiryDate() == null) {
                    // If no expiry dates, sort by manufacture date (older first)
                    if (b1.getManufactureDate() == null && b2.getManufactureDate() == null) {
                        return 0; // Both null, consider equal
                    }
                    if (b1.getManufactureDate() == null) return 1; // b1 goes last
                    if (b2.getManufactureDate() == null) return -1; // b2 goes last
                    return b1.getManufactureDate().compareTo(b2.getManufactureDate());
                }
                if (b1.getExpiryDate() == null) return 1; // No expiry goes last
                if (b2.getExpiryDate() == null) return -1; // No expiry goes last
                return b1.getExpiryDate().compareTo(b2.getExpiryDate()); // Earlier expiry first
            })
            .collect(Collectors.toList());
    }

    private boolean isBatchExpiredOrRecalled(StockBatch batch) {
        if (batch.getStatus() == BatchStatus.EXPIRED || batch.getStatus() == BatchStatus.RECALLED) {
            return true;
        }
        
        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDateTime.now())) {
            return true;
        }
        
        return false;
    }

    private void updateBatchStatus(StockBatch batch) {
        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDateTime.now())) {
            batch.setStatus(BatchStatus.EXPIRED);
            stockBatchRepository.save(batch);
        }
    }

    public static class BatchAllocation {
        private final StockBatch stockBatch;
        private final Warehouse warehouse;
        private final int quantityAllocated;

        public BatchAllocation(StockBatch stockBatch, Warehouse warehouse, int quantityAllocated) {
            this.stockBatch = stockBatch;
            this.warehouse = warehouse;
            this.quantityAllocated = quantityAllocated;
        }

        public StockBatch getStockBatch() {
            return stockBatch;
        }

        public Warehouse getWarehouse() {
            return warehouse;
        }

        public int getQuantityAllocated() {
            return quantityAllocated;
        }
    }
}
