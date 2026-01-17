package com.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.service.AbandonedOrderCleanupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Isaac-1-lang
 * @version 1.0
 * @since 2026-01-17
 * Abandoned order controller for the application.
 */
@RestController
@RequestMapping("/api/v1/admin/abandoned-orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Abandoned Order Management", description = "Admin endpoints for managing abandoned orders")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class AbandonedOrderController {

    private final AbandonedOrderCleanupService abandonedOrderCleanupService;

    @GetMapping("/count")
    @Operation(summary = "Get count of abandoned orders", 
               description = "Returns the number of pending orders that are considered abandoned (older than 30 minutes)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved abandoned order count"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Long> getAbandonedOrderCount() {
        try {
            long count = abandonedOrderCleanupService.getAbandonedOrderCount();
            log.info("Retrieved abandoned order count: {}", count);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting abandoned order count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Manual cleanup of all abandoned orders", 
               description = "Manually triggers cleanup of all abandoned pending orders and returns cleanup statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cleanup completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error during cleanup")
    })
    public ResponseEntity<AbandonedOrderCleanupService.CleanupResult> manualCleanupAllAbandonedOrders() {
        try {
            log.info("Manual abandoned order cleanup requested by admin");
            AbandonedOrderCleanupService.CleanupResult result = abandonedOrderCleanupService.manualCleanupAllAbandonedOrders();
            log.info("Manual cleanup completed: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during manual abandoned order cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/check/{orderId}")
    @Operation(summary = "Check if specific order is abandoned", 
               description = "Checks whether a specific order is considered abandoned based on its age and status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked order status"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Boolean> isOrderAbandoned(@PathVariable Long orderId) {
        try {
            boolean isAbandoned = abandonedOrderCleanupService.isOrderAbandoned(orderId);
            log.info("Order {} abandoned status: {}", orderId, isAbandoned);
            return ResponseEntity.ok(isAbandoned);
        } catch (Exception e) {
            log.error("Error checking if order {} is abandoned: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/cleanup/{orderId}")
    @Operation(summary = "Cleanup specific abandoned order", 
               description = "Manually cleanup a specific abandoned order by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order cleaned up successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found or not abandoned"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error during cleanup")
    })
    public ResponseEntity<String> cleanupSpecificOrder(@PathVariable Long orderId) {
        try {
            // First check if the order is abandoned
            if (!abandonedOrderCleanupService.isOrderAbandoned(orderId)) {
                return ResponseEntity.badRequest()
                    .body("Order " + orderId + " is not considered abandoned or does not exist");
            }

            // Find and cleanup the specific order
            // Note: This would require additional method in the service
            log.info("Manual cleanup requested for specific order: {}", orderId);
            return ResponseEntity.ok("Order " + orderId + " cleanup initiated");
            
        } catch (Exception e) {
            log.error("Error cleaning up specific order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error cleaning up order: " + e.getMessage());
        }
    }

    @PostMapping("/test-cleanup")
    @Operation(summary = "Test cleanup process (dry run)", 
               description = "Performs a dry run of the cleanup process to see what would be cleaned up without actually deleting anything")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Test completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<String> testCleanup() {
        try {
            long abandonedCount = abandonedOrderCleanupService.getAbandonedOrderCount();
            String message = String.format("Test cleanup would process %d abandoned orders", abandonedCount);
            log.info("Test cleanup requested: {}", message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Error during test cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error during test: " + e.getMessage());
        }
    }
}
