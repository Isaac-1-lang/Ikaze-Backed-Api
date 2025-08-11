package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductDTO {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String barcode;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be positive")
    private BigDecimal basePrice;

    private BigDecimal salePrice;

    private BigDecimal costPrice;

    private Integer stockQuantity;

    private Integer lowStockThreshold;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private UUID brandId;

    private String model;

    private String slug;

    private Boolean isActive;

    private Boolean isFeatured;

    private Boolean isBestseller;

    private Boolean isNewArrival;

    private Boolean isOnSale;

    private Integer salePercentage;

    private UUID discountId;

    // Product detail fields
    private String fullDescription;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String searchKeywords;
    private String dimensionsCm;
    private BigDecimal weightKg;

    // Media files
    private List<MultipartFile> productImages;
    private List<ImageMetadata> imageMetadata;
    private List<MultipartFile> productVideos;
    private List<VideoMetadata> videoMetadata;

    // Variants
    private List<CreateProductVariantDTO> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageMetadata {
        private String altText;
        private Boolean isPrimary;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoMetadata {
        private String title;
        private String description;
        private Integer sortOrder;
        private Integer durationSeconds; // To validate video duration
    }
}
