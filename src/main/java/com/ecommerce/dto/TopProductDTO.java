package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductDTO {
    private UUID productId;
    private String productName;
    private long totalSalesCount;
    private BigDecimal totalSalesAmount;
    private Double performancePercent;
}
