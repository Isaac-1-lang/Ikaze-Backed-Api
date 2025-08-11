package com.ecommerce.dto;

import jakarta.validation.Valid;
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
    private String name;

    private String description;

    private String barcode;

    private BigDecimal basePrice;

    private BigDecimal salePrice;

    private BigDecimal costPrice;

    private Integer stockQuantity;

    private Integer lowStockThreshold;

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

    // Product detail fields - all optional
    private String fullDescription;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String searchKeywords;
    private String dimensionsCm;
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
        private String altText;
        private Boolean isPrimary;
        private Integer sortOrder;
        private Integer variantIndex; // To associate with specific variant
    }
}
