package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockDTO {
    private Long stockId;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseAddress;
    private String warehouseCity;
    private String warehouseState;
    private String warehouseCountry;
    private String warehouseContactNumber;
    private String warehouseEmail;
    private Integer quantity;
    private Integer lowStockThreshold;
    private Boolean isInStock;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For variant-based stock
    private Long variantId;
    private String variantSku;
    private String variantName;
    private Boolean isVariantBased;

    // Batch information
    private List<StockBatchDTO> batches;
    private Integer totalBatches;
    private Integer activeBatches;
    private Integer expiredBatches;
    private Integer recalledBatches;

    // Convenience method for backward compatibility
    public Integer getStockQuantity() {
        return quantity;
    }
}