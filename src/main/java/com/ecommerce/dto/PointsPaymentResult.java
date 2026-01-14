package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsPaymentResult {

    private boolean success;
    private String message;
    private Long orderId;
    private String orderNumber;
    private Integer pointsUsed;
    private BigDecimal pointsValue;
    private BigDecimal remainingAmount;
    private String stripeSessionId; // Contains the complete Stripe checkout URL for redirection
    private boolean hybridPayment;

    // Multi-vendor breakdown
    private List<ShopPointsDeduction> shopPointsDeductions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShopPointsDeduction {
        private UUID shopId;
        private String shopName;
        private Integer pointsUsed;
        private BigDecimal pointsValue;
        private BigDecimal shopOrderAmount;
        private BigDecimal remainingForShop;
    }

    // Backward compatibility constructor
    public PointsPaymentResult(boolean success, String message, Long orderId, String orderNumber,
            Integer pointsUsed, BigDecimal pointsValue, BigDecimal remainingAmount,
            String stripeSessionId, boolean hybridPayment) {
        this.success = success;
        this.message = message;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.pointsUsed = pointsUsed;
        this.pointsValue = pointsValue;
        this.remainingAmount = remainingAmount;
        this.stripeSessionId = stripeSessionId;
        this.hybridPayment = hybridPayment;
        this.shopPointsDeductions = null;
    }
}
