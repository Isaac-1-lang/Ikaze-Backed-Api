package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseAssignmentDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;
    
    private Long batchId; // Optional - if reassigning to specific batch
    private String batchStatus; // ACTIVE, EXPIRED, RECALLED, DAMAGED
    private String assignmentReason;
    private boolean shouldRestock;
}
