package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderDTO {
    private String orderNumber;
    private String status;
    private List<DeliveryItemDTO> items;
    private DeliveryAddressDTO deliveryAddress;
    private DeliveryCustomerDTO customer;
    private String notes;
    private LocalDateTime createdAt;
    private String estimatedDelivery;
    private String trackingNumber;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryItemDTO {
        private String productName;
        private String variantSku;
        private Integer quantity;
        private String productImage;
        private String productDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddressDTO {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryCustomerDTO {
        private String fullName;
        private String phone;
        // No email for delivery agencies - privacy protection
    }
}
