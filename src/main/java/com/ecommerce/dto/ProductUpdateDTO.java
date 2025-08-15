package com.ecommerce.dto;

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
public class ProductUpdateDTO {

    // Basic product fields - all optional for partial updates
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 50, message = "Barcode must not exceed 50 characters")
    private String barcode;

    @Positive(message = "Base price must be positive")
    private BigDecimal basePrice;

    @Positive(message = "Sale price must be positive")
    private BigDecimal salePrice;

    @Positive(message = "Cost price must be positive")
    private BigDecimal costPrice;

    @Positive(message = "Stock quantity must be positive")
    private Integer stockQuantity;

    @Positive(message = "Low stock threshold must be positive")
    private Integer lowStockThreshold;

    private Long categoryId;

    private UUID brandId;

    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;

    private Boolean isActive;

    private Boolean isFeatured;

    private Boolean isBestseller;

    private Boolean isNewArrival;

    private Boolean isOnSale;

    @Positive(message = "Sale percentage must be positive")
    private Integer salePercentage;

    private UUID discountId;

    // Product detail fields - all optional
    @Size(max = 2000, message = "Full description must not exceed 2000 characters")
    private String fullDescription;

    @Size(max = 255, message = "Meta title must not exceed 255 characters")
    private String metaTitle;

    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    private String metaDescription;

    @Size(max = 500, message = "Meta keywords must not exceed 500 characters")
    private String metaKeywords;

    @Size(max = 1000, message = "Search keywords must not exceed 1000 characters")
    private String searchKeywords;

    @Size(max = 100, message = "Dimensions must not exceed 100 characters")
    private String dimensionsCm;

    @Positive(message = "Weight must be positive")
    private BigDecimal weightKg;

    // New variants to add (optional)
    private List<CreateProductVariantDTO> newVariants;

    // Variant images for new variants only
    private List<MultipartFile> newVariantImages;
    private List<VariantImageMetadata> newVariantImageMetadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantImageMetadata {
        @Size(max = 255, message = "Alt text must not exceed 255 characters")
        private String altText;
        private Boolean isPrimary;
        @Positive(message = "Sort order must be positive")
        private Integer sortOrder;
        @Positive(message = "Variant index must be positive")
        private Integer variantIndex; // To associate with specific variant
    }
}
