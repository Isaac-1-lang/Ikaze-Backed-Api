package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemResetResponse {
    
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    private SystemResetStats stats;
    private List<DeletionError> errors;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemResetStats {
        private long productsDeleted;
        private long discountsDeleted;
        private long ordersDeleted;
        private long rewardSystemsDeleted;
        private long shippingCostsDeleted;
        private long moneyFlowsDeleted;
        private long categoriesDeleted;
        private long brandsDeleted;
        private long warehousesDeleted;
        private long totalDeleted;
        private long executionTimeMs;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeletionError {
        private String entityType;
        private String errorMessage;
        private String details;
    }
    
    public void addError(String entityType, String errorMessage, String details) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(DeletionError.builder()
                .entityType(entityType)
                .errorMessage(errorMessage)
                .details(details)
                .build());
    }
}
