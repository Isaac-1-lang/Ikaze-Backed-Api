package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsPaymentPreviewDTO {
    
    private BigDecimal totalAmount;
    private Integer availablePoints;
    private BigDecimal pointsValue;
    private BigDecimal remainingToPay;
    private boolean canPayWithPointsOnly;
    private BigDecimal pointValue;
}
