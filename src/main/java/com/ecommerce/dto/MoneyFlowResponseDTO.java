package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyFlowResponseDTO {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String granularity;      // "minute", "hour", "day", "week", "month", "year"
    private List<MoneyFlowAggregationDTO> aggregations;
}
