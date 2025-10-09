package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for delivery statistics and metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatsDTO {
    
    // Overall statistics
    private long totalReturnRequests;
    private long assignedReturns;
    private long unassignedReturns;
    private long completedPickups;
    private long failedPickups;
    private long cancelledAssignments;
    
    // Current status breakdown
    private long notAssignedCount;
    private long assignedCount;
    private long pickupScheduledCount;
    private long pickupInProgressCount;
    private long pickupCompletedCount;
    private long pickupFailedCount;
    private long cancelledCount;
    
    // Performance metrics
    private double overallSuccessRate;
    private double averagePickupTime; // in hours
    private double averageAssignmentTime; // time from approval to assignment in hours
    
    // Agent statistics
    private int totalDeliveryAgents;
    private int availableDeliveryAgents;
    private int busyDeliveryAgents;
    private int unavailableDeliveryAgents;
    
    // Time-based statistics
    private long todayPickups;
    private long todayCompletions;
    private long todayFailures;
    private long weeklyPickups;
    private long monthlyPickups;
    
    /**
     * Calculates assignment rate percentage
     */
    public double getAssignmentRate() {
        if (totalReturnRequests == 0) return 0.0;
        return (assignedReturns * 100.0) / totalReturnRequests;
    }
    
    /**
     * Calculates completion rate percentage
     */
    public double getCompletionRate() {
        if (assignedReturns == 0) return 0.0;
        return (completedPickups * 100.0) / assignedReturns;
    }
    
    /**
     * Calculates failure rate percentage
     */
    public double getFailureRate() {
        if (assignedReturns == 0) return 0.0;
        return (failedPickups * 100.0) / assignedReturns;
    }
}
