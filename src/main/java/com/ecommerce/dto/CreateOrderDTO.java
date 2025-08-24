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
    
    @NotEmpty(message = "Order items cannot be empty")
    private List<CreateOrderItemDTO> items;
    
    @NotNull(message = "Shipping address is required")
    private CreateOrderAddressDTO shippingAddress;
    
    private CreateOrderAddressDTO billingAddress; // Optional
    
    @NotNull(message = "Payment method is required")
    @Size(min = 1, message = "Payment method cannot be empty")
    private String paymentMethod;
    
    private String notes;
    
    // Nested DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderItemDTO {
        @NotNull(message = "Product ID is required")
        private String productId; // Frontend sends product ID, not variant ID
        
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
        
        @NotNull(message = "Zip code is required")
        private String zipCode;
        
        @NotNull(message = "Country is required")
        private String country;
        
        @NotNull(message = "Phone is required")
        private String phone;
    }
}
