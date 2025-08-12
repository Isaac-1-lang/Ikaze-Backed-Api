package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private UUID productId;
    private String name;
    private String description;
    private String sku;
    private String barcode;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private BigDecimal discountedPrice;
    private Integer stockQuantity;
    private Long categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String model;
    private String slug;
    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isBestseller;
    private Boolean isNewArrival;
    private Boolean isOnSale;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Product media
    private List<ProductImageDTO> images;
    private List<ProductVideoDTO> videos;

    // Product variants
    private List<ProductVariantDTO> variants;

    // Product details
    private String fullDescription;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String dimensionsCm;
    private BigDecimal weightKg;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageDTO {
        private Long imageId;
        private String url;
        private String altText;
        private Boolean isPrimary;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVideoDTO {
        private Long videoId;
        private String url;
        private String title;
        private String description;
        private Integer sortOrder;
        private Integer durationSeconds;
    }
}
