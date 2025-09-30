package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for delivery agent workload information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentWorkloadDTO {
    
    private UUID deliveryAgentId;
    private String deliveryAgentName;
    private String deliveryAgentEmail;
    
    // Current workload
    private int totalAssignedReturns;
    private int pendingPickups;
    private int scheduledPickups;
    private int inProgressPickups;
    private int completedPickupsToday;
    private int failedPickupsToday;
    
    // Performance metrics
    private double averagePickupTime; // in hours
    private double successRate; // percentage
    private int totalCompletedReturns;
    private int totalFailedReturns;
    
    // Availability status
    private boolean isAvailable;
    private String unavailabilityReason;
    
    /**
     * Calculates the workload score (higher means more busy)
     */
    public double getWorkloadScore() {
        return (totalAssignedReturns * 1.0) + 
               (pendingPickups * 2.0) + 
               (scheduledPickups * 1.5) + 
               (inProgressPickups * 3.0);
    }
    
    /**
     * Checks if the agent is overloaded
     */
    public boolean isOverloaded() {
        return getWorkloadScore() > 10.0 || !isAvailable;
    }
}
