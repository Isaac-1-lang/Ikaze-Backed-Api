package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantWarehouseDTO {
    private Long variantId;
    private String variantName;
    private String variantSku;
    private Integer totalQuantity;
    private Integer activeBatchCount;
    private Integer expiredBatchCount;
    private Integer recalledBatchCount;
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private List<String> variantImages;
    private Double variantPrice;
    private Long stockId; // For reference
}
