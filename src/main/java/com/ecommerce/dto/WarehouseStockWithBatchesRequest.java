package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockWithBatchesRequest {

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @Size(max = 255, message = "Warehouse name must not exceed 255 characters")
    private String warehouseName;

    @NotNull(message = "Low stock threshold is required")
    @Min(value = 0, message = "Low stock threshold must be non-negative")
    private Integer lowStockThreshold;

    @NotEmpty(message = "At least one batch is required")
    @Valid
    private List<StockBatchRequest> batches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockBatchRequest {

        @NotNull(message = "Batch number is required")
        @Size(max = 100, message = "Batch number must not exceed 100 characters")
        private String batchNumber;

        private java.time.LocalDateTime manufactureDate;

        private java.time.LocalDateTime expiryDate;

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must be non-negative")
        private Integer quantity;

        @Size(max = 255, message = "Supplier name must not exceed 255 characters")
        private String supplierName;

        @Size(max = 100, message = "Supplier batch number must not exceed 100 characters")
        private String supplierBatchNumber;

    }
}
