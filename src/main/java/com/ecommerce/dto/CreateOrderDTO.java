package com.ecommerce.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {
    @NotNull(message = "User ID is required")
    private String userId;

    @NotEmpty(message = "Order items cannot be empty")
    private List<CreateOrderItemDTO> items;

    @NotNull(message = "Shipping address is required")
    private CreateOrderAddressDTO shippingAddress;

    private CreateOrderAddressDTO billingAddress; // Optional

    @NotNull(message = "Payment method is required")
    @Size(min = 1, message = "Payment method cannot be empty")
    private String paymentMethod;

    private String notes;

    // Stripe payment fields
    private String stripePaymentIntentId;

    private String stripeSessionId;

    // Nested DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderItemDTO {
        @NotNull(message = "Product ID is required")
        private String productId; // For reference, not used for stock/price

        @NotNull(message = "Variant ID is required")
        private String variantId; // New: must be provided by frontend

        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderAddressDTO {
        @NotNull(message = "Street is required")
        private String street;

        @NotNull(message = "City is required")
        private String city;

        @NotNull(message = "State is required")
        private String state;
        
        @NotNull(message = "Country is required")
        private String country;

        @NotNull(message = "Phone is required")
        private String phone;
    }
}
