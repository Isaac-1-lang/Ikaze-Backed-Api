package com.ecommerce.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for scheduling pickup for a return request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePickupRequestDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotNull(message = "Scheduled pickup time is required")
    @Future(message = "Scheduled pickup time must be in the future")
    private LocalDateTime scheduledPickupTime;
    
    @Future(message = "Estimated pickup time must be in the future")
    private LocalDateTime estimatedPickupTime;
    
    private String notes;
}
