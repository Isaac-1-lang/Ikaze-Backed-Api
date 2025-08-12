package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToWishlistDTO {

    @NotNull(message = "Product variant ID is required")
    private Long variantId;

    private String notes;

    private Integer priority = 0;
}
