package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockBatchRequest {

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    private LocalDate manufactureDate;

    private LocalDate expiryDate;

    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Integer quantity;

    private String supplierName;

    private String supplierBatchNumber;

}