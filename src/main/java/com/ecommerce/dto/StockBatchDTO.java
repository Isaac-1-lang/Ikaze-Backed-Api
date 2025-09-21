package com.ecommerce.dto;

import com.ecommerce.enums.BatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchDTO {
    private Long id;
    private Long stockId;
    private String batchNumber;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private Integer quantity;
    private BatchStatus status;
    private String supplierName;
    private String supplierBatchNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional computed fields
    private String productName;
    private String warehouseName;
    private Long warehouseId;
    private String productId;
    private String variantId;
    private String variantName;
    private Boolean isExpired;
    private Boolean isExpiringSoon;
    private Boolean isEmpty;
    private Boolean isRecalled;
    private Boolean isAvailable;
}