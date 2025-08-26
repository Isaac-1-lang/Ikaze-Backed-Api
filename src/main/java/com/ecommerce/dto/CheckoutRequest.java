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

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String phoneNumber;

    private AddressDto shippingAddress; // or flat fields

    private String currency = "usd";

    // For authenticated users
    private UUID userId;
}
