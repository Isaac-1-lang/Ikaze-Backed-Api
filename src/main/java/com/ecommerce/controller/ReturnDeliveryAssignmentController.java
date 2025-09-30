package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.entity.ReturnRequest.DeliveryStatus;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ReturnDeliveryAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing return delivery assignments
 */
@RestController
@RequestMapping("/api/v1/return-delivery")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Return Delivery Assignment", description = "APIs for managing return delivery assignments")
public class ReturnDeliveryAssignmentController {

    private final ReturnDeliveryAssignmentService deliveryAssignmentService;
    private final UserRepository userRepository;

    @Operation(summary = "Assign delivery agent to return request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery agent assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Return request or delivery agent not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ReturnDeliveryAssignmentDTO> assignDeliveryAgent(
            @Valid @RequestBody AssignDeliveryAgentRequestDTO request) {
        
        UUID assignedBy = getCurrentUserId();
        ReturnDeliveryAssignmentDTO assignment = deliveryAssignmentService.assignDeliveryAgent(request, assignedBy);
        
        return ResponseEntity.ok(assignment);
    }

    @Operation(summary = "Unassign delivery agent from return request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery agent unassigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Return request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/unassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ReturnDeliveryAssignmentDTO> unassignDeliveryAgent(
            @Valid @RequestBody UnassignDeliveryAgentRequestDTO request) {
        
        ReturnDeliveryAssignmentDTO assignment = deliveryAssignmentService.unassignDeliveryAgent(request);
        
        return ResponseEntity.ok(assignment);
    }

    @Operation(summary = "Schedule pickup for return request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pickup scheduled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Return request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/schedule-pickup")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DELIVERY_AGENT')")
    public ResponseEntity<ReturnDeliveryAssignmentDTO> schedulePickup(
            @Valid @RequestBody SchedulePickupRequestDTO request) {
        
        ReturnDeliveryAssignmentDTO assignment = deliveryAssignmentService.schedulePickup(request);
        
        return ResponseEntity.ok(assignment);
    }

    @Operation(summary = "Update pickup status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pickup status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Return request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/update-pickup-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DELIVERY_AGENT')")
    public ResponseEntity<ReturnDeliveryAssignmentDTO> updatePickupStatus(
            @Valid @RequestBody UpdatePickupStatusRequestDTO request) {
        
        ReturnDeliveryAssignmentDTO assignment = deliveryAssignmentService.updatePickupStatus(request);
        
        return ResponseEntity.ok(assignment);
    }

    @Operation(summary = "Get assignment details for return request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assignment details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Return request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/assignment/{returnRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DELIVERY_AGENT')")
    public ResponseEntity<ReturnDeliveryAssignmentDTO> getAssignmentDetails(
            @Parameter(description = "Return request ID") @PathVariable Long returnRequestId) {
        
        ReturnDeliveryAssignmentDTO assignment = deliveryAssignmentService.getAssignmentDetails(returnRequestId);
        
        return ResponseEntity.ok(assignment);
    }

    @Operation(summary = "Get assignments by delivery agent")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assignments retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/agent/{deliveryAgentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DELIVERY_AGENT')")
    public ResponseEntity<Page<ReturnDeliveryAssignmentDTO>> getAssignmentsByDeliveryAgent(
            @Parameter(description = "Delivery agent ID") @PathVariable UUID deliveryAgentId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "assignedAt") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<ReturnDeliveryAssignmentDTO> assignments = deliveryAssignmentService
                .getAssignmentsByDeliveryAgent(deliveryAgentId, pageable);
        
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get assignments by delivery status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assignments retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Page<ReturnDeliveryAssignmentDTO>> getAssignmentsByStatus(
            @Parameter(description = "Delivery status") @PathVariable DeliveryStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "assignedAt") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<ReturnDeliveryAssignmentDTO> assignments = deliveryAssignmentService
                .getAssignmentsByStatus(status, pageable);
        
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get assignable return requests")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assignable return requests retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/assignable")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Page<ReturnDeliveryAssignmentDTO>> getAssignableReturnRequests(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "submittedAt") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<ReturnDeliveryAssignmentDTO> assignments = deliveryAssignmentService
                .getAssignableReturnRequests(pageable);
        
