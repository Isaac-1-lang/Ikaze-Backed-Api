package com.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateProductDTO {
    private String name;
    private String description;
    private String sku;
    private String barcode;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Long categoryId;
    private UUID brandId;
    private UUID discountId;
    private String model;
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
