package com.ecommerce.dto;

import com.ecommerce.entity.ReturnRequest.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating pickup status (start, complete, fail)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePickupStatusRequestDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotNull(message = "New delivery status is required")
    private DeliveryStatus newStatus;
    
    private String notes;
    
    /**
     * Validates that the new status is a valid pickup status
     */
    public boolean isValidPickupStatus() {
        return newStatus == DeliveryStatus.PICKUP_IN_PROGRESS ||
               newStatus == DeliveryStatus.PICKUP_COMPLETED ||
               newStatus == DeliveryStatus.PICKUP_FAILED;
    }
}
