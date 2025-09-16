package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequest {
    @NotEmpty(message = "Items list cannot be empty")
    private List<CartItemDTO> items; 

    private AddressDto shippingAddress;

    private String currency = "usd";
    @NotBlank
    private String platform;
}
