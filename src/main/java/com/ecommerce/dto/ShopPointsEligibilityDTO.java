package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopPointsEligibilityDTO {
    private UUID shopId;
    private String shopName;
    private boolean isRewardingEnabled;
    private Integer currentPointsBalance;
    private BigDecimal currentPointsValue;
    private Integer potentialEarnedPoints;
    private BigDecimal totalAmount;
    private boolean canPayWithPoints;
    private BigDecimal maxPointsPayableAmount;
    private String message;
}
