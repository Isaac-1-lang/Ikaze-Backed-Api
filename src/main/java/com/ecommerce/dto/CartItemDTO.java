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
    private BigDecimal price; // Current price (with discount applied)
    private BigDecimal originalPrice; // Original price before discount
    private BigDecimal totalPrice;
    private BigDecimal weight;
    private LocalDateTime addedAt;
    private boolean inStock;
    private Integer availableStock;
    private boolean isVariantBased; // Flag to indicate if this is a variant-based item

    // Discount information
    private BigDecimal discountPercentage;
    private String discountName;
    private BigDecimal discountAmount; // Amount saved due to discount
    private boolean hasDiscount;

    /**
     * Determines if this cart item is variant-based
     * A cart item is variant-based if it has a variantId (regardless of the
     * isVariantBased flag)
     * This method ensures consistent logic regardless of how the frontend sends the
     * data
     */
    public boolean isVariantBasedItem() {
        return variantId != null;
    }

    /**
     * Determines if this cart item is product-based (no variants)
     * A cart item is product-based if it has a productId but no variantId
     */
    public boolean isProductBasedItem() {
        return productId != null && variantId == null;
    }
}
