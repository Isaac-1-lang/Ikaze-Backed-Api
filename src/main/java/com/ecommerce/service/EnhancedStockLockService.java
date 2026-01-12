package com.ecommerce.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.entity.StockBatch;
import com.ecommerce.entity.StockBatchLock;
import com.ecommerce.repository.StockBatchLockRepository;
import com.ecommerce.repository.StockBatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedStockLockService {

    private final StockBatchLockRepository stockBatchLockRepository;
    private final StockBatchRepository stockBatchRepository;
    
    // Lock expiration time in minutes
    private static final int LOCK_EXPIRY_MINUTES = 120; // 2 hours

    /**
     * Locks stock by temporarily reducing quantities from StockBatch entities
     */
    @Transactional
    public boolean lockStockBatches(String sessionId, List<BatchLockRequest> lockRequests) {
        try {
            log.info("Starting batch-level stock locking for session: {}", sessionId);
            
            List<StockBatchLock> locksToCreate = new ArrayList<>();
            List<StockBatch> batchesToUpdate = new ArrayList<>();
            
            // Check if session already has locks
            List<StockBatchLock> existingLocks = stockBatchLockRepository.findBySessionId(sessionId);
            if (!existingLocks.isEmpty()) {
                log.warn("Session {} already has {} existing locks", sessionId, existingLocks.size());
                return true; // Already locked
            }
            // Process each lock request
            for (BatchLockRequest request : lockRequests) {
                StockBatch batch = stockBatchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new RuntimeException("StockBatch not found: " + request.getBatchId()));
                
                // Check if batch has enough available quantity
                if (batch.getQuantity() < request.getQuantity()) {
                    log.error("Insufficient quantity in batch {}. Available: {}, Requested: {}", 
                            batch.getBatchNumber(), batch.getQuantity(), request.getQuantity());
                    
                    // Rollback all changes made so far
                    rollbackBatchChanges(batchesToUpdate, locksToCreate);
                    return false;
                }
                
                // Create lock record
                StockBatchLock lock = createBatchLock(sessionId, batch, request.getQuantity(), 
                                                    request.getWarehouseId(), request.getProductName(), 
                                                    request.getVariantName());
                locksToCreate.add(lock);
                
                // Temporarily reduce batch quantity
                batch.reduceQuantity(request.getQuantity());
                batchesToUpdate.add(batch);
                
                log.debug("Locked {} units from batch {} (remaining: {})", 
                        request.getQuantity(), batch.getBatchNumber(), batch.getQuantity());
            }
            
            // Save all locks and batch updates
            stockBatchLockRepository.saveAll(locksToCreate);
            stockBatchRepository.saveAll(batchesToUpdate);
            
            log.info("Successfully locked stock for session {}: {} locks created, {} batches updated", 
                    sessionId, locksToCreate.size(), batchesToUpdate.size());
            
            return true;
            
        } catch (Exception e) {
            log.error("Error locking stock for session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Unlocks all batches for a session by restoring quantities and deleting locks
     */
    @Transactional
    public void unlockAllBatches(String sessionId) {
        try {
            log.info("Unlocking all batches for session: {}", sessionId);
            
            List<StockBatchLock> locks = stockBatchLockRepository.findBySessionId(sessionId);
            
            if (locks.isEmpty()) {
                log.debug("No locks found for session: {}", sessionId);
                return;
            }
            
            List<StockBatch> batchesToUpdate = new ArrayList<>();
            
            int totalRestoredQuantity = 0;
            for (StockBatchLock lock : locks) {
                StockBatch batch = lock.getStockBatch();
                if (batch != null) {
                    int previousQuantity = batch.getQuantity();
                    int quantityToRestore = lock.getLockedQuantity();
                    
                    batch.increaseQuantity(quantityToRestore);
                    batchesToUpdate.add(batch);
                    totalRestoredQuantity += quantityToRestore;
                    
                    log.info("STOCK UNLOCK: Batch {} - Previous: {}, Restoring: {}, New: {} (Product: {}, Variant: {})", 
                            batch.getBatchNumber(), previousQuantity, quantityToRestore, 
                            batch.getQuantity(), lock.getProductName(), lock.getVariantName());
                }
            }
            
            // Save batch updates and delete locks
            stockBatchRepository.saveAll(batchesToUpdate);
            stockBatchLockRepository.deleteAll(locks);
            
            log.info("Successfully unlocked {} batches for session: {} (Total quantity restored: {})", 
                    locks.size(), sessionId, totalRestoredQuantity);
            
        } catch (Exception e) {
            log.error("Error unlocking batches for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to unlock batches: " + e.getMessage(), e);
        }
    }

    /**
     * Confirms locks by deleting lock records (quantities already reduced)
     */
    @Transactional
    public void confirmBatchLocks(String sessionId) {
        try {
            log.info("Confirming batch locks for session: {}", sessionId);
            
            List<StockBatchLock> locks = stockBatchLockRepository.findBySessionId(sessionId);
            
            if (locks.isEmpty()) {
                log.debug("No locks found to confirm for session: {}", sessionId);
                return;
            }
            
            // Just delete the lock records - quantities are already reduced
            stockBatchLockRepository.deleteAll(locks);
            
            log.info("Successfully confirmed {} batch locks for session: {}", locks.size(), sessionId);
            
        } catch (Exception e) {
            log.error("Error confirming batch locks for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to confirm batch locks: " + e.getMessage(), e);
        }
    }

    /**
     * Transfers locks from one session to another (for Stripe session ID updates)
     */
    @Transactional
    public void transferBatchLocks(String fromSessionId, String toSessionId) {
        try {
            log.info("Transferring batch locks from {} to {}", fromSessionId, toSessionId);
            
            List<StockBatchLock> locks = stockBatchLockRepository.findBySessionId(fromSessionId);
            
            if (locks.isEmpty()) {
                log.debug("No locks found to transfer from session: {}", fromSessionId);
                return;
            }
            
            // Update session IDs
            for (StockBatchLock lock : locks) {
                lock.setSessionId(toSessionId);
            }
            
            stockBatchLockRepository.saveAll(locks);
            
            log.info("Successfully transferred {} batch locks from {} to {}", 
                    locks.size(), fromSessionId, toSessionId);
            
        } catch (Exception e) {
            log.error("Error transferring batch locks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to transfer batch locks: " + e.getMessage(), e);
        }
    }

    /**
     * Gets detailed lock information for debugging
     */
    public Map<String, Object> getBatchLockInfo(String sessionId) {
        List<StockBatchLock> locks = stockBatchLockRepository.findBySessionId(sessionId);
        
        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("totalLocks", locks.size());
        info.put("hasLocks", !locks.isEmpty());
        
        if (!locks.isEmpty()) {
            int totalLockedQuantity = locks.stream().mapToInt(StockBatchLock::getLockedQuantity).sum();
            info.put("totalLockedQuantity", totalLockedQuantity);
            
            Map<String, List<Map<String, Object>>> locksByWarehouse = new HashMap<>();
            
            for (StockBatchLock lock : locks) {
                String warehouseName = lock.getWarehouseName() != null ? lock.getWarehouseName() : "Unknown";
                
                Map<String, Object> lockInfo = new HashMap<>();
                lockInfo.put("batchId", lock.getStockBatch().getId());
                lockInfo.put("batchNumber", lock.getStockBatch().getBatchNumber());
                lockInfo.put("lockedQuantity", lock.getLockedQuantity());
                lockInfo.put("productName", lock.getProductName());
                lockInfo.put("variantName", lock.getVariantName());
                lockInfo.put("createdAt", lock.getCreatedAt());
                lockInfo.put("expiresAt", lock.getExpiresAt());
                
                locksByWarehouse.computeIfAbsent(warehouseName, k -> new ArrayList<>()).add(lockInfo);
            }
            
            info.put("locksByWarehouse", locksByWarehouse);
        }
        
        return info;
    }

    /**
     * Scheduled cleanup of expired locks
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredLocks() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(LOCK_EXPIRY_MINUTES);
            List<StockBatchLock> expiredLocks = stockBatchLockRepository.findExpiredLocks(cutoffTime);
            
            if (expiredLocks.isEmpty()) {
                return;
            }
            
            log.info("Found {} expired batch locks to cleanup", expiredLocks.size());
            
            List<StockBatch> batchesToUpdate = new ArrayList<>();
            
            for (StockBatchLock lock : expiredLocks) {
                StockBatch batch = lock.getStockBatch();
                if (batch != null) {
                    batch.increaseQuantity(lock.getLockedQuantity());
                    batchesToUpdate.add(batch);
                    
                    log.debug("Restored {} units to batch {} from expired lock", 
                            lock.getLockedQuantity(), batch.getBatchNumber());
                }
            }
            
            stockBatchRepository.saveAll(batchesToUpdate);
            stockBatchLockRepository.deleteAll(expiredLocks);
            
            log.info("Successfully cleaned up {} expired batch locks", expiredLocks.size());
            
        } catch (Exception e) {
            log.error("Error during expired lock cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates a batch lock record
     */
    private StockBatchLock createBatchLock(String sessionId, StockBatch batch, int quantity, 
                                         Long warehouseId, String productName, String variantName) {
        StockBatchLock lock = new StockBatchLock();
        lock.setSessionId(sessionId);
        lock.setStockBatch(batch);
        lock.setLockedQuantity(quantity);
        lock.setWarehouseId(warehouseId);
        lock.setWarehouseName(batch.getWarehouseName());
        lock.setProductId(batch.getStock().getProduct() != null ? batch.getStock().getProduct().getProductId() : null);
        lock.setVariantId(batch.getStock().getProductVariant() != null ? batch.getStock().getProductVariant().getId() : null);
        lock.setProductName(productName != null ? productName : batch.getEffectiveProductName());
        lock.setVariantName(variantName);
        lock.setCreatedAt(LocalDateTime.now());
        lock.setExpiresAt(LocalDateTime.now().plusMinutes(LOCK_EXPIRY_MINUTES));
        
        return lock;
    }

    /**
     * Rollbacks batch changes in case of error
     */
    private void rollbackBatchChanges(List<StockBatch> batchesToUpdate, List<StockBatchLock> locksToCreate) {
        try {
            for (StockBatch batch : batchesToUpdate) {
                // Find corresponding lock to get the quantity that was reduced
                for (StockBatchLock lock : locksToCreate) {
                    if (lock.getStockBatch().getId().equals(batch.getId())) {
                        batch.increaseQuantity(lock.getLockedQuantity());
                        break;
                    }
                }
            }
            
            // Save the restored batches
            if (!batchesToUpdate.isEmpty()) {
                stockBatchRepository.saveAll(batchesToUpdate);
            }
            
            log.info("Successfully rolled back {} batch changes", batchesToUpdate.size());
            
        } catch (Exception e) {
            log.error("Error during rollback: {}", e.getMessage(), e);
        }
    }

    /**
     * Request class for batch locking
     */
    public static class BatchLockRequest {
        private Long batchId;
        private int quantity;
        private Long warehouseId;
        private String productName;
        private String variantName;

        // Constructors
        public BatchLockRequest() {}

        public BatchLockRequest(Long batchId, int quantity, Long warehouseId, String productName, String variantName) {
            this.batchId = batchId;
            this.quantity = quantity;
            this.warehouseId = warehouseId;
            this.productName = productName;
            this.variantName = variantName;
        }

        // Getters and setters
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public String getVariantName() { return variantName; }
        public void setVariantName(String variantName) { this.variantName = variantName; }
    }
}
