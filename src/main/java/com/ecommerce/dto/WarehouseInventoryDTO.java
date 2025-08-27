package com.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WarehouseInventoryDTO {
    private Long stockId;
    private Long warehouseId;
    private String warehouseName;
    private String productId;
    private String productName;
    private String productSku;
    private String variantId;
    private String variantSku;
    private Integer quantity;
    private Integer lowStockThreshold;
    private Integer reorderPoint;
    private BigDecimal productPrice;
    private BigDecimal warehousePrice;
    private BigDecimal warehouseCostPrice;
    private Boolean isAvailable;
    private String productImage;
    private String category;
    private String brand;
    private LocalDateTime lastUpdated;
    
    // Stock status indicators
    private Boolean isInStock;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Boolean needsReorder;
}
