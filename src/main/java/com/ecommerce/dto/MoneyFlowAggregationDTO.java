package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyFlowAggregationDTO {
    private String period;           // e.g., "2025-10-15 19:35", "Oct 10", "Week 35", "May 2025", "2024"
    private BigDecimal totalInflow;
    private BigDecimal totalOutflow;
    private BigDecimal netBalance;   // totalInflow - totalOutflow
    private List<MoneyFlowDTO> transactions; // Detailed transactions (only for minute/hour granularity)
}
