package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long id;
    private UUID productId; // For products without variants
    private Long variantId; // For products with variants
    private String sku; // SKU (either product SKU or variant SKU)
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private BigDecimal weight;
    private LocalDateTime addedAt;
    private boolean inStock;
    private Integer availableStock;
    private boolean isVariantBased; // Flag to indicate if this is a variant-based item
}
