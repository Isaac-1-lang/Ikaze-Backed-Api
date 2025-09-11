package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardRangeDTO {

    private Long id;
    private Long rewardSystemId;

    @NotNull(message = "Range type is required")
    private String rangeType;

    @NotNull(message = "Minimum value is required")
    private Double minValue;

    private Double maxValue;

    @NotNull(message = "Points are required")
    private Integer points;

    private String description;
}
