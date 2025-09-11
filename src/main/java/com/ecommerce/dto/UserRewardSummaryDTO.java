package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRewardSummaryDTO {

    private UUID userId;
    private String userFullName;
    private String userEmail;
    private Integer currentPoints;
    private BigDecimal currentPointsValue;
    private Integer totalPointsEarned;
    private Integer totalPointsSpent;
    private Integer totalPointsExpired;
    private BigDecimal totalValueEarned;
    private BigDecimal totalValueSpent;
    private String formattedCurrentPointsValue;
    private String formattedTotalValueEarned;
    private String formattedTotalValueSpent;

    // Reward system information
    private BigDecimal pointValue; // How much 1 point equals
    private String formattedPointValue;
}
