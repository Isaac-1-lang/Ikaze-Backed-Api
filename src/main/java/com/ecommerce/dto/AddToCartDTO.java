package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartDTO {
    
    // Product ID (for products without variants)
    private UUID productId;
    
    // Product variant ID (for products with variants)
    private Long variantId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Validates that either productId or variantId is provided, but not both
     */
    @AssertTrue(message = "Either productId or variantId must be provided, but not both")
    public boolean isValidProductReference() {
        return (productId != null && variantId == null) || 
               (productId == null && variantId != null);
    }

    /**
     * Checks if this is a product-based cart item (no variants)
     */
    public boolean isProductBased() {
        return productId != null && variantId == null;
    }

    /**
     * Checks if this is a variant-based cart item
     */
    public boolean isVariantBased() {
        return variantId != null && productId == null;
    }
}

