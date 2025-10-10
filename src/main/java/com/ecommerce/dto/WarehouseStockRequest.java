package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockRequest {

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @Size(max = 255, message = "Warehouse name must not exceed 255 characters")
    private String warehouseName;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    private Integer stockQuantity;

    @NotNull(message = "Low stock threshold is required")
    @Min(value = 0, message = "Low stock threshold must be non-negative")
    private Integer lowStockThreshold;

    @Valid
    private List<BatchRequest> batches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRequest {

        @NotNull(message = "Batch number is required")
        @Size(max = 100, message = "Batch number must not exceed 100 characters")
        private String batchNumber;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private java.time.LocalDateTime manufactureDate;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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
