package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Collection of stock batches for this stock entry
     * Each Stock can have multiple StockBatches to track different batches
     * of the same product/variant in the same warehouse
     */
    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<StockBatch> stockBatches = new ArrayList<>();

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
        return getTotalBatchQuantity() > 0;
    }

    public boolean isLowStock() {
        int batchQuantity = getTotalBatchQuantity();
        return batchQuantity <= lowStockThreshold && batchQuantity > 0;
    }

    public boolean isOutOfStock() {
        return getTotalBatchQuantity() <= 0;
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
     * Gets the quantity from active batches (replaces direct quantity field usage)
     * 
     * @return The total quantity from active batches
     */
    public Integer getQuantity() {
        return getTotalBatchQuantity();
    }

    /**
     * Sets the quantity - deprecated, use batch management instead
     * This method is kept for backward compatibility but should not be used
     * 
     * @param quantity The quantity to set
     * @deprecated Use batch management through StockBatch entities instead
     */
    @Deprecated
    public void setQuantity(Integer quantity) {
        throw new Error("Direct quantity setting is deprecated. Use batch management instead.");
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

    /**
     * Adds a stock batch to this stock entry
     * 
     * @param stockBatch The stock batch to add
     */
    public void addStockBatch(StockBatch stockBatch) {
        if (stockBatches == null) {
            stockBatches = new ArrayList<>();
        }
        stockBatches.add(stockBatch);
        stockBatch.setStock(this);
    }

    /**
     * Removes a stock batch from this stock entry
     * 
     * @param stockBatch The stock batch to remove
     */
    public void removeStockBatch(StockBatch stockBatch) {
        if (stockBatches != null) {
            stockBatches.remove(stockBatch);
            stockBatch.setStock(null);
        }
    }

    /**
     * Gets the total quantity from all active batches
     * 
     * @return Total quantity from active batches
     */
    public Integer getTotalBatchQuantity() {
        if (stockBatches == null || stockBatches.isEmpty()) {
            return 0;
        }
        return stockBatches.stream()
                .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.ACTIVE)
                .mapToInt(StockBatch::getQuantity)
                .sum();
    }

    /**
     * Gets the total quantity from all batches (including expired, empty, etc.)
     * 
     * @return Total quantity from all batches
     */
    public Integer getTotalAllBatchQuantity() {
        if (stockBatches == null || stockBatches.isEmpty()) {
            return 0;
        }
        return stockBatches.stream()
                .mapToInt(StockBatch::getQuantity)
                .sum();
    }

    /**
     * Checks if this stock has any batches
     * 
     * @return true if stock has batches
     */
    public boolean hasBatches() {
        return stockBatches != null && !stockBatches.isEmpty();
    }

    /**
     * Gets the number of active batches
     * 
     * @return Number of active batches
     */
    public int getActiveBatchCount() {
        if (stockBatches == null) {
            return 0;
        }
        return (int) stockBatches.stream()
                .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.ACTIVE)
                .count();
    }

    /**
     * Gets the number of expired batches
     * 
     * @return Number of expired batches
     */
    public int getExpiredBatchCount() {
        if (stockBatches == null) {
            return 0;
        }
        return (int) stockBatches.stream()
                .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.EXPIRED)
                .count();
    }

    /**
     * Gets the number of recalled batches
     * 
     * @return Number of recalled batches
     */
    public int getRecalledBatchCount() {
        if (stockBatches == null) {
            return 0;
        }
        return (int) stockBatches.stream()
                .filter(batch -> batch.getStatus() == com.ecommerce.enums.BatchStatus.RECALLED)
                .count();
    }
}
