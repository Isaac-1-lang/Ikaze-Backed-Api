package com.ecommerce.dto;

import com.ecommerce.entity.ReturnRequest.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for delivery assignment history tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentHistoryDTO {
    
    private Long id;
    private Long returnRequestId;
    private UUID deliveryAgentId;
    private String deliveryAgentName;
    private UUID actionPerformedBy;
    private String actionPerformedByName;
    private String action; // ASSIGNED, UNASSIGNED, REASSIGNED, PICKUP_STARTED, etc.
    private DeliveryStatus previousStatus;
    private DeliveryStatus newStatus;
    private String notes;
    private LocalDateTime timestamp;
    
    // Additional context
    private String reason; // for unassignments, cancellations, failures
    private LocalDateTime scheduledTime; // for pickup scheduling
    private LocalDateTime estimatedTime; // for pickup estimates
}
