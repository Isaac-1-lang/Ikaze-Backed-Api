package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    @JsonBackReference
    private Discount discount;

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
        return getTotalStockQuantity() > 0;
    }

    public boolean isLowStock() {
        if (stocks == null || stocks.isEmpty()) {
            return false;
        }
        
        int totalQuantity = getTotalStockQuantity();
        int totalThreshold = stocks.stream()
                .mapToInt(Stock::getLowStockThreshold)
                .sum();
        
        return totalQuantity <= totalThreshold && totalQuantity > 0;
    }

    public boolean isOutOfStock() {
        return getTotalStockQuantity() <= 0;
    }

    public Integer getTotalStockQuantity() {
        if (stocks == null || stocks.isEmpty()) {
            return 0;
        }
        
        // Calculate stock directly from active batches instead of Stock.quantity
        return stocks.stream()
                .mapToInt(Stock::getTotalBatchQuantity)
                .sum();
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

    /**
     * Gets the discounted price for this variant
     * Priority: Variant discount > Product discount > Sale percentage
     * 
     * @return The discounted price
     */
    public BigDecimal getDiscountedPrice() {
        // First check if variant has its own discount
        if (discount != null && discount.isValid()) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    discount.getPercentage().divide(BigDecimal.valueOf(100.0)));
            return price.multiply(discountMultiplier);
        }

        // Then check if parent product has discount
        if (product != null && product.getDiscount() != null && product.getDiscount().isValid()) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    product.getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
            return price.multiply(discountMultiplier);
        }

        // Finally check if product is on sale
        if (product != null && product.isOnSale() && product.getSalePercentage() != null
                && product.getSalePercentage() > 0) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(product.getSalePercentage()).divide(BigDecimal.valueOf(100.0)));
            return price.multiply(discountMultiplier);
        }

        return price;
    }

    /**
     * Gets the discount amount for this variant
     * 
     * @return The discount amount
     */
    public BigDecimal getDiscountAmount() {
        return price.subtract(getDiscountedPrice());
    }

    /**
     * Checks if this variant has any active discount
     * 
     * @return true if variant has active discount
     */
    public boolean hasActiveDiscount() {
        return (discount != null && discount.isValid()) ||
                (product != null && product.getDiscount() != null && product.getDiscount().isValid()) ||
                (product != null && product.isOnSale() && product.getSalePercentage() != null
                        && product.getSalePercentage() > 0);
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

    /**
     * Gets the discount
     * 
     * @return The discount
     */
    public Discount getDiscount() {
        return discount;
    }

    /**
     * Sets the discount
     * 
     * @param discount The discount to set
     */
    public void setDiscount(Discount discount) {
        this.discount = discount;
    }
}