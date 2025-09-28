package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_points")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Points amount is required")
    @Column(nullable = false)
    private Integer points; // Positive for earned, negative for spent (existing schema)

    @NotNull(message = "Points type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "points_type", nullable = false)
    private PointsType pointsType; // Use existing enum

    @Column(name = "description")
    private String description;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "points_value", precision = 10, scale = 2)
    private BigDecimal pointsValue;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PointsType {
        EARNED_PURCHASE,
        EARNED_REVIEW,
        EARNED_SIGNUP,
        EARNED_REFUND,
        SPENT_PURCHASE,
        ADJUSTMENT
    }

    // Removed PointsCategory enum - not needed since PointsType provides categorization

    public boolean isEarned() {
        return points > 0; // Positive points = earned
    }

    public boolean isSpent() {
        return points < 0; // Negative points = spent
    }
}
