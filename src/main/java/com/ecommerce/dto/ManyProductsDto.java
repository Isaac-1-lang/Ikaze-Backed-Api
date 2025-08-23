package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for displaying multiple products in cards
 * Contains only essential fields needed for product listing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManyProductsDto {

    private UUID productId;
    private String productName;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private Integer stockQuantity;
    private CategorySummary category;
    private BrandSummary brand;
    private Boolean isBestSeller;
    private Boolean isFeatured;
    private Object discountInfo;
    private ImageSummary primaryImage;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategorySummary {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BrandSummary {
        private UUID brandId;
        private String brandName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageSummary {
        private Long id;
        private String imageUrl;
        private String altText;
        private boolean isPrimary;
        private Integer sortOrder;
    }
}
