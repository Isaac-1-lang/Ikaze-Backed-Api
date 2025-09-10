package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCostDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal distanceKmCost;
    private BigDecimal weightKgCost;
    private BigDecimal baseFee;
    private BigDecimal internationalFee;
    private BigDecimal freeShippingThreshold;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
