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
            return productVariant.getTotalStockQuantity();
        }
        if (product != null) {
            return product.getTotalStockQuantity();
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
        if (productVariant != null && quantity > productVariant.getTotalStockQuantity()) {
            throw new IllegalArgumentException(
                    "Quantity cannot exceed available stock of " + productVariant.getTotalStockQuantity());
        }
        if (product != null && quantity > product.getTotalStockQuantity()) {
            throw new IllegalArgumentException(
                    "Quantity cannot exceed available stock of " + product.getTotalStockQuantity());
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
     * Gets the effective price (either from product or variant) with discount applied
     * 
     * @return The price
     */
    public BigDecimal getEffectivePrice() {
        if (productVariant != null) {
            // Check if variant has its own discount
            if (productVariant.getDiscount() != null && isDiscountActive(productVariant.getDiscount())) {
                return calculateDiscountedPrice(productVariant.getPrice(), productVariant.getDiscount().getPercentage());
            }
            // If product has discount, apply it to variant
            if (productVariant.getProduct().getDiscount() != null && isDiscountActive(productVariant.getProduct().getDiscount())) {
                return calculateDiscountedPrice(productVariant.getPrice(), productVariant.getProduct().getDiscount().getPercentage());
            }
            return productVariant.getPrice();
        }
        if (product != null) {
            // Check if product has discount
            if (product.getDiscount() != null && isDiscountActive(product.getDiscount())) {
                return calculateDiscountedPrice(product.getPrice(), product.getDiscount().getPercentage());
            }
            return product.getPrice();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the original price before any discounts
     * 
     * @return The original price
     */
    public BigDecimal getOriginalPrice() {
        if (productVariant != null) {
            return productVariant.getPrice();
        }
        if (product != null) {
            return product.getPrice();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the discount percentage applied to this cart item
     * 
     * @return The discount percentage, or 0 if no discount
     */
    public BigDecimal getDiscountPercentage() {
        if (productVariant != null) {
            // Check if variant has its own discount
            if (productVariant.getDiscount() != null && isDiscountActive(productVariant.getDiscount())) {
                return productVariant.getDiscount().getPercentage();
            }
            // If product has discount, return product discount percentage
            if (productVariant.getProduct().getDiscount() != null && isDiscountActive(productVariant.getProduct().getDiscount())) {
                return productVariant.getProduct().getDiscount().getPercentage();
            }
        }
        if (product != null) {
            if (product.getDiscount() != null && isDiscountActive(product.getDiscount())) {
                return product.getDiscount().getPercentage();
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the discount name applied to this cart item
     * 
     * @return The discount name, or null if no discount
     */
    public String getDiscountName() {
        if (productVariant != null) {
            // Check if variant has its own discount
            if (productVariant.getDiscount() != null && isDiscountActive(productVariant.getDiscount())) {
                return productVariant.getDiscount().getName();
            }
            // If product has discount, return product discount name
            if (productVariant.getProduct().getDiscount() != null && isDiscountActive(productVariant.getProduct().getDiscount())) {
                return productVariant.getProduct().getDiscount().getName();
            }
        }
        if (product != null) {
            if (product.getDiscount() != null && isDiscountActive(product.getDiscount())) {
                return product.getDiscount().getName();
            }
        }
        return null;
    }

    /**
     * Checks if a discount is currently active based on its date range
     * 
     * @param discount The discount to check
     * @return true if the discount is active
     */
    private boolean isDiscountActive(com.ecommerce.entity.Discount discount) {
        if (discount == null || !discount.isActive()) {
            return false;
        }
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return (discount.getStartDate() == null || now.isAfter(discount.getStartDate()) || now.isEqual(discount.getStartDate())) &&
               (discount.getEndDate() == null || now.isBefore(discount.getEndDate()) || now.isEqual(discount.getEndDate()));
    }

    /**
     * Calculates the discounted price based on original price and discount percentage
     * 
     * @param originalPrice The original price
     * @param discountPercentage The discount percentage
     * @return The discounted price
     */
    private BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, BigDecimal discountPercentage) {
        if (originalPrice == null || discountPercentage == null) {
            return originalPrice;
        }
        return originalPrice.multiply(BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100))));
    }

    /**
     * Gets the effective stock quantity (either from product or variant)
     * 
     * @return The stock quantity
     */
    public Integer getEffectiveStockQuantity() {
        if (productVariant != null) {
            return productVariant.getTotalStockQuantity();
        }
        if (product != null) {
            return product.getTotalStockQuantity();
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