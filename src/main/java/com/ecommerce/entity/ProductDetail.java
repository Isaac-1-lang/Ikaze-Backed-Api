package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "product" })
public class ProductDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description")
    private String metaDescription;

    @Column(name = "meta_keywords")
    private String metaKeywords;

    @Column(name = "search_keywords")
    private String searchKeywords;

    @Column(name = "dimensions_cm")
    private String dimensionsCm;

    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "material")
    private String material;

    @Column(name = "care_instructions", columnDefinition = "TEXT")
    private String careInstructions;

    @Column(name = "warranty_info", columnDefinition = "TEXT")
    private String warrantyInfo;

    @Column(name = "shipping_info", columnDefinition = "TEXT")
    private String shippingInfo;

    @Column(name = "return_policy", columnDefinition = "TEXT")
    private String returnPolicy;

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
     * Gets the description
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the meta title
     * 
     * @return The meta title
     */
    public String getMetaTitle() {
        return metaTitle;
    }

    /**
     * Gets the meta description
     * 
     * @return The meta description
     */
    public String getMetaDescription() {
        return metaDescription;
    }

    /**
     * Gets the meta keywords
     * 
     * @return The meta keywords
     */
    public String getMetaKeywords() {
        return metaKeywords;
    }

    /**
     * Gets the search keywords
     * 
     * @return The search keywords
     */
    public String getSearchKeywords() {
        return searchKeywords;
    }

    /**
     * Gets the dimensions in cm
     * 
     * @return The dimensions
     */
    public String getDimensionsCm() {
        return dimensionsCm;
    }

    /**
     * Gets the weight in kg
     * 
     * @return The weight
     */
    public BigDecimal getWeightKg() {
        return weightKg;
    }

    /**
     * Sets the product
     * 
     * @param product The product to set
     */
    public void setProduct(Product product) {
        this.product = product;
    }
}