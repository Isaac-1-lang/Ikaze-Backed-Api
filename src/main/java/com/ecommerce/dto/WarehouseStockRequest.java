package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
}
