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
    private Boolean isOnSale;
    private Integer salePercentage;

    // Product Details
    @Size(max = 2000, message = "Full description must not exceed 2000 characters")
    private String fullDescription;
    
    @Size(max = 255, message = "Meta title must not exceed 255 characters")
    private String metaTitle;
    
    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    private String metaDescription;
    
    @Size(max = 500, message = "Meta keywords must not exceed 500 characters")
    private String metaKeywords;
    
    @Size(max = 500, message = "Search keywords must not exceed 500 characters")
    private String searchKeywords;

    // Physical Dimensions and Weight
    @Positive(message = "Height must be positive")
    private BigDecimal heightCm;
    
    @Positive(message = "Width must be positive")
    private BigDecimal widthCm;
    
    @Positive(message = "Length must be positive")
    private BigDecimal lengthCm;
    
    @Positive(message = "Weight must be positive")
    private BigDecimal weightKg;

    // Product Specifications
    @Size(max = 100, message = "Material must not exceed 100 characters")
    private String material;
    
    @Size(max = 500, message = "Care instructions must not exceed 500 characters")
    private String careInstructions;
    
    @Size(max = 500, message = "Warranty info must not exceed 500 characters")
    private String warrantyInfo;
    
    @Size(max = 500, message = "Shipping info must not exceed 500 characters")
    private String shippingInfo;
    
    @Size(max = 500, message = "Return policy must not exceed 500 characters")
    private String returnPolicy;

    // Warehouse Assignment
    private List<WarehouseStockDTO> warehouseStock;

    // Media
    private List<MultipartFile> productImages;
    private List<ImageMetadata> imageMetadata;
    private List<String> productVideos;
    private String videoMetadata;
    
    // Variants
    private List<CreateProductVariantDTO> variants;

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

    public String getFullDescription() {
        return fullDescription != null ? fullDescription : description;
    }

    public String getMetaTitle() {
        return metaTitle != null ? metaTitle : name;
    }

    public String getMetaDescription() {
        return metaDescription != null ? metaDescription : description;
    }

    public String getMetaKeywords() {
        return metaKeywords != null ? metaKeywords : name + " " + description;
    }

    public String getSearchKeywords() {
        return searchKeywords != null ? searchKeywords : name + " " + description;
    }

    public String getDimensionsCm() {
        if (heightCm != null && widthCm != null && lengthCm != null) {
            return heightCm + "x" + widthCm + "x" + lengthCm;
        }
        return "0x0x0";
    }

    public BigDecimal getWeightKg() {
        return weightKg != null ? weightKg : BigDecimal.ZERO;
    }

    public Boolean getIsOnSale() {
        return isOnSale != null ? isOnSale : false;
    }

    public Integer getSalePercentage() {
        return salePercentage != null ? salePercentage : 0;
    }

    // Additional getters for new fields
    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public BigDecimal getWidthCm() {
        return widthCm;
    }

    public BigDecimal getLengthCm() {
        return lengthCm;
    }

    public String getMaterial() {
        return material;
    }

    public String getCareInstructions() {
        return careInstructions;
    }

    public String getWarrantyInfo() {
        return warrantyInfo;
    }

    public String getShippingInfo() {
        return shippingInfo;
    }

    public String getReturnPolicy() {
        return returnPolicy;
    }

    public List<WarehouseStockDTO> getWarehouseStock() {
        return warehouseStock;
    }

    // Existing getters
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

    public List<MultipartFile> getProductImages() {
        return productImages;
    }

    public List<ImageMetadata> getImageMetadata() {
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
}