        return ResponseEntity.ok(assignments);
    }

    @Operation(summary = "Get available delivery agents")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available delivery agents retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/agents/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<UserDTO>> getAvailableDeliveryAgents() {
        
        List<UserDTO> agents = deliveryAssignmentService.getAvailableDeliveryAgents();
        
        return ResponseEntity.ok(agents);
    }

    @Operation(summary = "Get available delivery agents with pagination and search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available delivery agents retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/agents/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Page<UserDTO>> searchAvailableDeliveryAgents(
            @Parameter(description = "Search term (name or email)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "firstName") String sort,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<UserDTO> agents = deliveryAssignmentService.searchAvailableDeliveryAgents(search, pageable);
        
        return ResponseEntity.ok(agents);
    }

    @Operation(summary = "Get delivery agent workload")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery agent workload retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Delivery agent not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/agents/{deliveryAgentId}/workload")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<DeliveryAgentWorkloadDTO> getDeliveryAgentWorkload(
            @Parameter(description = "Delivery agent ID") @PathVariable UUID deliveryAgentId) {
        
        DeliveryAgentWorkloadDTO workload = deliveryAssignmentService.getDeliveryAgentWorkload(deliveryAgentId);
        
        return ResponseEntity.ok(workload);
    }

    @Operation(summary = "Get delivery statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<DeliveryStatsDTO> getDeliveryStatistics() {
        
        DeliveryStatsDTO stats = deliveryAssignmentService.getDeliveryStatistics();
        
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Reassign delivery agent")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery agent reassigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Return request or delivery agent not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/reassign/{returnRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ReturnDeliveryAssignmentDTO> reassignDeliveryAgent(
            @Parameter(description = "Return request ID") @PathVariable Long returnRequestId,
            @Parameter(description = "New delivery agent ID") @RequestParam UUID newDeliveryAgentId,
            @Parameter(description = "Reason for reassignment") @RequestParam String reason) {
        
        UUID assignedBy = getCurrentUserId();
        ReturnDeliveryAssignmentDTO assignment = deliveryAssignmentService
                .reassignDeliveryAgent(returnRequestId, newDeliveryAgentId, assignedBy, reason);
        
        return ResponseEntity.ok(assignment);
    }

    @Operation(summary = "Cancel delivery assignment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Delivery assignment cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Return request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/cancel/{returnRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ReturnDeliveryAssignmentDTO> cancelAssignment(
            @Parameter(description = "Return request ID") @PathVariable Long returnRequestId,
            @Parameter(description = "Reason for cancellation") @RequestParam String reason) {
        
        ReturnDeliveryAssignmentDTO assignment = deliveryAssignmentService
                .cancelAssignment(returnRequestId, reason);
        
        return ResponseEntity.ok(assignment);
    }

    @Operation(summary = "Get assignment history")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assignment history retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Return request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/history/{returnRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<DeliveryAssignmentHistoryDTO>> getAssignmentHistory(
            @Parameter(description = "Return request ID") @PathVariable Long returnRequestId) {
        
        List<DeliveryAssignmentHistoryDTO> history = deliveryAssignmentService
                .getAssignmentHistory(returnRequestId);
        
        return ResponseEntity.ok(history);
    }

    /**
     * Get current user ID from security context
     */
    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("User not authenticated");
            }

            Object principal = auth.getPrincipal();

            // If principal is CustomUserDetails, extract email and find user
            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            // If principal is User entity
            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                return user.getId();
            }

            // If principal is UserDetails
            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            // Fallback to auth name
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + name));
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to get current user ID: " + e.getMessage());
        }
        throw new RuntimeException("Unable to get current user ID");
    }

    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        log.error("Error in ReturnDeliveryAssignmentController: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        log.error("Illegal state in ReturnDeliveryAssignmentController: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument in ReturnDeliveryAssignmentController: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
