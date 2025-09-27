package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
