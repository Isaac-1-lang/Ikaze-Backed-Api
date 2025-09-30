package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for assigning a delivery agent to a return request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignDeliveryAgentRequestDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotNull(message = "Delivery agent ID is required")
    private UUID deliveryAgentId;
    
    private String notes;
    
    private LocalDateTime estimatedPickupTime;
    
    @Builder.Default
    private boolean schedulePickupImmediately = false;
    
    private LocalDateTime scheduledPickupTime;
}
