package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToWishlistDTO {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    private String notes;

    private Integer priority = 0;
}
