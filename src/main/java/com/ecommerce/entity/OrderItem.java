package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Price is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateProductReference();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateProductReference();
    }

    /**
     * Validates that exactly one of product or productVariant is set
     */
    private void validateProductReference() {
        boolean hasProduct = product != null;
        boolean hasVariant = productVariant != null;

        if (!hasProduct && !hasVariant) {
            throw new IllegalStateException("OrderItem must have either a product or a productVariant, but not both");
        }

        if (hasProduct && hasVariant) {
            throw new IllegalStateException("OrderItem cannot have both a product and a productVariant");
        }
    }

    /**
     * Calculates the subtotal for this order item
     * 
     * @return The subtotal
     */
    @Transient
    public BigDecimal getSubtotal() {
        return price.multiply(new BigDecimal(quantity));
    }

    /**
     * Creates an order item from a cart item
     * 
     * @param cartItem The cart item
     * @return The order item
     */
    public static OrderItem fromCartItem(CartItem cartItem) {
        OrderItem orderItem = new OrderItem();

        if (cartItem.isVariantBased()) {
            orderItem.setProductVariant(cartItem.getProductVariant());
            orderItem.setPrice(cartItem.getProductVariant().getPrice());
        } else {
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setPrice(cartItem.getProduct().getDiscountedPrice());
        }

        orderItem.setQuantity(cartItem.getQuantity());
        return orderItem;
    }

    /**
     * Sets the order for this item
     * 
     * @param order The order to set
     */
    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * Sets the product
     * 
     * @param product The product to set
     */
    public void setProduct(Product product) {
        if (product != null && this.productVariant != null) {
            throw new IllegalStateException("Cannot set product when productVariant is already set");
        }
        this.product = product;
    }

    /**
     * Sets the product variant
     * 
     * @param productVariant The product variant to set
     */
    public void setProductVariant(ProductVariant productVariant) {
        if (productVariant != null && this.product != null) {
            throw new IllegalStateException("Cannot set productVariant when product is already set");
        }
        this.productVariant = productVariant;
    }

    /**
     * Get product (convenience method for analytics)
     */
    public Product getProduct() {
        if (productVariant != null) {
            return productVariant.getProduct();
        }
        return product;
    }

    /**
     * Debug method to check the state of this OrderItem
     */
    public String getDebugInfo() {
        return String.format("OrderItem[ID=%d, hasProduct=%s, hasVariant=%s, productId=%s, variantId=%s]",
                orderItemId,
                product != null,
                productVariant != null,
                product != null ? product.getProductId() : "null",
                productVariant != null ? productVariant.getId() : "null");
    }

    /**
     * Get effective product for this order item
     */
    public Product getEffectiveProduct() {
        return getProduct();
    }

    /**
     * Check if this is a variant-based order item
     */
    public boolean isVariantBased() {
        return productVariant != null;
    }

    /**
     * Get effective price for this order item
     */
    public BigDecimal getEffectivePrice() {
        if (productVariant != null) {
            return productVariant.getPrice();
        }
        if (product != null) {
            return product.getDiscountedPrice();
        }
        return price;
    }

    /**
     * Get available stock for this order item
     */
    public Integer getAvailableStock() {
        if (productVariant != null) {
            return productVariant.getStockQuantity();
        }
        if (product != null) {
            return product.getStockQuantity();
        }
        return 0;
    }

    /**
     * Sets the quantity
     * 
     * @param quantity The quantity to set
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}