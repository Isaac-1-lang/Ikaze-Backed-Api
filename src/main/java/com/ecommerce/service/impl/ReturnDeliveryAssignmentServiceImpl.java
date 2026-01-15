package com.ecommerce.service.impl;

import com.ecommerce.dto.*;
import com.ecommerce.entity.ReturnRequest;
import com.ecommerce.entity.ReturnRequest.DeliveryStatus;
import com.ecommerce.entity.User;
import com.ecommerce.Enum.UserRole;
import com.ecommerce.repository.ReturnRequestRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ReturnDeliveryAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ReturnDeliveryAssignmentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReturnDeliveryAssignmentServiceImpl implements ReturnDeliveryAssignmentService {

    private final ReturnRequestRepository returnRequestRepository;
    private final UserRepository userRepository;

    @Override
    public ReturnDeliveryAssignmentDTO assignDeliveryAgent(AssignDeliveryAgentRequestDTO request, UUID assignedBy) {
        log.info("Assigning delivery agent {} to return request {}", request.getDeliveryAgentId(),
                request.getReturnRequestId());

        // Validate return request
        ReturnRequest returnRequest = returnRequestRepository.findById(request.getReturnRequestId())
                .orElseThrow(() -> new RuntimeException("Return request not found: " + request.getReturnRequestId()));

        if (!returnRequest.canBeAssignedToDeliveryAgent()) {
            throw new IllegalStateException("Return request cannot be assigned to delivery agent. Status: " +
                    returnRequest.getStatus() + ", Delivery Status: " + returnRequest.getDeliveryStatus());
        }

        // Validate delivery agent
        User deliveryAgent = userRepository.findById(request.getDeliveryAgentId())
                .orElseThrow(() -> new RuntimeException("Delivery agent not found: " + request.getDeliveryAgentId()));

        if (deliveryAgent.getRole() != UserRole.DELIVERY_AGENT) {
            throw new IllegalArgumentException("User is not a delivery agent: " + request.getDeliveryAgentId());
        }

        // Validate assigner exists
        userRepository.findById(assignedBy)
                .orElseThrow(() -> new RuntimeException("Assigner not found: " + assignedBy));

        // Assign delivery agent
        returnRequest.assignDeliveryAgent(request.getDeliveryAgentId(), assignedBy, request.getNotes());

        // Schedule pickup if requested
        if (request.isSchedulePickupImmediately() && request.getScheduledPickupTime() != null) {
            returnRequest.schedulePickup(request.getScheduledPickupTime(), request.getEstimatedPickupTime());
        }

        // Save the updated return request
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Successfully assigned delivery agent {} to return request {}",
                request.getDeliveryAgentId(), request.getReturnRequestId());

        return convertToAssignmentDTO(returnRequest);
    }

    @Override
    public ReturnDeliveryAssignmentDTO unassignDeliveryAgent(UnassignDeliveryAgentRequestDTO request) {
        log.info("Unassigning delivery agent from return request {}", request.getReturnRequestId());

        ReturnRequest returnRequest = returnRequestRepository.findById(request.getReturnRequestId())
                .orElseThrow(() -> new RuntimeException("Return request not found: " + request.getReturnRequestId()));

        if (!returnRequest.isAssignedToDeliveryAgent()) {
            throw new IllegalStateException("Return request is not assigned to any delivery agent");
        }

        if (returnRequest.isPickupInProgressOrCompleted()) {
            throw new IllegalStateException("Cannot unassign delivery agent when pickup is in progress or completed");
        }

        // Unassign delivery agent
        returnRequest.unassignDeliveryAgent(request.getReason());

        // Save the updated return request
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Successfully unassigned delivery agent from return request {}", request.getReturnRequestId());

        return convertToAssignmentDTO(returnRequest);
    }

    @Override
    public ReturnDeliveryAssignmentDTO schedulePickup(SchedulePickupRequestDTO request) {
        log.info("Scheduling pickup for return request {}", request.getReturnRequestId());

        ReturnRequest returnRequest = returnRequestRepository.findById(request.getReturnRequestId())
                .orElseThrow(() -> new RuntimeException("Return request not found: " + request.getReturnRequestId()));

        if (!returnRequest.isAssignedToDeliveryAgent()) {
            throw new IllegalStateException(
                    "Return request must be assigned to a delivery agent before scheduling pickup");
        }

        // Schedule pickup
        returnRequest.schedulePickup(request.getScheduledPickupTime(), request.getEstimatedPickupTime());

        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            returnRequest.setDeliveryNotes(request.getNotes());
        }

        // Save the updated return request
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Successfully scheduled pickup for return request {} at {}",
                request.getReturnRequestId(), request.getScheduledPickupTime());

        return convertToAssignmentDTO(returnRequest);
    }

    @Override
    public ReturnDeliveryAssignmentDTO updatePickupStatus(UpdatePickupStatusRequestDTO request) {
        log.info("Updating pickup status for return request {} to {}",
                request.getReturnRequestId(), request.getNewStatus());

        if (!request.isValidPickupStatus()) {
            throw new IllegalArgumentException("Invalid pickup status: " + request.getNewStatus());
        }

        ReturnRequest returnRequest = returnRequestRepository.findById(request.getReturnRequestId())
                .orElseThrow(() -> new RuntimeException("Return request not found: " + request.getReturnRequestId()));

        // Update status based on the new status
        switch (request.getNewStatus()) {
            case PICKUP_IN_PROGRESS -> returnRequest.startPickup();
            case PICKUP_COMPLETED -> returnRequest.completePickup(request.getNotes());
            case PICKUP_FAILED -> returnRequest.failPickup(request.getNotes());
            default -> throw new IllegalArgumentException("Unsupported status update: " + request.getNewStatus());
        }

        // Save the updated return request
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Successfully updated pickup status for return request {} to {}",
                request.getReturnRequestId(), request.getNewStatus());

        return convertToAssignmentDTO(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnDeliveryAssignmentDTO getAssignmentDetails(Long returnRequestId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnRequestId));

        return convertToAssignmentDTO(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnDeliveryAssignmentDTO> getAssignmentsByDeliveryAgent(UUID deliveryAgentId, Pageable pageable) {
        Page<ReturnRequest> returnRequests = returnRequestRepository.findByDeliveryAgentId(deliveryAgentId, pageable);

        List<ReturnDeliveryAssignmentDTO> assignments = returnRequests.getContent().stream()
                .map(this::convertToAssignmentDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(assignments, pageable, returnRequests.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnDeliveryAssignmentDTO> getAssignmentsByStatus(DeliveryStatus status, Pageable pageable) {
        Page<ReturnRequest> returnRequests = returnRequestRepository.findByDeliveryStatus(status, pageable);

        List<ReturnDeliveryAssignmentDTO> assignments = returnRequests.getContent().stream()
                .map(this::convertToAssignmentDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(assignments, pageable, returnRequests.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnDeliveryAssignmentDTO> getAssignableReturnRequests(Pageable pageable) {
        Page<ReturnRequest> returnRequests = returnRequestRepository.findAssignableReturnRequests(pageable);

        List<ReturnDeliveryAssignmentDTO> assignments = returnRequests.getContent().stream()
                .map(this::convertToAssignmentDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(assignments, pageable, returnRequests.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAvailableDeliveryAgents() {
        List<User> deliveryAgents = userRepository.findByRole(UserRole.DELIVERY_AGENT);

        return deliveryAgents.stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> searchAvailableDeliveryAgents(String search, Pageable pageable) {
        Page<User> deliveryAgents;

        if (search != null && !search.trim().isEmpty()) {
            // Search by first name, last name, or email
            String searchTerm = search.trim().toLowerCase();
            deliveryAgents = userRepository.findByRoleAndSearchTerm(UserRole.DELIVERY_AGENT, searchTerm, pageable);
        } else {
            // Get all delivery agents with pagination
            deliveryAgents = userRepository.findByRole(UserRole.DELIVERY_AGENT, pageable);
        }

        return deliveryAgents.map(this::convertToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentWorkloadDTO getDeliveryAgentWorkload(UUID deliveryAgentId) {
        User deliveryAgent = userRepository.findById(deliveryAgentId)
                .orElseThrow(() -> new RuntimeException("Delivery agent not found: " + deliveryAgentId));

        if (deliveryAgent.getRole() != UserRole.DELIVERY_AGENT) {
            throw new IllegalArgumentException("User is not a delivery agent: " + deliveryAgentId);
        }

        // Get workload statistics
        int totalAssigned = returnRequestRepository.countByDeliveryAgentId(deliveryAgentId);
        int pendingPickups = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                deliveryAgentId, DeliveryStatus.ASSIGNED);
        int scheduledPickups = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                deliveryAgentId, DeliveryStatus.PICKUP_SCHEDULED);
        int inProgressPickups = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                deliveryAgentId, DeliveryStatus.PICKUP_IN_PROGRESS);
        int completedToday = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatusAndPickupCompletedAtAfter(
                deliveryAgentId, DeliveryStatus.PICKUP_COMPLETED, LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
        int failedToday = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatusAndPickupStartedAtAfter(
                deliveryAgentId, DeliveryStatus.PICKUP_FAILED, LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));

        int totalCompleted = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                deliveryAgentId, DeliveryStatus.PICKUP_COMPLETED);
        int totalFailed = returnRequestRepository.countByDeliveryAgentIdAndDeliveryStatus(
                deliveryAgentId, DeliveryStatus.PICKUP_FAILED);

        // Calculate success rate
        double successRate = 0.0;
        if (totalCompleted + totalFailed > 0) {
            successRate = (totalCompleted * 100.0) / (totalCompleted + totalFailed);
        }

        return DeliveryAgentWorkloadDTO.builder()
                .deliveryAgentId(deliveryAgentId)
                .deliveryAgentName(deliveryAgent.getFirstName() + " " + deliveryAgent.getLastName())
                .deliveryAgentEmail(deliveryAgent.getUserEmail())
                .totalAssignedReturns(totalAssigned)
                .pendingPickups(pendingPickups)
                .scheduledPickups(scheduledPickups)
                .inProgressPickups(inProgressPickups)
                .completedPickupsToday(completedToday)
                .failedPickupsToday(failedToday)
                .totalCompletedReturns(totalCompleted)
                .totalFailedReturns(totalFailed)
                .successRate(successRate)
                .isAvailable(true) // TODO: Implement availability logic
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryStatsDTO getDeliveryStatistics() {
        // Get overall statistics
        long totalReturns = returnRequestRepository.countByStatus(ReturnRequest.ReturnStatus.APPROVED);
        long assignedReturns = returnRequestRepository.countByDeliveryAgentIdIsNotNull();
        long unassignedReturns = totalReturns - assignedReturns;

        // Get status breakdown
        long notAssigned = returnRequestRepository.countByDeliveryStatus(DeliveryStatus.NOT_ASSIGNED);
        long assigned = returnRequestRepository.countByDeliveryStatus(DeliveryStatus.ASSIGNED);
        long scheduled = returnRequestRepository.countByDeliveryStatus(DeliveryStatus.PICKUP_SCHEDULED);
        long inProgress = returnRequestRepository.countByDeliveryStatus(DeliveryStatus.PICKUP_IN_PROGRESS);
        long completed = returnRequestRepository.countByDeliveryStatus(DeliveryStatus.PICKUP_COMPLETED);
        long failed = returnRequestRepository.countByDeliveryStatus(DeliveryStatus.PICKUP_FAILED);
        long cancelled = returnRequestRepository.countByDeliveryStatus(DeliveryStatus.CANCELLED);

        // Calculate success rate
        double successRate = 0.0;
        if (completed + failed > 0) {
            successRate = (completed * 100.0) / (completed + failed);
        }

        // Get agent statistics
        int totalAgents = userRepository.countByRole(UserRole.DELIVERY_AGENT);

        // Get today's statistics
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        long todayPickups = returnRequestRepository.countByPickupStartedAtAfter(startOfDay);
        long todayCompletions = returnRequestRepository.countByPickupCompletedAtAfter(startOfDay);
        long todayFailures = returnRequestRepository.countByDeliveryStatusAndPickupStartedAtAfter(
                DeliveryStatus.PICKUP_FAILED, startOfDay);

        return DeliveryStatsDTO.builder()
                .totalReturnRequests(totalReturns)
                .assignedReturns(assignedReturns)
                .unassignedReturns(unassignedReturns)
                .completedPickups(completed)
                .failedPickups(failed)
                .cancelledAssignments(cancelled)
                .notAssignedCount(notAssigned)
                .assignedCount(assigned)
                .pickupScheduledCount(scheduled)
                .pickupInProgressCount(inProgress)
                .pickupCompletedCount(completed)
                .pickupFailedCount(failed)
                .cancelledCount(cancelled)
                .overallSuccessRate(successRate)
                .totalDeliveryAgents(totalAgents)
                .todayPickups(todayPickups)
                .todayCompletions(todayCompletions)
                .todayFailures(todayFailures)
                .build();
    }

    @Override
    public ReturnDeliveryAssignmentDTO reassignDeliveryAgent(Long returnRequestId, UUID newDeliveryAgentId,
            UUID assignedBy, String reason) {
        log.info("Reassigning return request {} from current agent to {}", returnRequestId, newDeliveryAgentId);

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnRequestId));

        if (!returnRequest.isAssignedToDeliveryAgent()) {
            throw new IllegalStateException("Return request is not currently assigned to any delivery agent");
        }

        if (returnRequest.isPickupInProgressOrCompleted()) {
            throw new IllegalStateException("Cannot reassign delivery agent when pickup is in progress or completed");
        }

        // Validate new delivery agent
        User newDeliveryAgent = userRepository.findById(newDeliveryAgentId)
                .orElseThrow(() -> new RuntimeException("New delivery agent not found: " + newDeliveryAgentId));

        if (newDeliveryAgent.getRole() != UserRole.DELIVERY_AGENT) {
            throw new IllegalArgumentException("User is not a delivery agent: " + newDeliveryAgentId);
        }

        // Reassign
        returnRequest.assignDeliveryAgent(newDeliveryAgentId, assignedBy, reason);

        // Save the updated return request
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Successfully reassigned return request {} to delivery agent {}", returnRequestId, newDeliveryAgentId);

        return convertToAssignmentDTO(returnRequest);
    }

    @Override
    public ReturnDeliveryAssignmentDTO cancelAssignment(Long returnRequestId, String reason) {
        log.info("Cancelling assignment for return request {}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnRequestId));

        if (!returnRequest.isAssignedToDeliveryAgent()) {
            throw new IllegalStateException("Return request is not currently assigned to any delivery agent");
        }

        if (returnRequest.getDeliveryStatus() == DeliveryStatus.PICKUP_COMPLETED) {
            throw new IllegalStateException("Cannot cancel assignment for completed pickup");
        }

        // Cancel assignment
        returnRequest.cancelDeliveryAssignment(reason);

        // Save the updated return request
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Successfully cancelled assignment for return request {}", returnRequestId);

        return convertToAssignmentDTO(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentHistoryDTO> getAssignmentHistory(Long returnRequestId) {
        // TODO: Implement assignment history tracking
        // This would require a separate entity to track assignment history
        throw new UnsupportedOperationException("Assignment history tracking not yet implemented");
    }

    /**
     * Converts ReturnRequest entity to ReturnDeliveryAssignmentDTO
     */
    private ReturnDeliveryAssignmentDTO convertToAssignmentDTO(ReturnRequest returnRequest) {
        ReturnDeliveryAssignmentDTO.ReturnDeliveryAssignmentDTOBuilder builder = ReturnDeliveryAssignmentDTO.builder()
                .returnRequestId(returnRequest.getId())
                .deliveryAgentId(returnRequest.getDeliveryAgentId())
                .assignedBy(returnRequest.getAssignedBy())
                .assignedAt(returnRequest.getAssignedAt())
                .deliveryStatus(returnRequest.getDeliveryStatus())
                .deliveryStatusDisplayName(returnRequest.getDeliveryStatusDisplayName())
                .deliveryNotes(returnRequest.getDeliveryNotes())
                .pickupScheduledAt(returnRequest.getPickupScheduledAt())
                .estimatedPickupTime(returnRequest.getEstimatedPickupTime())
                .pickupStartedAt(returnRequest.getPickupStartedAt())
                .pickupCompletedAt(returnRequest.getPickupCompletedAt())
                .actualPickupTime(returnRequest.getActualPickupTime())
                .returnReason(returnRequest.getReason())
                .returnSubmittedAt(returnRequest.getSubmittedAt());

        // Add delivery agent information if assigned
        if (returnRequest.getDeliveryAgent() != null) {
            User deliveryAgent = returnRequest.getDeliveryAgent();
            builder.deliveryAgentName(deliveryAgent.getFirstName() + " " + deliveryAgent.getLastName())
                    .deliveryAgentEmail(deliveryAgent.getUserEmail())
                    .deliveryAgentPhone(deliveryAgent.getPhoneNumber());
        }

        // Add assigner information if available
        if (returnRequest.getAssignedByUser() != null) {
            User assigner = returnRequest.getAssignedByUser();
            builder.assignedByName(assigner.getFirstName() + " " + assigner.getLastName());
        }

        // Add customer information if available
        if (returnRequest.getCustomer() != null) {
            User customer = returnRequest.getCustomer();
            builder.customerName(customer.getFirstName() + " " + customer.getLastName())
                    .customerEmail(customer.getUserEmail())
                    .customerPhone(customer.getPhoneNumber());
        }

        if (returnRequest.getShopOrder() != null) {
            builder.orderNumber(returnRequest.getShopOrder().getShopOrderCode());
        }

        return builder.build();

    }

    /**
     * Converts User entity to UserDTO
     */
    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userEmail(user.getUserEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .enabled(user.isEnabled())
                .build();
    }
}