package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

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
    private BigDecimal discountedPrice;
    private Integer stockQuantity;
    private SimpleCategoryDto category;
    private SimpleBrandDto brand;
    private Boolean isBestSeller;
    private Boolean isNew;
    private Boolean isFeatured;
    private SimpleDiscountDto discountInfo;
    private SimpleProductImageDto primaryImage;
    private Double averageRating;
    private Integer reviewCount;
    private Boolean hasActiveDiscount;

    /**
     * Simplified Category DTO to prevent infinite recursion
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleCategoryDto {
        private Long id;
        private String name;
        private String description;
        private String slug;
        private String imageUrl;
    }

    /**
     * Simplified Brand DTO to prevent infinite recursion
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleBrandDto {
        private UUID brandId;
        private String brandName;
        private String description;
        private String logoUrl;
    }

    /**
     * Simplified Discount DTO to prevent infinite recursion
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleDiscountDto {
        private UUID discountId;
        private String name;
        private BigDecimal percentage;
        private String startDate;
        private String endDate;
        private Boolean active;
        private Boolean isValid;
        private String discountCode;
    }

    /**
     * Simplified Product Image DTO to prevent infinite recursion
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleProductImageDto {
        private Long id;
        private String imageUrl;
        private String altText;
        private String title;
        private Boolean isPrimary;
        private Integer sortOrder;
        private Integer width;
        private Integer height;
        private Long fileSize;
        private String mimeType;
    }
}
