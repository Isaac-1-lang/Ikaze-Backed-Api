package com.ecommerce.service;

import com.ecommerce.entity.Stock;
import com.ecommerce.entity.StockBatch;
import com.ecommerce.entity.StockBatchLock;
import com.ecommerce.repository.StockRepository;
import com.ecommerce.repository.StockBatchRepository;
import com.ecommerce.repository.StockBatchLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockLockService {

    private final StockRepository stockRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockBatchLockRepository stockBatchLockRepository;
    private final Map<String, Map<Long, Integer>> stockLocks = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Integer>> specificStockLocks = new ConcurrentHashMap<>();

    @Transactional
    public boolean lockStock(String sessionId, java.util.UUID productId, Long variantId, int quantity,
            Long warehouseId) {
        try {
            Stock stock = null;
            if (variantId != null) {
                stock = stockRepository.findByProductVariantVariantIdAndWarehouseWarehouseId(variantId, warehouseId)
                        .orElse(null);
            } else {
                stock = stockRepository.findByProductProductIdAndWarehouseWarehouseId(productId, warehouseId)
                        .orElse(null);
            }

            if (stock == null || stock.getQuantity() < quantity) {
                log.warn("Insufficient stock for product {} variant {} in warehouse {}", productId, variantId,
                        warehouseId);
                return false;
            }

            stockLocks.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                    .put(warehouseId, quantity);

            stockRepository.save(stock);

            log.info("Locked {} units of product {} variant {} in warehouse {}", quantity, productId, variantId,
                    warehouseId);
            return true;
        } catch (Exception e) {
            log.error("Error locking stock: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void releaseStock(String sessionId) {
        try {
            // Release specific stock locks (more accurate)
            Map<Long, Integer> specificLocks = specificStockLocks.remove(sessionId);
            if (specificLocks != null) {
                for (Map.Entry<Long, Integer> entry : specificLocks.entrySet()) {
                    Long stockId = entry.getKey();
                    Integer quantity = entry.getValue();

                    Stock stock = stockRepository.findByIdWithWarehouse(stockId).orElse(null);
                    if (stock != null) {
                        stock.setQuantity(stock.getQuantity() + quantity);
                        stockRepository.save(stock);
                        log.info("Released {} units back to stock ID {} (warehouse: {})",
                                quantity, stockId, stock.getWarehouse().getName());
                    }
                }
            }

            // Also clean up the general warehouse locks (fallback)
            Map<Long, Integer> locks = stockLocks.remove(sessionId);
            if (locks != null) {
                log.info("Cleaned up general warehouse locks for session: {}", sessionId);
            }

            log.info("Released all stock locks for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error releasing stock locks: {}", e.getMessage());
        }
    }

    @Transactional
    public void confirmStock(String sessionId) {
        stockLocks.remove(sessionId);
        specificStockLocks.remove(sessionId);
        log.info("Confirmed stock locks for session: {}", sessionId);
    }

    @Transactional
    public boolean lockStockFromMultipleWarehouses(String sessionId,
            Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> allocations) {
        try {
            log.info("Starting stock locking for session {} with {} allocation groups", sessionId, allocations.size());
            Map<Long, Integer> totalLocks = new HashMap<>();
            Map<Long, Integer> specificLocks = new HashMap<>();

            for (Map.Entry<Long, List<MultiWarehouseStockAllocator.StockAllocation>> entry : allocations.entrySet()) {
                log.info("Processing allocation group with key: {}", entry.getKey());
                for (MultiWarehouseStockAllocator.StockAllocation allocation : entry.getValue()) {
                    Long warehouseId = allocation.getWarehouseId();
                    Long stockId = allocation.getStockId();
                    Integer quantity = allocation.getQuantity();

                    log.info("Attempting to lock: stockId={}, warehouseId={}, quantity={}", stockId, warehouseId,
                            quantity);

                    Stock stock = stockRepository.findByIdWithWarehouse(stockId).orElse(null);
                    if (stock == null) {
                        log.error("Stock not found for stockId: {}", stockId);
                        releaseStock(sessionId);
                        return false;
                    }

                    log.info("Current stock quantity: {}, requested: {}", stock.getQuantity(), quantity);

                    if (stock.getQuantity() < quantity) {
                        log.warn(
                                "Insufficient stock for allocation: warehouse {}, stock {}, available {}, requested {}",
                                warehouseId, stockId, stock.getQuantity(), quantity);
                        releaseStock(sessionId);
                        return false;
                    }

                    stock.setQuantity(stock.getQuantity() - quantity);
                    stockRepository.save(stock);

                    // Track both general warehouse locks and specific stock locks
                    totalLocks.merge(warehouseId, quantity, Integer::sum);
                    specificLocks.put(stockId, quantity);

                    log.info("Successfully locked {} units from stock ID {} in warehouse {} (distance: {} km)",
                            quantity, stockId, allocation.getWarehouseName(), allocation.getDistance());
                }
            }

            stockLocks.put(sessionId, totalLocks);
            specificStockLocks.put(sessionId, specificLocks);
            log.info("Successfully locked stock for session {}: {} total locks, {} specific locks",
                    sessionId, totalLocks.size(), specificLocks.size());
            return true;
        } catch (Exception e) {
            log.error("Error locking stock from multiple warehouses: {}", e.getMessage(), e);
            releaseStock(sessionId);
            return false;
        }
    }

    public Map<String, Object> getLockedStockInfo(String sessionId) {
        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("generalLocks", stockLocks.get(sessionId));
        info.put("specificLocks", specificStockLocks.get(sessionId));
        info.put("hasLocks", stockLocks.containsKey(sessionId) || specificStockLocks.containsKey(sessionId));
        return info;
    }

    // ===== ENHANCED BATCH-LEVEL LOCKING METHODS =====

    @Transactional
    public boolean lockStockFromBatches(String sessionId, 
                                       Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> allocations) {
        try {
            log.info("Starting enhanced batch-level stock locking for session {} with {} allocation groups", 
                    sessionId, allocations.size());
            
            List<StockBatchLock> batchLocks = new ArrayList<>();
            
            for (Map.Entry<Long, List<MultiWarehouseStockAllocator.StockAllocation>> entry : allocations.entrySet()) {
                for (MultiWarehouseStockAllocator.StockAllocation allocation : entry.getValue()) {
                    Long warehouseId = allocation.getWarehouseId();
                    Long stockId = allocation.getStockId();
                    Integer requiredQuantity = allocation.getQuantity();
                    
                    log.info("Processing allocation: stockId={}, warehouseId={}, quantity={}", 
                            stockId, warehouseId, requiredQuantity);
                    
                    Stock stock = stockRepository.findByIdWithWarehouse(stockId).orElse(null);
                    if (stock == null) {
                        log.error("Stock not found for stockId: {}", stockId);
                        unlockAllBatches(sessionId);
                        return false;
                    }
                    
                    if (!lockBatchesForQuantity(sessionId, stock, requiredQuantity, allocation, batchLocks)) {
                        log.error("Failed to lock batches for stock: {}", stockId);
                        unlockAllBatches(sessionId);
                        return false;
                    }
                }
            }
            
            // Save all batch locks
            stockBatchLockRepository.saveAll(batchLocks);
            log.info("Successfully created {} batch locks for session {}", batchLocks.size(), sessionId);
            
            return true;
        } catch (Exception e) {
            log.error("Error during batch locking for session {}: {}", sessionId, e.getMessage(), e);
            unlockAllBatches(sessionId);
            return false;
        }
    }

    private boolean lockBatchesForQuantity(String sessionId, Stock stock, Integer requiredQuantity, 
                                          MultiWarehouseStockAllocator.StockAllocation allocation, 
                                          List<StockBatchLock> batchLocks) {
        
        List<StockBatch> availableBatches = stockBatchRepository.findActiveByStockIdOrderByExpiryDateAsc(stock.getId());
        
        if (availableBatches.isEmpty()) {
            log.warn("No active batches found for stock: {}", stock.getId());
            return false;
        }
        
        int remainingQuantity = requiredQuantity;
        
        for (StockBatch batch : availableBatches) {
            if (remainingQuantity <= 0) break;
            
            // Check existing locks for this batch
            int existingLocks = stockBatchLockRepository.findByStockBatchIdAndSessionId(batch.getId(), sessionId)
                    .stream()
                    .mapToInt(StockBatchLock::getLockedQuantity)
                    .sum();
            
            int availableInBatch = batch.getQuantity() - existingLocks;
            if (availableInBatch <= 0) continue;
            
            int quantityToLock = Math.min(remainingQuantity, availableInBatch);
            
            StockBatchLock batchLock = new StockBatchLock();
            batchLock.setSessionId(sessionId);
            batchLock.setStockBatch(batch);
            batchLock.setLockedQuantity(quantityToLock);
            batchLock.setWarehouseId(allocation.getWarehouseId());
            batchLock.setWarehouseName(allocation.getWarehouseName());
            
            if (stock.getProduct() != null) {
                batchLock.setProductId(stock.getProduct().getProductId());
                batchLock.setProductName(stock.getProduct().getProductName());
            }
            
            if (stock.getProductVariant() != null) {
                batchLock.setVariantId(stock.getProductVariant().getId());
                batchLock.setVariantName(stock.getProductVariant().getVariantSku());
            }
            
            batchLocks.add(batchLock);
            remainingQuantity -= quantityToLock;
            
            log.info("Prepared batch lock: batchId={}, batchNumber={}, quantity={}, remaining={}", 
                    batch.getId(), batch.getBatchNumber(), quantityToLock, remainingQuantity);
        }
        
        if (remainingQuantity > 0) {
            log.warn("Could not allocate full quantity. Missing: {} units for stock {}", 
                    remainingQuantity, stock.getId());
            return false;
        }
        
        return true;
    }

    @Transactional
    public void unlockAllBatches(String sessionId) {
        try {
            List<StockBatchLock> locks = stockBatchLockRepository.findBySessionId(sessionId);
            log.info("Unlocking {} batch locks for session: {}", locks.size(), sessionId);
            
            stockBatchLockRepository.deleteAll(locks);
            log.info("Successfully unlocked all batches for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error unlocking batches for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    @Transactional
    public void confirmBatchLocks(String sessionId) {
        try {
            List<StockBatchLock> locks = stockBatchLockRepository.findBySessionId(sessionId);
            log.info("Confirming {} batch locks for session: {}", locks.size(), sessionId);
            
            for (StockBatchLock lock : locks) {
                StockBatch batch = lock.getStockBatch();
                int newQuantity = batch.getQuantity() - lock.getLockedQuantity();
                batch.setQuantity(Math.max(0, newQuantity));
                
                // Update batch status if needed
                batch.updateStatus();
                stockBatchRepository.save(batch);
                
                log.debug("Reduced batch {} quantity by {}, new quantity: {}", 
                         batch.getBatchNumber(), lock.getLockedQuantity(), batch.getQuantity());
            }
            
            stockBatchLockRepository.deleteAll(locks);
            log.info("Confirmed and removed {} batch locks for session: {}", locks.size(), sessionId);
        } catch (Exception e) {
            log.error("Error confirming batch locks for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    public Map<String, Object> getBatchLockInfo(String sessionId) {
        List<StockBatchLock> locks = stockBatchLockRepository.findBySessionId(sessionId);
        
        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("totalLocks", locks.size());
        info.put("hasLocks", !locks.isEmpty());
        
        if (!locks.isEmpty()) {
            int totalQuantity = locks.stream().mapToInt(StockBatchLock::getLockedQuantity).sum();
            info.put("totalLockedQuantity", totalQuantity);
            
            Map<String, List<Map<String, Object>>> locksByWarehouse = locks.stream()
                .collect(Collectors.groupingBy(
                    lock -> lock.getWarehouseName() != null ? lock.getWarehouseName() : "Unknown",
                    Collectors.mapping(lock -> {
                        Map<String, Object> lockInfo = new HashMap<>();
                        lockInfo.put("batchId", lock.getStockBatch().getId());
                        lockInfo.put("batchNumber", lock.getStockBatch().getBatchNumber());
                        lockInfo.put("lockedQuantity", lock.getLockedQuantity());
                        lockInfo.put("productName", lock.getProductName());
                        lockInfo.put("variantName", lock.getVariantName());
                        lockInfo.put("createdAt", lock.getCreatedAt());
                        lockInfo.put("expiresAt", lock.getExpiresAt());
                        return lockInfo;
                    }, Collectors.toList())
                ));
            
            info.put("locksByWarehouse", locksByWarehouse);
        }
        
        return info;
    }

    public void cleanupExpiredLocks() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<StockBatchLock> expiredLocks = stockBatchLockRepository.findByExpiresAtBefore(now);
            
            if (!expiredLocks.isEmpty()) {
                log.info("Cleaning up {} expired batch locks", expiredLocks.size());
                stockBatchLockRepository.deleteAll(expiredLocks);
                log.info("Successfully cleaned up {} expired batch locks", expiredLocks.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired batch locks: {}", e.getMessage(), e);
        }
    }
}
