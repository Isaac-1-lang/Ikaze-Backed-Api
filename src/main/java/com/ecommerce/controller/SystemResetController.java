package com.ecommerce.controller;

import com.ecommerce.dto.SystemResetRequest;
import com.ecommerce.dto.SystemResetResponse;
import com.ecommerce.service.SystemResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/system-reset")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System Reset Management", description = "Admin-only APIs for system reset operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SystemResetController {

    private final SystemResetService systemResetService;

    @PostMapping("/reset")
    @Operation(
        summary = "Perform system reset",
        description = "Admin-only endpoint to reset selected parts of the system. " +
                     "Deletes selected entities with cascading relationships using multithreading. " +
                     "Continues execution even if errors occur, collecting all errors for reporting. " +
                     "At least one deletion option must be selected.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "System reset completed successfully (may contain errors for specific entities)",
                content = @Content(schema = @Schema(implementation = SystemResetResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request - no deletion options selected"
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Unauthorized - authentication required"
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Forbidden - admin role required"
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        }
    )
    public ResponseEntity<?> performSystemReset(@Valid @RequestBody SystemResetRequest request) {
        try {
            log.info("System reset requested by admin");
            log.debug("Reset request details: {}", request);
            
            // Validate that at least one option is selected
            if (!request.hasAtLeastOneSelection()) {
                log.warn("System reset request rejected: no deletion options selected");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("INVALID_REQUEST", 
                            "At least one deletion option must be selected"));
            }
            
            // Perform the system reset
            SystemResetResponse response = systemResetService.performSystemReset(request);
            
            log.info("System reset completed. Total deleted: {}, Errors: {}", 
                    response.getStats().getTotalDeleted(), 
                    response.getErrors() != null ? response.getErrors().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Unexpected error during system reset: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", 
                        "An unexpected error occurred during system reset: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/products")
    @Operation(
        summary = "Delete all products",
        description = "Admin-only endpoint to delete all products with cascading relationships. " +
                     "Removes associated variants, images, videos, stocks, batches, cart items, and reviews.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Products deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllProducts() {
        try {
            log.info("Delete all products requested by admin");
            long count = systemResetService.deleteAllProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All products deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} products", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete products: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/discounts")
    @Operation(
        summary = "Delete all discounts",
        description = "Admin-only endpoint to delete all discounts with cascading relationships. " +
                     "Removes discount associations from products and variants.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Discounts deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllDiscounts() {
        try {
            log.info("Delete all discounts requested by admin");
            long count = systemResetService.deleteAllDiscounts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All discounts deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} discounts", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all discounts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete discounts: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/orders")
    @Operation(
        summary = "Delete all orders",
        description = "Admin-only endpoint to delete all orders with cascading relationships. " +
                     "Removes order items, transactions, addresses, customer info, and tracking data.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Orders deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllOrders() {
        try {
            log.info("Delete all orders requested by admin");
            long count = systemResetService.deleteAllOrders();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All orders deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} orders", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete orders: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/reward-systems")
    @Operation(
        summary = "Delete all reward systems",
        description = "Admin-only endpoint to delete all reward systems with cascading relationships. " +
                     "Removes reward ranges and associated user points.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reward systems deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllRewardSystems() {
        try {
            log.info("Delete all reward systems requested by admin");
            long count = systemResetService.deleteAllRewardSystems();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All reward systems deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} reward systems", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all reward systems: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete reward systems: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/shipping-costs")
    @Operation(
        summary = "Delete all shipping costs",
        description = "Admin-only endpoint to delete all shipping cost configurations.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Shipping costs deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllShippingCosts() {
        try {
            log.info("Delete all shipping costs requested by admin");
            long count = systemResetService.deleteAllShippingCosts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All shipping costs deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} shipping costs", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all shipping costs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete shipping costs: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/money-flows")
    @Operation(
        summary = "Delete all money flow records",
        description = "Admin-only endpoint to delete all money flow transaction records.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Money flows deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllMoneyFlows() {
        try {
            log.info("Delete all money flows requested by admin");
            long count = systemResetService.deleteAllMoneyFlows();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All money flow records deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} money flow records", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all money flows: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete money flows: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/categories")
    @Operation(
        summary = "Delete all categories",
        description = "Admin-only endpoint to delete all categories with cascading relationships. " +
                     "Removes category associations from products and child categories.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Categories deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllCategories() {
        try {
            log.info("Delete all categories requested by admin");
            long count = systemResetService.deleteAllCategories();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All categories deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} categories", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all categories: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete categories: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/brands")
    @Operation(
        summary = "Delete all brands",
        description = "Admin-only endpoint to delete all brands with cascading relationships. " +
                     "Removes brand associations from products.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Brands deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllBrands() {
        try {
            log.info("Delete all brands requested by admin");
            long count = systemResetService.deleteAllBrands();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All brands deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} brands", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all brands: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete brands: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/warehouses")
    @Operation(
        summary = "Delete all warehouses",
        description = "Admin-only endpoint to delete all warehouses with cascading relationships. " +
                     "Removes associated stocks, batches, and warehouse images.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Warehouses deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<?> deleteAllWarehouses() {
        try {
            log.info("Delete all warehouses requested by admin");
            long count = systemResetService.deleteAllWarehouses();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All warehouses deleted successfully");
            response.put("deletedCount", count);
            
            log.info("Successfully deleted {} warehouses", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting all warehouses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("DELETION_ERROR", 
                        "Failed to delete warehouses: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to create error responses
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("errorCode", errorCode);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
