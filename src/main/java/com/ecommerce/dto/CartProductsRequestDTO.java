package com.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartProductsRequestDTO {

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<CartItemRequestDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemRequestDTO {

        @NotNull(message = "Product ID is required")
        private String productId;

        private Long variantId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;

        private String itemId; // For localStorage identification
    }
}
