package com.ecommerce.dto;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequest {
    private List<CartDTO> items; // productVariantId, name, unitPrice, quantity
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private AddressDto shippingAddress; // or flat fields
    private String currency = "usd";
}
