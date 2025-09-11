package com.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSystemDTO {

    private Long id;

    @NotNull(message = "Point value is required")
    @DecimalMin(value = "0.01", message = "Point value must be greater than 0")
    private BigDecimal pointValue;

    private Boolean isActive;
    private Boolean isSystemEnabled;
    private Boolean isReviewPointsEnabled;
    private Integer reviewPointsAmount;
    private Boolean isSignupPointsEnabled;
    private Integer signupPointsAmount;
    private Boolean isPurchasePointsEnabled;
    private Boolean isQuantityBasedEnabled;
    private Boolean isAmountBasedEnabled;
    private Boolean isPercentageBasedEnabled;
    private BigDecimal percentageRate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Valid
    private List<RewardRangeDTO> rewardRanges;
}
