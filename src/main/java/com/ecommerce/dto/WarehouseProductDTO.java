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
    private Long stockId;
    private UUID productId;
    private String productName;
    private String productSku;
    private Long variantId;
    private String variantSku;
    private Integer quantity;
    private Integer lowStockThreshold;
    private Boolean isVariant;
    private List<String> productImages;
}
