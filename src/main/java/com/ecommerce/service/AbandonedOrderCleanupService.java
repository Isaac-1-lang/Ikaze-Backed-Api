package com.ecommerce.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.config.AbandonedOrderProperties;
import com.ecommerce.entity.Order;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.CheckoutService;
import com.ecommerce.service.StockLockService;
import com.ecommerce.service.EnhancedStockLockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.abandoned-order-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AbandonedOrderCleanupService {

    private final OrderRepository orderRepository;
    private final CheckoutService checkoutService;
    private final StockLockService stockLockService;
    private final EnhancedStockLockService enhancedStockLockService;
    private final AbandonedOrderProperties properties;

    /**
     * Scheduled task that runs every 10 minutes to clean up abandoned orders
     */
    @Scheduled(cron = "${app.abandoned-order-cleanup.cleanup-schedule:0 0/10 * * * *}")
    @Transactional
    public void cleanupAbandonedOrders() {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            log.info("Starting scheduled cleanup of abandoned orders");
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(properties.getExpiryMinutes());
            
            // Find pending orders older than cutoff time
            Pageable pageable = PageRequest.of(0, properties.getBatchSize());
            List<Order> abandonedOrders = orderRepository.findAbandonedPendingOrders(cutoffTime, pageable);
            
            if (abandonedOrders.isEmpty()) {
                if (properties.isDetailedLogging()) {
                    log.debug("No abandoned orders found for cleanup");
                }
                return;
            }
            
            log.info("Found {} abandoned orders to clean up", abandonedOrders.size());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (Order order : abandonedOrders) {
                try {
                    if (properties.isDryRun()) {
                        log.info("DRY RUN: Would cleanup abandoned order: {} (created: {})", 
                                order.getOrderId(), order.getCreatedAt());
                    } else {
                        cleanupSingleAbandonedOrder(order);
                    }
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to cleanup abandoned order {}: {}", order.getOrderId(), e.getMessage(), e);
                    errorCount++;
                }
            }
            
            log.info("Abandoned order cleanup completed: {} successful, {} errors", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("Error during scheduled abandoned order cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up a single abandoned order
     */
    @Transactional
    public void cleanupSingleAbandonedOrder(Order order) {
        try {
            if (properties.isDetailedLogging()) {
                log.info("Cleaning up abandoned order: {} (created: {})", 
                        order.getOrderId(), order.getCreatedAt());
            }
            
            String sessionId = null;
            if (order.getOrderTransaction() != null) {
                sessionId = order.getOrderTransaction().getStripeSessionId();
                
                if (sessionId == null) {
                    sessionId = "temp_" + order.getOrderId().toString();
                } else {
                    String tempSessionId = "temp_" + order.getOrderId().toString();
                    String tempGuestSessionId = "temp_guest_" + order.getOrderId().toString();
                    
                    enhancedStockLockService.unlockAllBatches(tempSessionId);
                    stockLockService.releaseStock(tempSessionId);
                    enhancedStockLockService.unlockAllBatches(tempGuestSessionId);
                    stockLockService.releaseStock(tempGuestSessionId);
                }
            }
            
            if (sessionId != null) {
                enhancedStockLockService.unlockAllBatches(sessionId);
                stockLockService.releaseStock(sessionId);
            }
            
            // Delete the order (cascades to related entities)
            orderRepository.delete(order);
            
            if (properties.isDetailedLogging()) {
                log.info("Successfully cleaned up abandoned order: {}", order.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("Error cleaning up abandoned order {}: {}", order.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Manual cleanup method for admin use - cleans up all abandoned orders
     */
    @Transactional
    public CleanupResult manualCleanupAllAbandonedOrders() {
        log.info("Starting manual cleanup of all abandoned orders");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(properties.getExpiryMinutes());
        
        int totalProcessed = 0;
        int totalSuccessful = 0;
        int totalErrors = 0;
        
        List<Order> abandonedOrders;
        
        // Process in batches to avoid memory issues
        do {
            Pageable pageable = PageRequest.of(0, properties.getBatchSize());
            abandonedOrders = orderRepository.findAbandonedPendingOrders(cutoffTime, pageable);
            
            for (Order order : abandonedOrders) {
                totalProcessed++;
                try {
                    if (properties.isDryRun()) {
                        log.info("DRY RUN: Would cleanup order: {}", order.getOrderId());
                    } else {
                        cleanupSingleAbandonedOrder(order);
                    }
                    totalSuccessful++;
                } catch (Exception e) {
                    totalErrors++;
                    log.error("Failed to cleanup order {} during manual cleanup: {}", 
                             order.getOrderId(), e.getMessage());
                }
            }
            
        } while (!abandonedOrders.isEmpty() && !properties.isDryRun());
        
        CleanupResult result = new CleanupResult(totalProcessed, totalSuccessful, totalErrors);
        log.info("Manual cleanup completed: {}", result);
        
        return result;
    }

    /**
     * Get count of abandoned orders for monitoring
     */
    public long getAbandonedOrderCount() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(properties.getExpiryMinutes());
        return orderRepository.countAbandonedPendingOrders(cutoffTime);
    }

    /**
     * Check if a specific order is considered abandoned
     */
    public boolean isOrderAbandoned(Long orderId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(properties.getExpiryMinutes());
        return orderRepository.isOrderAbandoned(orderId, cutoffTime);
    }

    /**
     * Result class for cleanup operations
     */
    public static class CleanupResult {
        private final int totalProcessed;
        private final int successful;
        private final int errors;

        public CleanupResult(int totalProcessed, int successful, int errors) {
            this.totalProcessed = totalProcessed;
            this.successful = successful;
            this.errors = errors;
        }

        public int getTotalProcessed() { return totalProcessed; }
        public int getSuccessful() { return successful; }
        public int getErrors() { return errors; }

        @Override
        public String toString() {
            return String.format("CleanupResult{processed=%d, successful=%d, errors=%d}", 
                               totalProcessed, successful, errors);
        }
    }
}
