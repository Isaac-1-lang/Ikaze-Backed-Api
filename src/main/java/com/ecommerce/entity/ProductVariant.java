package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "variant_sku", unique = true)
    private String variantSku;

    @Column(name = "variant_barcode")
    private String variantBarcode;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @NotNull(message = "Stock quantity is required")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<ProductVariantImage> images;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<VariantAttributeValue> attributeValues;

    @OneToMany(mappedBy = "productVariant", fetch = FetchType.LAZY)
    private java.util.List<CartItem> cartItems;

    @OneToMany(mappedBy = "productVariant", fetch = FetchType.LAZY)
    private java.util.List<OrderItem> orderItems;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Gets the stock quantity
     * 
     * @return The stock quantity
     */
    public Integer getStockQuantity() {
        return stockQuantity;
    }

    /**
     * Gets the price
     * 
     * @return The price
     */
    public BigDecimal getPrice() {
        return price;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold && stockQuantity > 0;
    }

    public boolean isOutOfStock() {
        return stockQuantity <= 0;
    }

    public String getVariantName() {
        if (attributeValues == null || attributeValues.isEmpty()) {
            return "";
        }

        StringBuilder name = new StringBuilder();

        // Get all attribute values and build a name from them
        attributeValues.stream()
                .map(varAttr -> varAttr.getAttributeValue())
                .forEach(attrValue -> {
                    if (name.length() > 0) {
                        name.append(" - ");
                    }
                    name.append(attrValue.getValue());
                });

        return name.toString();
    }

    public Product getProduct() {
        return product;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}