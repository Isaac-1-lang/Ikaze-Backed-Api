package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseProductDTO {
    private UUID productId;
    private String productName;
    private String productSku;
    private Integer totalQuantity; // Total quantity from all active batches for this product
    private Integer activeBatchCount; // Number of active batches for this product
    private Integer expiredBatchCount; // Number of expired batches for this product
    private Integer recalledBatchCount; // Number of recalled batches for this product
    private Integer lowStockThreshold; // Minimum threshold across all stock entries
    private Boolean isLowStock; // Whether the product is below threshold
    private Boolean isOutOfStock; // Whether the product has no active stock
    private List<String> productImages;
    private String productDescription;
    private Double productPrice;
}
