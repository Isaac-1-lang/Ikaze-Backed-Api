package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSystemPublicDTO {
    private Long id;
    private BigDecimal pointValue;
    private Boolean isActive;
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
    private List<RewardRangeDTO> rewardRanges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardRangeDTO {
        private Long id;
        private String rangeType;
        private Double minValue;
        private Double maxValue;
        private Integer points;
        private String description;
    }
}
