package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartProductsResponseDTO {

    private List<CartProductDTO> items;
    private BigDecimal subtotal;
    private Integer totalItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartProductDTO {

        private String itemId; // From localStorage
        private String productId;
        private Long variantId;
        private String productName;
        private String productDescription;
        private BigDecimal price;
        private BigDecimal previousPrice;
        private String productImage;
        private Integer quantity;
        private Integer availableStock;
        private BigDecimal totalPrice;
        private Double averageRating;
        private Integer reviewCount;

        // Variant specific fields
        private String variantSku;
        private List<VariantAttributeDTO> variantAttributes;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class VariantAttributeDTO {
            private String attributeTypeName;
            private String attributeValue;
        }
    }
}
