package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardRangeDTO {

    private Long id;
    private Long rewardSystemId;
    private String rangeType;
    private Double minValue;
    private Double maxValue;
    private Integer points;
    private String description;
}
