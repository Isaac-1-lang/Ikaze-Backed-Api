package com.ecommerce.dto;

import com.ecommerce.enums.BatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating new StockBatch entities
 * Used for API requests to create stock batches
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockBatchDTO {

    @NotNull(message = "Stock ID is required")
    private Long stockId;

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    private LocalDate manufactureDate;

    private LocalDate expiryDate;

    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Integer quantity;

    private BatchStatus status = BatchStatus.ACTIVE;

    private String supplierName;

    private String supplierBatchNumber;

    private BigDecimal costPrice;
}
