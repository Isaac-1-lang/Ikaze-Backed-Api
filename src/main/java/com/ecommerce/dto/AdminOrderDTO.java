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
    private String status; // Derived from shop orders
    private String paymentStatus;
    private List<ShopOrderDTO> shopOrders;
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
    private com.ecommerce.dto.DeliveryGroupInfoDTO deliveryGroup;

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
        private BigDecimal originalPrice; // Price before discount
        private BigDecimal totalPrice;
        private BigDecimal discountPercentage;
        private String discountName;
        private Boolean hasDiscount;
        private Integer availableStock;
        private List<AdminOrderWarehouseDTO> warehouses; // Warehouses where this item will be sourced from
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOrderWarehouseDTO {
        private String warehouseId;
        private String warehouseName;
        private String warehouseLocation;
        private String warehouseAddress;
        private String warehousePhone;
        private String warehouseManager;
        private Integer quantityFromWarehouse;
        private List<AdminOrderBatchDTO> batches; // Batches from this warehouse
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOrderBatchDTO {
        private String batchId;
        private String batchNumber;
        private Integer quantityFromBatch;
        private LocalDateTime manufactureDate;
        private LocalDateTime expiryDate;
        private String batchStatus;
        private String supplierName;
        private BigDecimal costPrice;
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
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminPaymentInfoDTO {
        private String paymentMethod;
        private String paymentStatus;
        private String stripePaymentIntentId;
        private String stripeSessionId;
        private String transactionRef;
        private LocalDateTime paymentDate;
        private String receiptUrl;
        private Integer pointsUsed;
        private BigDecimal pointsValue;
        private List<AdminShopTransactionDTO> shopTransactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminShopTransactionDTO {
        private String shopName;
        private BigDecimal amount;
        private Integer pointsUsed;
        private BigDecimal pointsValue;
    }
}
