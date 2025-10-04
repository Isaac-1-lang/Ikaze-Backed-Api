package com.ecommerce.controller;

import com.ecommerce.dto.ReturnRequestDTO;
import com.ecommerce.dto.DeliveryAgentReturnTableDTO;
import com.ecommerce.entity.ReturnRequest;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.DeliveryAgentReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery-agent/returns")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DeliveryAgentReturnController {

    private final DeliveryAgentReturnService deliveryAgentReturnService;
    private final UserRepository userRepository;

    /**
     * Get return requests assigned to the current delivery agent with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<Page<DeliveryAgentReturnTableDTO>> getAssignedReturnRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String returnStatus,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String orderNumber
    ) {
        try {
            UUID deliveryAgentId = getCurrentUserId();
            
            // Create pageable with sorting
            Sort sort = Sort.by(sortDirection.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Convert LocalDate to LocalDateTime for filtering
            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
            
            Page<DeliveryAgentReturnTableDTO> returnRequests = deliveryAgentReturnService.getAssignedReturnRequests(
                deliveryAgentId, 
                pageable, 
                returnStatus, 
                deliveryStatus, 
                startDateTime, 
                endDateTime, 
                customerName, 
                orderNumber
            );
            
            log.info("Retrieved {} return requests for delivery agent {}", 
                returnRequests.getTotalElements(), deliveryAgentId);
            
            return ResponseEntity.ok(returnRequests);
            
        } catch (Exception e) {
            log.error("Error retrieving return requests for delivery agent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve return requests: " + e.getMessage());
        }
    }

    /**
     * Get return request details by ID (only if assigned to current delivery agent)
     */
    @GetMapping("/{returnRequestId}")
    public ResponseEntity<ReturnRequestDTO> getReturnRequestById(@PathVariable Long returnRequestId) {
        try {
            UUID deliveryAgentId = getCurrentUserId();
            
            ReturnRequestDTO returnRequest = deliveryAgentReturnService.getReturnRequestById(
                returnRequestId, deliveryAgentId
            );
            
            return ResponseEntity.ok(returnRequest);
            
        } catch (Exception e) {
            log.error("Error retrieving return request {}: {}", returnRequestId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve return request: " + e.getMessage());
        }
    }

    /**
     * Update delivery status of a return request
     */
    @PutMapping("/{returnRequestId}/delivery-status")
    public ResponseEntity<ReturnRequestDTO> updateDeliveryStatus(
            @PathVariable Long returnRequestId,
            @RequestParam ReturnRequest.DeliveryStatus deliveryStatus,
            @RequestParam(required = false) String notes
    ) {
        try {
            UUID deliveryAgentId = getCurrentUserId();
            
            ReturnRequestDTO updatedRequest = deliveryAgentReturnService.updateDeliveryStatus(
                returnRequestId, deliveryAgentId, deliveryStatus, notes
            );
            
            log.info("Updated delivery status for return request {} to {} by delivery agent {}", 
                returnRequestId, deliveryStatus, deliveryAgentId);
            
            return ResponseEntity.ok(updatedRequest);
            
        } catch (Exception e) {
            log.error("Error updating delivery status for return request {}: {}", 
                returnRequestId, e.getMessage(), e);
            throw new RuntimeException("Failed to update delivery status: " + e.getMessage());
        }
    }

    /**
     * Get delivery agent statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DeliveryAgentReturnService.DeliveryAgentStats> getDeliveryAgentStats() {
        try {
            UUID deliveryAgentId = getCurrentUserId();
            
            DeliveryAgentReturnService.DeliveryAgentStats stats = 
                deliveryAgentReturnService.getDeliveryAgentStats(deliveryAgentId);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving delivery agent stats: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve statistics: " + e.getMessage());
        }
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

            // Handle CustomUserDetails (JWT tokens)
            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            // Handle User entity
            if (principal instanceof User user && user.getId() != null) {
                return user.getId();
            }

            // Handle UserDetails
            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            // Fallback to auth name
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name)
                    .map(User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + name));
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to get current user ID: " + e.getMessage());
        }
        throw new RuntimeException("Unable to get current user ID");
    }
}
