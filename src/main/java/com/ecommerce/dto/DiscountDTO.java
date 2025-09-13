package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonProperty("isActive")
    private boolean active;
    private Integer usageLimit;
    private Integer usedCount;
    private String discountType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonProperty("isValid")
    private boolean valid;
    @JsonIgnore
    private boolean canBeUsed;
}
