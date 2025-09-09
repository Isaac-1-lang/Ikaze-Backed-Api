package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    /**
     * Gets the stock quantity (delegates to product or variant)
     */
    public Integer getStockQuantity() {
        if (productVariant != null) {
            return productVariant.getStockQuantity();
        }
        if (product != null) {
            return product.getStockQuantity();
        }
        return 0;
    }

    /**
     * Gets the SKU (delegates to product or variant)
     */
    public String getSku() {
        if (productVariant != null) {
            return productVariant.getVariantSku();
        }
        if (product != null) {
            return product.getSku();
        }
        return null;
    }

    /**
     * Gets the product name (delegates to product or variant)
     */
    public String getProductName() {
        if (productVariant != null && productVariant.getProduct() != null) {
            return productVariant.getProduct().getProductName() + " - " + productVariant.getVariantName();
        }
        if (product != null) {
            return product.getProductName();
        }
        return null;
    }

    /**
     * Gets the images (delegates to product)
     */
    public java.util.List<ProductImage> getImages() {
        if (product != null) {
            return product.getImages();
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Gets the variants (delegates to product)
     */
    public java.util.List<ProductVariant> getVariants() {
        if (product != null) {
            return product.getVariants();
        }
        return java.util.Collections.emptyList();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    // Product reference (for products without variants)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Product variant reference (for products with variants)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    /**
     * Sets the quantity of this cart item, ensuring it doesn't exceed the available
     * stock.
     * 
     * @param quantity The quantity to set
     * @throws IllegalArgumentException if quantity exceeds available stock
     */
    public void setQuantity(int quantity) {
        if (productVariant != null && quantity > productVariant.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Quantity cannot exceed available stock of " + productVariant.getStockQuantity());
        }
        if (product != null && quantity > product.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Quantity cannot exceed available stock of " + product.getStockQuantity());
        }
        this.quantity = quantity;
    }

    /**
     * Sets the cart for this item
     * 
     * @param cart The cart to set
     */
    public void setCart(Cart cart) {
        this.cart = cart;
    }

    /**
     * Gets the quantity of this cart item
     * 
     * @return The quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Gets the product variant of this cart item (if it's a variant-based item)
     * 
     * @return The product variant, or null if this is a product-based item
     */
    public ProductVariant getProductVariant() {
        return productVariant;
    }

    /**
     * Gets the product of this cart item (if it's a product-based item)
     * 
     * @return The product, or null if this is a variant-based item
     */
    public Product getProduct() {
        return product;
    }

    /**
     * Gets the effective product (either the direct product or the product from
     * variant)
     * 
     * @return The product
     */
    public Product getEffectiveProduct() {
        if (product != null) {
            return product;
        }
        if (productVariant != null) {
            return productVariant.getProduct();
        }
        return null;
    }

    /**
     * Gets the effective price (either from product or variant)
     * 
     * @return The price
     */
    public BigDecimal getEffectivePrice() {
        if (productVariant != null) {
            return productVariant.getPrice();
        }
        if (product != null) {
            return product.getDiscountedPrice();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the effective stock quantity (either from product or variant)
     * 
     * @return The stock quantity
     */
    public Integer getEffectiveStockQuantity() {
        if (productVariant != null) {
            return productVariant.getStockQuantity();
        }
        if (product != null) {
            return product.getStockQuantity();
        }
        return 0;
    }

    /**
     * Gets the effective SKU (either from product or variant)
     * 
     * @return The SKU
     */
    public String getEffectiveSku() {
        if (productVariant != null) {
            return productVariant.getVariantSku();
        }
        if (product != null) {
            return product.getSku();
        }
        return null;
    }

    /**
     * Gets the effective name (either from product or variant)
     * 
     * @return The name
     */
    public String getEffectiveName() {
        if (productVariant != null) {
            return productVariant.getProduct().getProductName() + " - " + productVariant.getVariantName();
        }
        if (product != null) {
            return product.getProductName();
        }
        return null;
    }

    /**
     * Checks if this cart item is for a product variant
     * 
     * @return true if it's a variant-based item
     */
    public boolean isVariantBased() {
        return productVariant != null;
    }

    /**
     * Checks if this cart item is for a product without variants
     * 
     * @return true if it's a product-based item
     */
    public boolean isProductBased() {
        return product != null && productVariant == null;
    }

    /**
     * Validates that the cart item has either a product or variant, but not both
     * 
     * @throws IllegalStateException if the item is not properly configured
     */
    public void validateConfiguration() {
        if (product == null && productVariant == null) {
            throw new IllegalStateException("Cart item must have either a product or product variant");
        }
        if (product != null && productVariant != null) {
            throw new IllegalStateException("Cart item cannot have both product and product variant");
        }
    }
}