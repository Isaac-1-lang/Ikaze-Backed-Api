package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderDTO {
    private String id;
    private String orderNumber;
    private String status;
    private List<CustomerOrderItemDTO> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private CustomerOrderAddressDTO shippingAddress;
    private CustomerOrderAddressDTO billingAddress;
    private String paymentMethod; // Only method name, no sensitive data
    private String paymentStatus;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String estimatedDelivery;
    private String trackingNumber;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerOrderItemDTO {
        private String id;
        private String productId;
        private SimpleProductDTO product;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerOrderAddressDTO {
        private String id;
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String phone;
    }
}
