package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountDTO {
    private UUID discountId;
    private String name;
    private String description;
    private BigDecimal percentage;
    private String discountCode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
    private Integer usageLimit;
    private Integer usedCount;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private String discountType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isValid;
    private boolean canBeUsed;
}
