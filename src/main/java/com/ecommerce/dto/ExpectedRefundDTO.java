package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for expected refund breakdown for a return request
 * Shows the expected refund amounts based on payment method
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedRefundDTO {
    
    /**
     * Payment method used for the original order
     * (CREDIT_CARD, DEBIT_CARD, POINTS, HYBRID)
     */
    private String paymentMethod;
    
    /**
     * Total monetary refund amount (for card payments)
     */
    private BigDecimal monetaryRefund;
    
    /**
     * Points to be refunded (for POINTS or HYBRID payments)
     */
    private Integer pointsRefund;
    
    /**
     * Monetary value of the points being refunded
     */
    private BigDecimal pointsRefundValue;
    
    /**
     * Total refund value (monetary + points value combined)
     */
    private BigDecimal totalRefundValue;
    
    /**
     * Whether this is a full order return
     */
    private boolean isFullReturn;
    
    /**
     * Breakdown of refund components
     */
    private BigDecimal itemsRefund;
    private BigDecimal shippingRefund;
    
    /**
     * Human-readable refund description
     */
    private String refundDescription;
}
