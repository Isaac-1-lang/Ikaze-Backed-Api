package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CheckoutRequest {
    @NotEmpty(message = "Items list cannot be empty")
    private List<CartItemDTO> items; // variantId, quantity, price, etc.

    private AddressDto shippingAddress; // or flat fields

    private String currency = "usd";

    // For authenticated users
    private UUID userId;

    @NotBlank
    private String platform; // "web" or "mobile"
}
