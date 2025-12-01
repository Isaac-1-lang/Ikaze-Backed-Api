package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProductVariantDTO {
    private Long variantId;
    private String variantSku;
    private String variantName;
    private String variantBarcode;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private Boolean isActive;
    private Boolean isInStock;
    private Boolean isLowStock;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Discount information
    private DiscountDTO discount;
    private BigDecimal discountedPrice;
    private Boolean hasActiveDiscount;

    // Variant media
    private List<VariantImageDTO> images;

    // Variant attributes
    private List<VariantAttributeDTO> attributes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantImageDTO {
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
    public static class VariantAttributeDTO {
        private Long attributeValueId;
        private String attributeValue;
        private Long attributeTypeId;
        private String attributeType;
    }
}
