package com.ecommerce.dto;

import com.ecommerce.enums.MoneyFlowType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMoneyFlowDTO {
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Type is required")
    private MoneyFlowType type;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
}
