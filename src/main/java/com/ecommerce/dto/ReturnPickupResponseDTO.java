package com.ecommerce.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for return pickup response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnPickupResponseDTO {

    private Long returnRequestId;
    private String message;
    private LocalDateTime pickupCompletedAt;
    private List<ReturnItemProcessingResult> itemResults;

    /**
     * DTO for individual item processing result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemProcessingResult {
        
        private Long returnItemId;
        private String productName;
        private String variantName;
        private Integer quantityProcessed;
        private ReturnPickupRequestDTO.ReturnItemPickupStatus status;
        private boolean restockedSuccessfully;
        private String warehouseName;
        private String batchNumber;
        private String message;
    }
}
