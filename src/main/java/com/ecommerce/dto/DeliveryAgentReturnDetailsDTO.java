package com.ecommerce.dto;

import com.ecommerce.entity.ReturnRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive DTO for delivery agent return request details page
 * Contains all information needed for pickup including location, items, and customer details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentReturnDetailsDTO {
    
    // Return request basic info
    private Long id;
    private String reason;
    private ReturnRequest.ReturnStatus status;
    private ReturnRequest.DeliveryStatus deliveryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime decisionAt;
    private String decisionNotes;
    
    // Order information
    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private BigDecimal orderTotal;
    
    // Customer information
    private CustomerInfoDTO customer;
    
    // Pickup address with coordinates
    private PickupAddressDTO pickupAddress;
    
    // Return items with product details
    private List<ReturnItemDetailsDTO> returnItems;
    
    // Delivery agent information
    private UUID deliveryAgentId;
    private String deliveryAgentName;
    private LocalDateTime assignedAt;
    
    // Pickup tracking
    private LocalDateTime pickupScheduledAt;
    private LocalDateTime pickupStartedAt;
    private LocalDateTime pickupCompletedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfoDTO {
        private String name;
        private String email;
        private String phone;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PickupAddressDTO {
        private String street;
        private String country;
        private String regions;
        private String roadName;
        private Double latitude;
        private Double longitude;
        private String fullAddress;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDetailsDTO {
        private Long id;
        private Integer returnQuantity;
        private String itemReason;
        private boolean returnable;
        
        // Product information
        private ProductInfoDTO product;
        
        // Variant information (if applicable)
        private VariantInfoDTO variant;
        
        // Order item reference
        private Integer orderQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfoDTO {
        private UUID productId;
        private String name;
        private String description;
        private String brand;
        private String category;
        private List<String> imageUrls;
        private boolean returnable;
        private Integer returnWindowDays;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantInfoDTO {
        private Long variantId;
        private String variantName;
        private String color;
        private String size;
        private String material;
        private List<String> variantImageUrls;
        private BigDecimal variantPrice;
    }
}
