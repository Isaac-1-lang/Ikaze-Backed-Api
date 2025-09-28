package com.ecommerce.scheduler;

import com.ecommerce.service.StockLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockLockCleanupScheduler {

    private final StockLockService stockLockService;

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredStockLocks() {
        try {
            log.debug("Starting cleanup of expired stock batch locks");
            stockLockService.cleanupExpiredLocks();
            log.debug("Completed cleanup of expired stock batch locks");
        } catch (Exception e) {
            log.error("Error during stock lock cleanup: {}", e.getMessage(), e);
        }
    }
}
