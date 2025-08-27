package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateProductDTO {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    private String sku;

    @Size(max = 50, message = "Barcode must not exceed 50 characters")
    private String barcode;

    @NotNull(message = "Base price is required")
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

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private UUID brandId;
    private UUID discountId;

    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;

    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isBestseller;
    private Boolean isNewArrival;
    private List<String> productImages;
    private String imageMetadata;
    private List<String> productVideos;
    private String videoMetadata;
    private List<CreateProductVariantDTO> variants;

    // Variant images and mapping
    private List<MultipartFile> variantImages;
    private String variantImageMapping;

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSku() {
        return sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public UUID getDiscountId() {
        return discountId;
    }

    public String getModel() {
        return model;
    }

    public String getSlug() {
        return slug;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public Boolean getIsBestseller() {
        return isBestseller;
    }

    public Boolean getIsNewArrival() {
        return isNewArrival;
    }

    public List<String> getProductImages() {
        return productImages;
    }

    public String getImageMetadata() {
        return imageMetadata;
    }

    public List<String> getProductVideos() {
        return productVideos;
    }

    public String getVideoMetadata() {
        return videoMetadata;
    }

    public List<CreateProductVariantDTO> getVariants() {
        return variants;
    }

    // Additional methods for ProductServiceImpl
    public String getFullDescription() {
        return description;
    }

    public String getMetaTitle() {
        return name;
    }

    public String getMetaDescription() {
        return description;
    }

    public String getMetaKeywords() {
        return name + " " + description;
    }

    public String getSearchKeywords() {
        return name + " " + description;
    }

    public String getDimensionsCm() {
        return "0x0x0";
    }

    public BigDecimal getWeightKg() {
        return BigDecimal.ZERO;
    }

    public Boolean getIsOnSale() {
        return false;
    }

    public Integer getSalePercentage() {
        return 0;
    }
}
