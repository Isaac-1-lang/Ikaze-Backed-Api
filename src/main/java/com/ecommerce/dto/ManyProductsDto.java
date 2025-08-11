package com.ecommerce.dto;

import com.ecommerce.entity.Brand;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Discount;
import com.ecommerce.entity.ProductImage;
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
    private Category category;
    private Brand brand;
    private Boolean isBestSeller;
    private Boolean isFeatured;
    private Discount discountInfo;
    private ProductImage primaryImage;
}
