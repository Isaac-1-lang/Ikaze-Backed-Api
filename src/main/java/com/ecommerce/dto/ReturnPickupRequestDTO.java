package com.ecommerce.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO for return pickup request from delivery agent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnPickupRequestDTO {

    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;

    @NotEmpty(message = "Return items list cannot be empty")
    private List<ReturnItemPickupDTO> returnItems;

    /**
     * DTO for individual return item pickup details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemPickupDTO {
        
        @NotNull(message = "Return item ID is required")
        private Long returnItemId;
        
        @NotNull(message = "Pickup status is required")
        private ReturnItemPickupStatus pickupStatus;
        
        private String notes; // Optional notes from delivery agent
    }

    /**
     * Enum for return item pickup status
     */
    public enum ReturnItemPickupStatus {
        UNDAMAGED,  // Item is in perfect condition and can be restocked
        DAMAGED,    // Item has visible damage but was received
        MISSING,    // Item was not provided by customer
        DEFECTIVE   // Item has functional issues or defects
    }
}
