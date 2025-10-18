package com.ecommerce.dto;

import com.ecommerce.enums.MoneyFlowType;
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
public class MoneyFlowDTO {
    private Long id;
    private String description;
    private MoneyFlowType type;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private LocalDateTime createdAt;
}
