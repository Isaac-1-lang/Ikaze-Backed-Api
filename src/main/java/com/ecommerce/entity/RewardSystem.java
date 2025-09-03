package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reward_system")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Point value is required")
    @DecimalMin(value = "0.01", message = "Point value must be greater than 0")
    @Column(name = "point_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal pointValue;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "is_system_enabled", nullable = false)
    private Boolean isSystemEnabled = true;

    @Column(name = "is_review_points_enabled", nullable = false)
    private Boolean isReviewPointsEnabled = false;

    @Column(name = "review_points_amount")
    private Integer reviewPointsAmount = 0;

    @Column(name = "is_signup_points_enabled", nullable = false)
    private Boolean isSignupPointsEnabled = false;

    @Column(name = "signup_points_amount")
    private Integer signupPointsAmount = 0;

    @Column(name = "is_purchase_points_enabled", nullable = false)
    private Boolean isPurchasePointsEnabled = false;

    @Column(name = "is_quantity_based_enabled", nullable = false)
    private Boolean isQuantityBasedEnabled = false;

    @Column(name = "is_amount_based_enabled", nullable = false)
    private Boolean isAmountBasedEnabled = false;

    @Column(name = "is_percentage_based_enabled", nullable = false)
    private Boolean isPercentageBasedEnabled = false;

    @Column(name = "percentage_rate", precision = 5, scale = 2)
    private BigDecimal percentageRate;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "rewardSystem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RewardRange> rewardRanges;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer calculateReviewPoints() {
        if (!isSystemEnabled || !isReviewPointsEnabled || reviewPointsAmount == null) {
            return 0;
        }
        return reviewPointsAmount;
    }

    public Integer calculateSignupPoints() {
        if (!isSystemEnabled || !isSignupPointsEnabled || signupPointsAmount == null) {
            return 0;
        }
        return signupPointsAmount;
    }

    public Integer calculatePurchasePoints(Integer productCount, BigDecimal orderAmount) {
        if (!isSystemEnabled || !isPurchasePointsEnabled) {
            return 0;
        }

        Integer totalPoints = 0;

        if (isQuantityBasedEnabled && rewardRanges != null) {
            totalPoints += calculateQuantityBasedPoints(productCount);
        }

        if (isAmountBasedEnabled && rewardRanges != null) {
            totalPoints += calculateAmountBasedPoints(orderAmount);
        }

        if (isPercentageBasedEnabled && percentageRate != null) {
            totalPoints += calculatePercentageBasedPoints(orderAmount);
        }

        return totalPoints;
    }

    private Integer calculateQuantityBasedPoints(Integer productCount) {
        return rewardRanges.stream()
                .filter(range -> range.getRangeType() == RewardRange.RangeType.QUANTITY)
                .filter(range -> range.getMinValue() <= productCount
                        && (range.getMaxValue() == null || productCount <= range.getMaxValue()))
                .mapToInt(RewardRange::getPoints)
                .sum();
    }

    private Integer calculateAmountBasedPoints(BigDecimal orderAmount) {
        return rewardRanges.stream()
                .filter(range -> range.getRangeType() == RewardRange.RangeType.AMOUNT)
                .filter(range -> range.getMinValue() <= orderAmount.doubleValue()
                        && (range.getMaxValue() == null || orderAmount.doubleValue() <= range.getMaxValue()))
                .mapToInt(RewardRange::getPoints)
                .sum();
    }

    private Integer calculatePercentageBasedPoints(BigDecimal orderAmount) {
        if (percentageRate == null || percentageRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return orderAmount.multiply(percentageRate).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    public BigDecimal calculatePointsValue(Integer points) {
        if (pointValue == null || points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        return pointValue.multiply(BigDecimal.valueOf(points));
    }
}
