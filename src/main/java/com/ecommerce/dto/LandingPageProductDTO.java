package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageProductDTO {

    private UUID productId;
    private String productName;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discountPercentage;
    private Integer stockQuantity;
    private String primaryImageUrl;
    private String primaryImageAlt;
    private Double averageRating;
    private Integer reviewCount;
    private Boolean isNew;
    private Boolean isBestseller;
    private Boolean isFeatured;
    private Boolean isInStock;
    private String brandName;
    private String categoryName;
    private String slug;
    private LocalDateTime createdAt;
    private LocalDateTime discountEndDate;
    private String discountName;
    private Boolean hasActiveDiscount;
    
    // Variant discount fields
    private Boolean hasVariantDiscounts;
    private BigDecimal maxVariantDiscount;
    private Integer discountedVariantsCount;

    // Helper method to calculate discount percentage
    public BigDecimal getDiscountPercentage() {
        if (originalPrice != null && price != null && originalPrice.compareTo(price) > 0) {
            return originalPrice.subtract(price)
                    .divide(originalPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }

    // Helper method to check if product is new (created within last 30 days)
    public Boolean getIsNew() {
        if (createdAt == null)
            return false;
        return createdAt.isAfter(LocalDateTime.now().minusDays(30));
    }

    // Helper method to check if product is in stock
    public Boolean getIsInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
}
