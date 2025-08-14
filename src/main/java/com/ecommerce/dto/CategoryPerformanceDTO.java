package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryPerformanceDTO {
    private Long categoryId;
    private String categoryName;
    private BigDecimal revenue;
    private Double revenuePercent;
}
