package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing individual items within a return request
 */
@Entity
@Table(name = "return_items")
@Data
@ToString(exclude = { "returnRequest", "orderItem", "product", "productVariant" })
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "returnRequest", "orderItem", "product", "productVariant" })
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Return request is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @NotNull(message = "Order item is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    @NotNull(message = "Return quantity is required")
    @Min(value = 1, message = "Return quantity must be at least 1")
    @Column(name = "return_quantity", nullable = false)
    private Integer returnQuantity;

    @Column(name = "item_reason", length = 500)
    private String itemReason; // Specific reason for this item

    @Column(name = "created_at", nullable = false, updatable = false)
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
     * Validates that the product/variant matches the order item
     */
    private void validateProductReference() {
        if (orderItem == null) {
            return;
        }

        boolean orderItemHasProduct = orderItem.getProduct() != null;
        boolean orderItemHasVariant = orderItem.getProductVariant() != null;
        boolean returnItemHasProduct = product != null;
        boolean returnItemHasVariant = productVariant != null;

        if (orderItemHasVariant) {
            if (!returnItemHasVariant) {
                throw new IllegalStateException("Return item must reference the same variant as order item");
            }
            if (!orderItem.getProductVariant().getId().equals(productVariant.getId())) {
                throw new IllegalStateException("Return item variant must match order item variant");
            }
            return;
        }

        if (orderItemHasProduct) {
            if (!returnItemHasProduct) {
                throw new IllegalStateException("Return item must reference the same product as order item");
            }
            if (!orderItem.getProduct().getProductId().equals(product.getProductId())) {
                throw new IllegalStateException("Return item product must match order item product");
            }
            return;
        }

        // If we reach here, the order item has neither product nor variant, which
        // shouldn't happen
        throw new IllegalStateException("Order item must have either a product or variant reference");
    }

    /**
     * Check if this return item is for a variant-based product
     */
    public boolean isVariantBased() {
        return productVariant != null;
    }

    /**
     * Get the effective product (either direct product or variant's product)
     */
    public Product getEffectiveProduct() {
        if (productVariant != null) {
            return productVariant.getProduct();
        }
        return product;
    }

    /**
     * Get the product ID for this return item
     */
    public UUID getEffectiveProductId() {
        Product effectiveProduct = getEffectiveProduct();
        return effectiveProduct != null ? effectiveProduct.getProductId() : null;
    }

    /**
     * Get the variant ID for this return item (null if not variant-based)
     */
    public Long getEffectiveVariantId() {
        return productVariant != null ? productVariant.getId() : null;
    }

    /**
     * Validate that return quantity doesn't exceed order quantity
     */
    public void validateReturnQuantity() {
        if (orderItem != null && returnQuantity > orderItem.getQuantity()) {
            throw new IllegalArgumentException(
                    String.format("Return quantity (%d) cannot exceed order quantity (%d)",
                            returnQuantity, orderItem.getQuantity()));
        }
    }
}
