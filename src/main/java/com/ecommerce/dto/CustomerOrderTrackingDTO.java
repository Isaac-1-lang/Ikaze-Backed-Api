package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced DTO for customer order tracking with shop-grouped products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderTrackingDTO {

    // Overall order information
    private Long orderId;
    private String orderCode;
    private LocalDateTime orderDate;
    private String overallStatus;

    // Customer information
    private CustomerInfo customerInfo;

    // Shipping address
    private AddressDTO shippingAddress;

    // Billing address
    private AddressDTO billingAddress;

    // Payment information
    private PaymentInfo paymentInfo;

    // Shop-grouped orders
    private List<ShopOrderGroup> shopOrders;

    // Order totals
    private BigDecimal subtotal;
    private BigDecimal totalShipping;
    private BigDecimal totalDiscount;
    private BigDecimal tax;
    private BigDecimal grandTotal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDTO {
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
    public static class PaymentInfo {
        private String paymentMethod;
        private String paymentStatus;
        private LocalDateTime paymentDate;
        private String transactionRef;
        private Integer pointsUsed;
        private BigDecimal pointsValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopOrderGroup {
        private Long shopOrderId;
        private String shopOrderCode;
        private String shopId;
        private String shopName;
        private String shopLogo;
        private String shopSlug;

        // Shop order status and tracking
        private String status;
        private List<StatusTimeline> timeline;

        // Products in this shop order
        private List<OrderItemDTO> items;

        // Shop order totals
        private BigDecimal subtotal;
        private BigDecimal shippingCost;
        private BigDecimal discountAmount;
        private BigDecimal total;

        // Delivery information
        private DeliveryInfo deliveryInfo;

        // Return information
        private List<ReturnRequest> returnRequests;

        // Delivery notes
        private DeliveryNoteDTO deliveryNote;

        // Tracking token
        private String trackingToken;
        private Boolean pickupTokenUsed;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Long itemId;
        private String productId;
        private String productName;
        private String productDescription;
        private List<String> productImages;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private BigDecimal totalPrice;
        private Integer discountPercentage;
        private String discountName;
        private Boolean hasDiscount;

        // Return fields
        private Boolean returnEligible;
        private Integer maxReturnDays;
        private Integer daysRemainingForReturn;
        private ReturnItemInfo returnInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemInfo {
        private Boolean hasReturnRequest;
        private Integer totalReturnedQuantity;
        private Integer remainingQuantity;
        private List<ReturnRequestInfo> returnRequests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnRequestInfo {
        private Long id;
        private String returnCode;
        private String status;
        private String reason;
        private LocalDateTime submittedAt;
        private LocalDateTime decisionAt;
        private String decisionNotes;
        private Boolean canBeAppealed;
        private ReturnAppealInfo appeal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnAppealInfo {
        private Long id;
        private String status;
        private String reason;
        private String description;
        private LocalDateTime submittedAt;
        private LocalDateTime decisionAt;
        private String decisionNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusTimeline {
        private String status;
        private String statusLabel;
        private String description;
        private LocalDateTime timestamp;
        private Boolean isCompleted;
        private Boolean isCurrent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryInfo {
        private String deliveryGroupName;
        private String delivererName;
        private String delivererPhone;
        private LocalDateTime scheduledAt;
        private LocalDateTime deliveryStartedAt;
        private LocalDateTime deliveredAt;
        private Boolean hasDeliveryStarted;
        private String pickupToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnRequest {
        private Long returnId;
        private String returnCode;
        private String reason;
        private String status;
        private LocalDateTime requestedAt;
        private LocalDateTime processedAt;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryNoteDTO {
        private Long noteId;
        private String note;
        private LocalDateTime createdAt;
    }
}
