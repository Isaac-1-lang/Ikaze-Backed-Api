package com.ecommerce.dto;

import com.ecommerce.entity.ReturnRequest.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for return delivery assignment operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDeliveryAssignmentDTO {
    
    private Long returnRequestId;
    private UUID deliveryAgentId;
    private String deliveryAgentName;
    private String deliveryAgentEmail;
    private String deliveryAgentPhone;
    private UUID assignedBy;
    private String assignedByName;
    private LocalDateTime assignedAt;
    private DeliveryStatus deliveryStatus;
    private String deliveryStatusDisplayName;
    private String deliveryNotes;
    private LocalDateTime pickupScheduledAt;
    private LocalDateTime estimatedPickupTime;
    private LocalDateTime pickupStartedAt;
    private LocalDateTime pickupCompletedAt;
    private LocalDateTime actualPickupTime;
    
    // Customer and Order Information
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String orderNumber;
    private String returnReason;
    private LocalDateTime returnSubmittedAt;
    
    // Address Information for Pickup
    private String pickupAddress;
    private String pickupCity;
    private String pickupState;
    private String pickupZipCode;
    private String pickupCountry;
    
    /**
     * Checks if the assignment can be modified
     */
    public boolean canBeModified() {
        return deliveryStatus == DeliveryStatus.ASSIGNED || 
               deliveryStatus == DeliveryStatus.PICKUP_SCHEDULED;
    }
    
    /**
     * Checks if pickup can be started
     */
    public boolean canStartPickup() {
        return deliveryStatus == DeliveryStatus.ASSIGNED || 
               deliveryStatus == DeliveryStatus.PICKUP_SCHEDULED;
    }
    
    /**
     * Checks if pickup can be completed
     */
    public boolean canCompletePickup() {
        return deliveryStatus == DeliveryStatus.PICKUP_IN_PROGRESS;
    }
}
