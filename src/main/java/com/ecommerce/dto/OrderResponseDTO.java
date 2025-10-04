package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String userId;
    private String orderNumber;
    private String pickupToken;
    private Boolean pickupTokenUsed;
    private String status;
    private List<OrderItem> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private ShippingAddress shippingAddress;
    private OrderAddressDTO billingAddress;
    private CustomerInfo customerInfo;
    private String paymentMethod;
    private String paymentStatus;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String estimatedDelivery;
    private String trackingNumber;
    private OrderTransactionDTO transaction;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        private String street;
        private String city;
        private String state;
        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long id;
        private int quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
        private Product product;
        private Variant variant;
        private boolean returnEligible;
        private int maxReturnDays;
        private int daysRemainingForReturn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private UUID id;
        private String name;
        private List<String> images;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variant {
        private Long id;
        private String name;
        private List<String> images;
    }
}
