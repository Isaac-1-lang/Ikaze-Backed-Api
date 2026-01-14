package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Entity
@Table(name = "reward_system")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Shop is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    @JsonBackReference
    private Shop shop;

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

    @OneToMany(mappedBy = "rewardSystem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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

    private void logDebugToFile(String message) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("checkout_debug_logs.txt");
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logLine = timestamp + " - " + message + "\n";
            java.nio.file.Files.write(path, logLine.getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.error("Failed to write to debug file: " + e.getMessage());
        }
    }

    public Integer calculatePurchasePoints(Integer productCount, BigDecimal orderAmount) {
        if (!isSystemEnabled || !isPurchasePointsEnabled) {
            logDebugToFile("system disabled or purchase points disabled");
            logDebugToFile(isSystemEnabled.toString());
            logDebugToFile(isPurchasePointsEnabled.toString());
            return 0;
        }

        Integer totalPoints = 0;
        logDebugToFile("passed system disabled");

        if (isQuantityBasedEnabled && rewardRanges != null) {
            logDebugToFile("passed quantity based enabled");
            totalPoints += calculateQuantityBasedPoints(productCount);
        }

        if (isAmountBasedEnabled && rewardRanges != null) {
            logDebugToFile("passed amount based enabled");
            totalPoints += calculateAmountBasedPoints(orderAmount);
        }

        if (isPercentageBasedEnabled && percentageRate != null) {
            logDebugToFile("passed percentage based enabled");
            totalPoints += calculatePercentageBasedPoints(orderAmount);
        }

        logDebugToFile("total points: " + totalPoints);

        return totalPoints;
    }

    private Integer calculateQuantityBasedPoints(Integer productCount) {
        logDebugToFile("calculateQuantityBasedPoints - productCount: " + productCount);
        logDebugToFile("Total reward ranges: " + (rewardRanges != null ? rewardRanges.size() : "null"));

        if (rewardRanges != null) {
            rewardRanges.forEach(range -> {
                logDebugToFile("Range: type=" + range.getRangeType() +
                        ", min=" + range.getMinValue() +
                        ", max=" + range.getMaxValue() +
                        ", points=" + range.getPoints());
            });
        }

        int points = rewardRanges.stream()
                .filter(range -> {
                    boolean isQuantity = range.getRangeType() == RewardRange.RangeType.QUANTITY;
                    logDebugToFile("Range type QUANTITY? " + isQuantity + " (actual: " + range.getRangeType() + ")");
                    return isQuantity;
                })
                .filter(range -> {
                    boolean inRange = range.getMinValue() <= productCount
                            && (range.getMaxValue() == null || productCount <= range.getMaxValue());
                    logDebugToFile("Product count " + productCount + " in range [" + range.getMinValue() + "-"
                            + range.getMaxValue() + "]? " + inRange);
                    return inRange;
                })
                .mapToInt(RewardRange::getPoints)
                .sum();

        logDebugToFile("Quantity-based points calculated: " + points);
        return points;
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
