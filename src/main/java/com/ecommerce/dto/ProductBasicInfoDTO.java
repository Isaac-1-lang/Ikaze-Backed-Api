package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBasicInfoDTO {
    private UUID productId;
    private String productName;
    private String shortDescription;
    private String description;
    private String sku;
    private String barcode;
    private String model;
    private String slug;
    private String material;
    private String warrantyInfo;
    private String careInstructions;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private Long categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String brandLogoUrl;
    private boolean active;
    private boolean featured;
    private boolean bestseller;
    private boolean newArrival;
    private boolean onSale;
    private Integer salePercentage;
}
