package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for comprehensive product search and filtering
 * All fields are optional, but at least one must be provided
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDTO {

    // Basic product identifiers
    private UUID productId;
    private String name;
    private String description;
    private String sku;
    private String barcode;
    private String slug;
    private String model;

    // Price filters
    private BigDecimal basePriceMin;
    private BigDecimal basePriceMax;
    private BigDecimal salePriceMin;
    private BigDecimal salePriceMax;
    private BigDecimal compareAtPriceMin;
    private BigDecimal compareAtPriceMax;

    // Stock filters
    private Integer stockQuantityMin;
    private Integer stockQuantityMax;
    private Boolean inStock;

    // Category and brand filters
    private Long categoryId;
    private List<Long> categoryIds;
    private List<String> categoryNames; // Fallback when ID mapping fails
    private Boolean includeSubcategories;
    private UUID brandId;
    private List<UUID> brandIds; // For multiple brands
    private List<String> brandNames; // Fallback when ID mapping fails

    // Discount filters
    private UUID discountId;
    private List<UUID> discountIds; // For multiple discounts
    private BigDecimal discountPercentageMin;
    private BigDecimal discountPercentageMax;
    private Boolean hasDiscount;
    private Boolean isOnSale;

    // Feature flags
    private Boolean isFeatured;
    private Boolean isBestseller;
    private Boolean isNewArrival;

    // Rating and review filters
    private Double averageRatingMin;
    private Double averageRatingMax;
    private Integer reviewCountMin;
    private Integer reviewCountMax;

    // Variant filters
    private Integer variantCountMin;
    private Integer variantCountMax;
    private List<String> variantAttributes;

    // Physical attributes
    private BigDecimal weightMin;
    private BigDecimal weightMax;
    private BigDecimal dimensionsMin;
    private BigDecimal dimensionsMax;

    // Date filters
    private LocalDateTime createdAtMin;
    private LocalDateTime createdAtMax;
    private LocalDateTime updatedAtMin;
    private LocalDateTime updatedAtMax;

    // Creator filter
    private UUID createdBy;

    // Pagination and sorting
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;

    // Shop scope
    private UUID shopId;

    // Text search
    private String searchKeyword; // For full-text search across name, description, SKU, etc.

    /**
     * Validate that at least one filter criterion is provided
     */
    public boolean hasAtLeastOneFilter() {
        return productId != null ||
                (name != null && !name.trim().isEmpty()) ||
                (description != null && !description.trim().isEmpty()) ||
                (sku != null && !sku.trim().isEmpty()) ||
                (barcode != null && !barcode.trim().isEmpty()) ||
                (slug != null && !slug.trim().isEmpty()) ||
                (model != null && !model.trim().isEmpty()) ||
                basePriceMin != null ||
                basePriceMax != null ||
                salePriceMin != null ||
                salePriceMax != null ||
                compareAtPriceMin != null ||
                compareAtPriceMax != null ||
                stockQuantityMin != null ||
                stockQuantityMax != null ||
                inStock != null ||
                categoryId != null ||
                (categoryIds != null && !categoryIds.isEmpty()) ||
                (categoryNames != null && !categoryNames.isEmpty()) ||
                includeSubcategories != null ||
                brandId != null ||
                (brandIds != null && !brandIds.isEmpty()) ||
                (brandNames != null && !brandNames.isEmpty()) ||
                discountId != null ||
                (discountIds != null && !discountIds.isEmpty()) ||
                discountPercentageMin != null ||
                discountPercentageMax != null ||
                hasDiscount != null ||
                isOnSale != null ||
                isFeatured != null ||
                isBestseller != null ||
                isNewArrival != null ||
                averageRatingMin != null ||
                averageRatingMax != null ||
                reviewCountMin != null ||
                reviewCountMax != null ||
                variantCountMin != null ||
                variantCountMax != null ||
                (variantAttributes != null && !variantAttributes.isEmpty()) ||
                weightMin != null ||
                weightMax != null ||
                dimensionsMin != null ||
                dimensionsMax != null ||
                createdAtMin != null ||
                createdAtMax != null ||
                updatedAtMin != null ||
                updatedAtMax != null ||
                createdBy != null ||
                shopId != null ||
                (searchKeyword != null && !searchKeyword.trim().isEmpty());
    }
}
