package com.ecommerce.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.entity.Order;
import com.ecommerce.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbandonedOrderCleanupService {

    private final OrderRepository orderRepository;
    private final RewardService rewardService;
    private final EnhancedStockLockService enhancedStockLockService;

    /**
     * Clean up a single abandoned order
     */
    @Transactional
    public void cleanupSingleAbandonedOrder(Order order) {
        try {
            log.info("Cleaning up abandoned order: {} (created: {})",
                    order.getOrderId(), order.getCreatedAt());

            // Refund points if any were used
            refundPointsForAbandonedOrder(order);

            // Unlock stock batches for this order
            unlockStockBatchesForOrder(order);

            // Delete the order
            orderRepository.delete(order);

            log.info("Successfully cleaned up abandoned order: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Error cleaning up abandoned order {}: {}", order.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Scheduled cleanup of abandoned orders - runs every 5 minutes
     * Only cleans up orders that are older than 10 minutes to avoid deleting orders
     * that are currently being processed
     */
    @Scheduled(fixedRate = 50000)
    @Transactional
    public void scheduledCleanupAbandonedOrders() {
        try {
            log.info("🔄 SCHEDULED CLEANUP STARTED - Looking for abandoned orders...");

            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(10);

            List<Order> abandonedOrders;
            int totalCleaned = 0;

            do {
                Pageable pageable = PageRequest.of(0, 20);
                abandonedOrders = orderRepository.findAbandonedPendingOrders(cutoffTime, pageable);

                for (Order order : abandonedOrders) {
                    try {
                        cleanupSingleAbandonedOrder(order);
                        totalCleaned++;
                    } catch (Exception e) {
                        log.error("Failed to cleanup abandoned order {} during scheduled cleanup: {}",
                                order.getOrderId(), e.getMessage());
                    }
                }

            } while (!abandonedOrders.isEmpty());

            if (totalCleaned > 0) {
                log.info("SCHEDULED CLEANUP COMPLETED: cleaned up {} abandoned orders", totalCleaned);
            } else {
                log.info(" SCHEDULED CLEANUP COMPLETED: no abandoned orders found (cutoff: {})", cutoffTime);
            }

        } catch (Exception e) {
            log.error("Error during scheduled abandoned order cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Unlock stock batches for an abandoned order
     */
    private void unlockStockBatchesForOrder(Order order) {
        try {
            if (order.getOrderTransaction() != null && order.getOrderTransaction().getStripeSessionId() != null) {
                String sessionId = order.getOrderTransaction().getStripeSessionId();
                enhancedStockLockService.unlockAllBatches(sessionId);
                log.debug("Unlocked stock batches for session: {}", sessionId);
            } else {
                // For orders without stripe session, try to unlock using order ID as session
                String tempSessionId = "temp_" + order.getOrderId().toString();
                enhancedStockLockService.unlockAllBatches(tempSessionId);
                log.debug("Unlocked stock batches for temp session: {}", tempSessionId);

                // Also try guest session format
                String tempGuestSessionId = "temp_guest_" + order.getOrderId().toString();
                enhancedStockLockService.unlockAllBatches(tempGuestSessionId);
                log.debug("Unlocked stock batches for temp guest session: {}", tempGuestSessionId);
            }
        } catch (Exception e) {
            log.error("Error unlocking stock batches for abandoned order {}: {}",
                    order.getOrderId(), e.getMessage());
            // Don't throw exception here - we still want to clean up the order
        }
    }

    private void refundPointsForAbandonedOrder(Order order) {
        try {
            if (order.getOrderTransaction() == null) {
                return; // No transaction, no points to refund
            }

            Integer pointsUsed = order.getOrderTransaction().getPointsUsed();
            if (pointsUsed == null || pointsUsed <= 0) {
                return; // No points were used, nothing to refund
            }

            if (order.getUser() == null) {
                log.warn("Cannot refund points for order {} - no user associated", order.getOrderId());
                return;
            }

            // This was a hybrid payment that was abandoned/cancelled
            String refundDescription = String.format("Points refunded for cancelled hybrid payment (Order #%s)",
                    order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());

            rewardService.refundPointsForCancelledOrder(
                    order.getUser().getId(),
                    pointsUsed,
                    refundDescription);

            log.info("Refunded {} points to user {} for abandoned hybrid payment order {}",
                    pointsUsed, order.getUser().getId(), order.getOrderId());

        } catch (Exception e) {
            log.error("Error refunding points for abandoned order {}: {}",
                    order.getOrderId(), e.getMessage(), e);
            // Don't throw exception here - we still want to clean up the order
        }
    }

    /**
     * Get count of abandoned orders for monitoring
     */
    public long getAbandonedOrderCount() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30); // 30 minutes default
        return orderRepository.countAbandonedPendingOrders(cutoffTime);
    }

    /**
     * Check if a specific order is considered abandoned
     */
    public boolean isOrderAbandoned(Long orderId) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30); // 30 minutes default
        return orderRepository.isOrderAbandoned(orderId, cutoffTime);
    }

    /**
     * Manual cleanup method for admin use - cleans up all abandoned orders
     */
    @Transactional
    public CleanupResult manualCleanupAllAbandonedOrders() {
        log.info("Starting manual cleanup of all abandoned orders");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);

        int totalProcessed = 0;
        int totalSuccessful = 0;
        int totalErrors = 0;

        List<Order> abandonedOrders;

        // Process in batches to avoid memory issues
        do {
            Pageable pageable = PageRequest.of(0, 50); // 50 batch size default
            abandonedOrders = orderRepository.findAbandonedPendingOrders(cutoffTime, pageable);

            for (Order order : abandonedOrders) {
                totalProcessed++;
                try {
                    cleanupSingleAbandonedOrder(order);
                    totalSuccessful++;
                } catch (Exception e) {
                    totalErrors++;
                    log.error("Failed to cleanup order {} during manual cleanup: {}",
                            order.getOrderId(), e.getMessage());
                }
            }

        } while (!abandonedOrders.isEmpty());

        CleanupResult result = new CleanupResult(totalProcessed, totalSuccessful, totalErrors);
        log.info("Manual cleanup completed: {}", result);

        return result;
    }

    /**
     * Cleanup specific order by ID
     */
    @Transactional
    public void cleanupSpecificOrder(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (isOrderAbandoned(orderId)) {
                cleanupSingleAbandonedOrder(order);
            } else {
                throw new IllegalStateException("Order " + orderId + " is not abandoned");
            }
        } else {
            throw new IllegalArgumentException("Order " + orderId + " not found");
        }
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

        public int getTotalProcessed() {
            return totalProcessed;
        }

        public int getSuccessful() {
            return successful;
        }

        public int getErrors() {
            return errors;
        }

        @Override
        public String toString() {
            return String.format("CleanupResult{processed=%d, successful=%d, errors=%d}",
                    totalProcessed, successful, errors);
        }
    }
}
