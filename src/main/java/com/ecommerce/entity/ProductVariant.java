package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private java.util.List<Stock> stocks;

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
     * Gets the price
     * 
     * @return The price
     */
    public BigDecimal getPrice() {
        return price;
    }

    public boolean isInStock() {
        return stocks != null && stocks.stream()
                .anyMatch(stock -> stock.getQuantity() > 0);
    }

    public boolean isLowStock() {
        return stocks != null && stocks.stream()
                .anyMatch(stock -> stock.getQuantity() <= stock.getLowStockThreshold() && stock.getQuantity() > 0);
    }

    public boolean isOutOfStock() {
        return stocks == null || stocks.stream()
                .allMatch(stock -> stock.getQuantity() <= 0);
    }

    public Integer getTotalStockQuantity() {
        return stocks != null ? stocks.stream()
                .mapToInt(Stock::getQuantity)
                .sum() : 0;
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

    /**
     * Adds a stock entry for this product variant
     * 
     * @param stock The stock to add
     */
    public void addStock(Stock stock) {
        if (stocks == null) {
            stocks = new java.util.ArrayList<>();
        }
        stocks.add(stock);
        stock.setProductVariant(this);
    }

    /**
     * Removes a stock entry from this product variant
     * 
     * @param stock The stock to remove
     */
    public void removeStock(Stock stock) {
        if (stocks != null) {
            stocks.remove(stock);
            stock.setProductVariant(null);
        }
    }

    /**
     * Gets the variant SKU
     * 
     * @return The variant SKU
     */
    public String getVariantSku() {
        return variantSku;
    }

    /**
     * Sets the variant SKU
     * 
     * @param variantSku The variant SKU to set
     */
    public void setVariantSku(String variantSku) {
        this.variantSku = variantSku;
    }

    /**
     * Gets the ID
     * 
     * @return The ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the ID
     * 
     * @param id The ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }
}