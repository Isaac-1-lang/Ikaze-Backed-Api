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
    // private String status; // Removed as status is now per shop
    private List<ShopOrderDTO> shopOrders; // Grouped by shop
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private CustomerOrderAddressDTO shippingAddress;
    private CustomerOrderAddressDTO billingAddress;
    private String paymentMethod;
    private String paymentStatus;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper nested classes remain the same...
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
        private BigDecimal originalPrice;
        private BigDecimal totalPrice;
        private BigDecimal discountPercentage;
        private String discountName;
        private Boolean hasDiscount;
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
        private String country;
        private String phone;
        private Double latitude;
        private Double longitude;
    }
}
