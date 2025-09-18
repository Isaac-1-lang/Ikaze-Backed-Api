package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "productDetail", "images", "videos", "variants", "reviews" })
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID productId;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Column(nullable = false)
    private String productName;

    @Column(name = "short_description")
    private String shortDescription;

    @NotBlank(message = "SKU is required")
    @Column(unique = true, nullable = false)
    private String sku;

    @Column(name = "barcode")
    private String barcode;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @JsonBackReference
    private Brand brand;

    @Column(name = "model")
    private String model;

    @Column(name = "slug", unique = true)
    private String slug;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_featured")
    private boolean isFeatured = false;

    @Column(name = "is_bestseller")
    private boolean isBestseller = false;

    @Column(name = "is_new_arrival")
    private boolean isNewArrival = false;

    @Column(name = "is_on_sale")
    private boolean isOnSale = false;

    @Column(name = "sale_percentage")
    private Integer salePercentage;

    @Column(name = "maximum_days_for_return")
    private Integer maximumDaysForReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    @JsonBackReference
    private Discount discount;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ProductDetail productDetail;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ProductVideo> videos = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Stock> stocks = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (slug == null || slug.trim().isEmpty()) {
            slug = generateSlug(productName);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
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
        if (variants != null && !variants.isEmpty()) {
            return variants.stream()
                    .mapToInt(ProductVariant::getTotalStockQuantity)
                    .sum();
        }
        return stocks != null ? stocks.stream()
                .mapToInt(Stock::getQuantity)
                .sum() : 0;
    }

    public BigDecimal getDiscountedPrice() {
        if (discount != null && discount.isValid()) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    discount.getPercentage().divide(BigDecimal.valueOf(100.0)));
            return price.multiply(discountMultiplier);
        } else if (isOnSale && salePercentage != null && salePercentage > 0) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(salePercentage).divide(BigDecimal.valueOf(100.0)));
            return price.multiply(discountMultiplier);
        }
        return price;
    }

    public BigDecimal getDiscountAmount() {
        return price.subtract(getDiscountedPrice());
    }

    /**
     * Checks if this product has variants
     * 
     * @return true if product has variants
     */
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    /**
     * Checks if this product has any active discount (either direct discount or
     * sale)
     * 
     * @return true if product has active discount
     */
    public boolean hasActiveDiscount() {
        return (discount != null && discount.isValid()) ||
                (isOnSale && salePercentage != null && salePercentage > 0);
    }

    public String getDescription() {
        return productDetail != null ? productDetail.getDescription() : null;
    }

    public String getMetaTitle() {
        return productDetail != null ? productDetail.getMetaTitle() : null;
    }

    public String getMetaDescription() {
        return productDetail != null ? productDetail.getMetaDescription() : null;
    }

    public String getMetaKeywords() {
        return productDetail != null ? productDetail.getMetaKeywords() : null;
    }

    public String getSearchKeywords() {
        return productDetail != null ? productDetail.getSearchKeywords() : null;
    }

    public String getDimensionsCm() {
        return productDetail != null ? productDetail.getDimensionsCm() : null;
    }

    public BigDecimal getWeightKg() {
        return productDetail != null ? productDetail.getWeightKg() : null;
    }

    public String getMainImageUrl() {
        return images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    public double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Gets the product ID
     * 
     * @return The product ID
     */
    public UUID getProductId() {
        return productId;
    }

    public int getReviewCount() {
        return reviews != null ? reviews.size() : 0;
    }

    /**
     * Adds a stock entry for this product
     * 
     * @param stock The stock to add
     */
    public void addStock(Stock stock) {
        if (stocks == null) {
            stocks = new ArrayList<>();
        }
        stocks.add(stock);
        stock.setProduct(this);
    }

    /**
     * Removes a stock entry from this product
     * 
     * @param stock The stock to remove
     */
    public void removeStock(Stock stock) {
        if (stocks != null) {
            stocks.remove(stock);
            stock.setProduct(null);
        }
    }

    /**
     * Sets the category
     * 
     * @param category The category to set
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Sets the brand
     * 
     * @param brand The brand to set
     */
    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    /**
     * Sets the discount
     * 
     * @param discount The discount to set
     */
    public void setDiscount(Discount discount) {
        this.discount = discount;
    }

    /**
     * Sets the model
     * 
     * @param model The model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Sets the slug
     * 
     * @param slug The slug to set
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * Sets the active status
     * 
     * @param isActive The active status to set
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Sets the featured status
     * 
     * @param isFeatured The featured status to set
     */
    public void setFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    /**
     * Sets the bestseller status
     * 
     * @param isBestseller The bestseller status to set
     */
    public void setBestseller(boolean isBestseller) {
        this.isBestseller = isBestseller;
    }

    /**
     * Sets the new arrival status
     * 
     * @param isNewArrival The new arrival status to set
     */
    public void setNewArrival(boolean isNewArrival) {
        this.isNewArrival = isNewArrival;
    }

    /**
     * Sets the on sale status
     * 
     * @param isOnSale The on sale status to set
     */
    public void setOnSale(boolean isOnSale) {
        this.isOnSale = isOnSale;
    }

    /**
     * Sets the product detail
     * 
     * @param productDetail The product detail to set
     */
    public void setProductDetail(ProductDetail productDetail) {
        this.productDetail = productDetail;
    }

    /**
     * Gets the product name
     * 
     * @return The product name
     */
    public String getProductName() {
        return productName;
    }

    /**
     * Sets the product name
     * 
     * @param productName The product name to set
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * Gets the SKU
     * 
     * @return The SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Sets the SKU
     * 
     * @param sku The SKU to set
     */
    public void setSku(String sku) {
        this.sku = sku;
    }
}