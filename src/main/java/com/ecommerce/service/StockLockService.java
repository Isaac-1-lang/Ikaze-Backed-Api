package com.ecommerce.service;

import com.ecommerce.entity.Stock;
import com.ecommerce.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockLockService {

    private final StockRepository stockRepository;
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

            stock.setQuantity(stock.getQuantity() - quantity);
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

                    Stock stock = stockRepository.findById(stockId).orElse(null);
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
            Map<Long, Integer> totalLocks = new HashMap<>();
            Map<Long, Integer> specificLocks = new HashMap<>();

            for (Map.Entry<Long, List<MultiWarehouseStockAllocator.StockAllocation>> entry : allocations.entrySet()) {
                for (MultiWarehouseStockAllocator.StockAllocation allocation : entry.getValue()) {
                    Long warehouseId = allocation.getWarehouseId();
                    Long stockId = allocation.getStockId();
                    Integer quantity = allocation.getQuantity();

                    Stock stock = stockRepository.findById(stockId).orElse(null);
                    if (stock == null || stock.getQuantity() < quantity) {
                        log.warn("Insufficient stock for allocation: warehouse {}, stock {}, quantity {}",
                                warehouseId, stockId, quantity);
                        releaseStock(sessionId);
                        return false;
                    }

                    stock.setQuantity(stock.getQuantity() - quantity);
                    stockRepository.save(stock);

                    // Track both general warehouse locks and specific stock locks
                    totalLocks.merge(warehouseId, quantity, Integer::sum);
                    specificLocks.put(stockId, quantity);

                    log.info("Locked {} units from stock ID {} in warehouse {} (distance: {} km)",
                            quantity, stockId, allocation.getWarehouseName(), allocation.getDistance());
                }
            }

            stockLocks.put(sessionId, totalLocks);
            specificStockLocks.put(sessionId, specificLocks);
            return true;
        } catch (Exception e) {
            log.error("Error locking stock from multiple warehouses: {}", e.getMessage());
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
}
