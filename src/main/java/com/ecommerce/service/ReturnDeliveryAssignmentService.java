package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.ReturnRequest.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing return delivery assignments
 */
public interface ReturnDeliveryAssignmentService {
    
    /**
     * Assigns a delivery agent to a return request
     * 
     * @param request The assignment request
     * @param assignedBy The user who is making the assignment
     * @return The assignment details
     */
    ReturnDeliveryAssignmentDTO assignDeliveryAgent(AssignDeliveryAgentRequestDTO request, UUID assignedBy);
    
    /**
     * Unassigns a delivery agent from a return request
     * 
     * @param request The unassignment request
     * @return The updated assignment details
     */
    ReturnDeliveryAssignmentDTO unassignDeliveryAgent(UnassignDeliveryAgentRequestDTO request);
    
    /**
     * Schedules pickup for a return request
     * 
     * @param request The schedule pickup request
     * @return The updated assignment details
     */
    ReturnDeliveryAssignmentDTO schedulePickup(SchedulePickupRequestDTO request);
    
    /**
     * Updates pickup status (start, complete, fail)
     * 
     * @param request The status update request
     * @return The updated assignment details
     */
    ReturnDeliveryAssignmentDTO updatePickupStatus(UpdatePickupStatusRequestDTO request);
    
    /**
     * Gets assignment details for a specific return request
     * 
     * @param returnRequestId The return request ID
     * @return The assignment details or null if not assigned
     */
    ReturnDeliveryAssignmentDTO getAssignmentDetails(Long returnRequestId);
    
    /**
     * Gets all return requests assigned to a specific delivery agent
     * 
     * @param deliveryAgentId The delivery agent ID
     * @param pageable Pagination information
     * @return Page of assigned return requests
     */
    Page<ReturnDeliveryAssignmentDTO> getAssignmentsByDeliveryAgent(UUID deliveryAgentId, Pageable pageable);
    
    /**
     * Gets all return requests with a specific delivery status
     * 
     * @param status The delivery status to filter by
     * @param pageable Pagination information
     * @return Page of return requests with the specified status
     */
    Page<ReturnDeliveryAssignmentDTO> getAssignmentsByStatus(DeliveryStatus status, Pageable pageable);
    
    /**
     * Gets all return requests that can be assigned to delivery agents
     * (approved returns that are not yet assigned)
     * 
     * @return Page of assignable return requests
     */
    Page<ReturnDeliveryAssignmentDTO> getAssignableReturnRequests(Pageable pageable);
    
    /**
     * Get all available delivery agents
     * @return List of available delivery agents
     */
    List<UserDTO> getAvailableDeliveryAgents();
    
    /**
     * Search available delivery agents with pagination
     * @param search Search term for name or email
     * @param pageable Pagination parameters
     * @return Page of available delivery agents
     */
    Page<UserDTO> searchAvailableDeliveryAgents(String search, Pageable pageable);
    
    /**
     * Gets delivery agent workload (number of assigned return requests)
     * 
     * @param deliveryAgentId The delivery agent ID
     */
    DeliveryAgentWorkloadDTO getDeliveryAgentWorkload(UUID deliveryAgentId);
    
    /**
     * Gets delivery statistics for reporting
     * 
     * @return Delivery statistics
     */
    DeliveryStatsDTO getDeliveryStatistics();
    
    /**
     * Reassigns a return request from one delivery agent to another
     * 
     * @param returnRequestId The return request ID
     * @param newDeliveryAgentId The new delivery agent ID
     * @param assignedBy The user making the reassignment
     * @param reason The reason for reassignment
     * @return The updated assignment details
     */
    ReturnDeliveryAssignmentDTO reassignDeliveryAgent(Long returnRequestId, UUID newDeliveryAgentId, UUID assignedBy, String reason);
    
    /**
     * Cancels a delivery assignment
     * 
     * @param returnRequestId The return request ID
     * @param reason The reason for cancellation
     * @return The updated assignment details
     */
    ReturnDeliveryAssignmentDTO cancelAssignment(Long returnRequestId, String reason);
    
    /**
     * Gets assignment history for a return request
     * 
     * @param returnRequestId The return request ID
     * @return List of assignment history entries
     */
    List<DeliveryAssignmentHistoryDTO> getAssignmentHistory(Long returnRequestId);
}
