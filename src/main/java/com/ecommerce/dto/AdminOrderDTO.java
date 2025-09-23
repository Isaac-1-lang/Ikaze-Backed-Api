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
public class AdminOrderDTO {
    private String id;
    private String userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String orderNumber;
    private String status;
    private List<AdminOrderItemDTO> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private AdminOrderAddressDTO shippingAddress;
    private AdminOrderAddressDTO billingAddress;
    private AdminPaymentInfoDTO paymentInfo;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String estimatedDelivery;
    private String trackingNumber;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOrderItemDTO {
        private String id;
        private String productId;
        private String variantId;
        private SimpleProductDTO product;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
        private Integer availableStock;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOrderAddressDTO {
        private String id;
        private String street;
        private String city;
        private String state;
        private String country;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminPaymentInfoDTO {
        private String paymentMethod;
        private String paymentStatus;
        private String stripePaymentIntentId; // Only Stripe ID, no sensitive data
        private String stripeSessionId;
        private String transactionRef;
        private LocalDateTime paymentDate;
        private String receiptUrl;
    }
}
