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
public class ShopOrderDTO {
    private String shopOrderId;
    private String shopOrderCode;
    private String shopId;
    private String shopName;
    private String shopLogo;
    private String status;
    private List<CustomerOrderDTO.CustomerOrderItemDTO> items;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String pickupToken;
    private Boolean pickupTokenUsed;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private DeliveryGroupInfoDTO deliveryGroup;

    // Points-related fields for this shop order
    private Integer pointsUsed;
    private BigDecimal pointsValue;
    private String paymentMethod; // POINTS, HYBRID, CARD, etc.
}
