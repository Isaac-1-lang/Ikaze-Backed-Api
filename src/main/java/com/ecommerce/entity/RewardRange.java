package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reward_ranges")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_system_id", nullable = false)
    private RewardSystem rewardSystem;

    @NotNull(message = "Range type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "range_type", nullable = false)
    private RangeType rangeType;

    @NotNull(message = "Minimum value is required")
    @Column(name = "min_value", nullable = false)
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @NotNull(message = "Points are required")
    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "description")
    private String description;

    public enum RangeType {
        QUANTITY,
        AMOUNT
    }
}
