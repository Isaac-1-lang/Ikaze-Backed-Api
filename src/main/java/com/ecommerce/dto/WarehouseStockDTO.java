package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WarehouseStockDTO {
    
    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;
    
    @Positive(message = "Stock quantity must be positive")
    private Integer stockQuantity;
    
    @Positive(message = "Low stock threshold must be positive")
    private Integer lowStockThreshold;
    
    @Positive(message = "Reorder point must be positive")
    private Integer reorderPoint;
    
    // Optional fields for warehouse-specific pricing
    private BigDecimal warehousePrice;
    private BigDecimal warehouseCostPrice;
    
    // Optional fields for warehouse-specific availability
    private Boolean isAvailable;
    private String notes;
}
