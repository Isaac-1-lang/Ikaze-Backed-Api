package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "warehouse_id", "product_id", "variant_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

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

    private void validateProductReference() {
        boolean hasProduct = product != null;
        boolean hasVariant = productVariant != null;

        if (!hasProduct && !hasVariant) {
            throw new IllegalStateException("Stock must reference either a product or a product variant");
        }

        if (hasProduct && hasVariant) {
            throw new IllegalStateException("Stock cannot reference both a product and a product variant");
        }
    }

    public boolean isInStock() {
        return quantity > 0;
    }

    public boolean isLowStock() {
        return quantity <= lowStockThreshold && quantity > 0;
    }

    public boolean isOutOfStock() {
        return quantity <= 0;
    }

    public Product getEffectiveProduct() {
        if (product != null) {
            return product;
        } else if (productVariant != null) {
            return productVariant.getProduct();
        }
        return null;
    }

    public boolean isVariantBased() {
        return productVariant != null;
    }

    public String getProductName() {
        Product effectiveProduct = getEffectiveProduct();
        if (effectiveProduct != null) {
            String baseName = effectiveProduct.getProductName();
            if (isVariantBased()) {
                String variantName = productVariant.getVariantName();
                return variantName.isEmpty() ? baseName : baseName + " - " + variantName;
            }
            return baseName;
        }
        return "Unknown Product";
    }

    /**
     * Gets the quantity
     * 
     * @return The quantity
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Sets the quantity
     * 
     * @param quantity The quantity to set
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets the low stock threshold
     * 
     * @return The low stock threshold
     */
    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    /**
     * Sets the low stock threshold
     * 
     * @param lowStockThreshold The low stock threshold to set
     */
    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    /**
     * Sets the product
     * 
     * @param product The product to set
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * Sets the product variant
     * 
     * @param productVariant The product variant to set
     */
    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }
}
