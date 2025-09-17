package com.ecommerce.dto;

import com.ecommerce.enums.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for StockBatch entity
 * Used for API responses and data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private BigDecimal costPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional computed fields
    private String productName;
    private String variantName;
    private String warehouseName;
    private Boolean isExpired;
    private Boolean isExpiringSoon;
    private Boolean isAvailable;
    private Boolean isEmpty;
    private Boolean isRecalled;

    // Helper methods for computed fields
    public Boolean getIsExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public Boolean getIsExpiringSoon() {
        if (expiryDate == null) {
            return false;
        }
        LocalDate thresholdDate = LocalDate.now().plusDays(30); // Default 30 days
        return expiryDate.isBefore(thresholdDate) && !getIsExpired();
    }

    public Boolean getIsAvailable() {
        return status == BatchStatus.ACTIVE && quantity > 0;
    }

    public Boolean getIsEmpty() {
        return quantity <= 0;
    }

    public Boolean getIsRecalled() {
        return status == BatchStatus.RECALLED;
    }
}
