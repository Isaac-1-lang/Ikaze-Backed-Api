package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWishlistProductDTO {

    @NotNull(message = "Wishlist product ID is required")
    private Long wishlistProductId;

    private String notes;

    private Integer priority;
}
