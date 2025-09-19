package com.ecommerce.controller;

import com.ecommerce.dto.StockBatchDTO;
import com.ecommerce.dto.CreateStockBatchRequest;
import com.ecommerce.dto.UpdateStockBatchRequest;
import com.ecommerce.service.StockBatchService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-batches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stock Batch Management", description = "APIs for managing stock batches")
@SecurityRequirement(name = "bearerAuth")
public class StockBatchController {

    private final StockBatchService stockBatchService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Create a new stock batch", description = "Create a new stock batch for a specific stock entry", responses = {
            @ApiResponse(responseCode = "201", description = "Stock batch created successfully", content = @Content(schema = @Schema(implementation = StockBatchDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Stock entry not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createStockBatch(@Valid @RequestBody CreateStockBatchRequest request) {
        try {
            log.info("Creating stock batch for stock ID: {}", request.getStockId());
            StockBatchDTO stockBatch = stockBatchService.createStockBatch(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(stockBatch);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating stock batch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating stock batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to create stock batch"));
        }
    }

    @GetMapping("/stock/{stockId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all batches for a stock entry", description = "Retrieve all stock batches for a specific stock entry", responses = {
            @ApiResponse(responseCode = "200", description = "Stock batches retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Stock entry not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getStockBatchesByStockId(@PathVariable Long stockId) {
        try {
            log.info("Retrieving stock batches for stock ID: {}", stockId);
            List<StockBatchDTO> batches = stockBatchService.getStockBatchesByStockId(stockId);
            return ResponseEntity.ok(batches);
        } catch (IllegalArgumentException e) {
            log.error("Stock not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("STOCK_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving stock batches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to retrieve stock batches"));
        }
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all batches for a product", description = "Retrieve all stock batches for a specific product across all warehouses", responses = {
            @ApiResponse(responseCode = "200", description = "Stock batches retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getStockBatchesByProductId(@PathVariable UUID productId) {
        try {
            log.info("Retrieving stock batches for product ID: {}", productId);
            List<StockBatchDTO> batches = stockBatchService.getStockBatchesByProductId(productId);
            return ResponseEntity.ok(batches);
        } catch (IllegalArgumentException e) {
            log.error("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving stock batches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to retrieve stock batches"));
        }
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get stock batch by ID", description = "Retrieve a specific stock batch by its ID", responses = {
            @ApiResponse(responseCode = "200", description = "Stock batch retrieved successfully", content = @Content(schema = @Schema(implementation = StockBatchDTO.class))),
            @ApiResponse(responseCode = "404", description = "Stock batch not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getStockBatchById(@PathVariable Long batchId) {
        try {
            log.info("Retrieving stock batch with ID: {}", batchId);
            StockBatchDTO stockBatch = stockBatchService.getStockBatchById(batchId);
            return ResponseEntity.ok(stockBatch);
        } catch (IllegalArgumentException e) {
            log.error("Stock batch not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("BATCH_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving stock batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to retrieve stock batch"));
        }
    }

    @PutMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Update stock batch", description = "Update an existing stock batch", responses = {
            @ApiResponse(responseCode = "200", description = "Stock batch updated successfully", content = @Content(schema = @Schema(implementation = StockBatchDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Stock batch not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateStockBatch(@PathVariable Long batchId,
            @Valid @RequestBody UpdateStockBatchRequest request) {
        try {
            log.info("Updating stock batch with ID: {}", batchId);
            StockBatchDTO stockBatch = stockBatchService.updateStockBatch(batchId, request);
            return ResponseEntity.ok(stockBatch);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating stock batch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating stock batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to update stock batch"));
        }
    }

    @DeleteMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Delete stock batch", description = "Delete a stock batch", responses = {
            @ApiResponse(responseCode = "200", description = "Stock batch deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Stock batch not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteStockBatch(@PathVariable Long batchId) {
        try {
            log.info("Deleting stock batch with ID: {}", batchId);
            stockBatchService.deleteStockBatch(batchId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Stock batch deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Stock batch not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("BATCH_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting stock batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to delete stock batch"));
        }
    }

    @PostMapping("/{batchId}/recall")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Recall stock batch", description = "Recall a stock batch due to quality issues or safety concerns", responses = {
            @ApiResponse(responseCode = "200", description = "Stock batch recalled successfully", content = @Content(schema = @Schema(implementation = StockBatchDTO.class))),
            @ApiResponse(responseCode = "404", description = "Stock batch not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> recallStockBatch(@PathVariable Long batchId,
            @RequestParam(required = false) String reason) {
        try {
            log.info("Recalling stock batch with ID: {}", batchId);
            StockBatchDTO stockBatch = stockBatchService.recallStockBatch(batchId, reason);
            return ResponseEntity.ok(stockBatch);
        } catch (IllegalArgumentException e) {
            log.error("Stock batch not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("BATCH_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error recalling stock batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to recall stock batch"));
        }
    }

    @GetMapping("/expiring-soon")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get batches expiring soon", description = "Retrieve batches that are expiring within a specified number of days", responses = {
            @ApiResponse(responseCode = "200", description = "Expiring batches retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getBatchesExpiringSoon(@RequestParam(defaultValue = "30") int daysThreshold) {
        try {
            log.info("Retrieving batches expiring within {} days", daysThreshold);
            List<StockBatchDTO> batches = stockBatchService.getBatchesExpiringSoon(daysThreshold);
            return ResponseEntity.ok(batches);
        } catch (Exception e) {
            log.error("Error retrieving expiring batches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to retrieve expiring batches"));
        }
    }

    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        return Map.of(
                "error", errorCode,
                "message", message,
                "timestamp", java.time.LocalDateTime.now());
    }
}