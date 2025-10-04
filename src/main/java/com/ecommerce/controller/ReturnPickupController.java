package com.ecommerce.controller;

import com.ecommerce.dto.ReturnPickupRequestDTO;
import com.ecommerce.dto.ReturnPickupResponseDTO;
import com.ecommerce.service.ReturnPickupService;
import com.ecommerce.util.JwtUtil;
import com.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for return pickup operations by delivery agents
 */
@RestController
@RequestMapping("/api/v1/delivery-agent/pickup")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Return Pickup", description = "API for delivery agents to process return pickups")
public class ReturnPickupController {

    private final ReturnPickupService returnPickupService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/process")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Process return pickup", 
               description = "Process the pickup of returned items by delivery agent")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pickup processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not a delivery agent"),
        @ApiResponse(responseCode = "404", description = "Return request not found"),
        @ApiResponse(responseCode = "409", description = "Validation failed - Invalid status or expired return window"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ReturnPickupResponseDTO> processReturnPickup(
            @Parameter(description = "Return pickup request with item statuses", required = true)
            @Valid @RequestBody ReturnPickupRequestDTO pickupRequest,
            HttpServletRequest request) {
        
        try {
            log.info("Processing return pickup request for return request ID: {}", pickupRequest.getReturnRequestId());
            
            // Extract delivery agent ID from JWT token
            UUID deliveryAgentId = extractDeliveryAgentId(request);
            
            // Process the pickup
            ReturnPickupResponseDTO response = returnPickupService.processReturnPickup(pickupRequest, deliveryAgentId);
            
            log.info("Successfully processed return pickup for request ID: {}", pickupRequest.getReturnRequestId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for return pickup: {}", e.getMessage());
            throw new RuntimeException("Invalid request: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Invalid state for return pickup: {}", e.getMessage());
            throw new RuntimeException("Invalid state: " + e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Database constraint violation during return pickup: {}", e.getMessage());
            String message = "Database constraint violation. This may be due to invalid status combination or missing required fields.";
            if (e.getMessage().contains("return_requests_status_check")) {
                message = "Invalid return request status combination. Please ensure the return request is in the correct state for pickup completion.";
            }
            throw new RuntimeException(message);
        } catch (RuntimeException e) {
            log.error("Error processing return pickup: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing return pickup: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process return pickup: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Validate return pickup", 
               description = "Validate if a return pickup request can be processed")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation successful"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Return request not found")
    })
    public ResponseEntity<String> validateReturnPickup(
            @Parameter(description = "Return pickup request to validate", required = true)
            @Valid @RequestBody ReturnPickupRequestDTO pickupRequest,
            HttpServletRequest request) {
        
        try {
            log.debug("Validating return pickup request for return request ID: {}", pickupRequest.getReturnRequestId());
            
            // Extract delivery agent ID from JWT token
            UUID deliveryAgentId = extractDeliveryAgentId(request);
            
            // Validate the pickup request
            returnPickupService.validateReturnPickup(pickupRequest, deliveryAgentId);
            
            log.debug("Return pickup validation successful for request ID: {}", pickupRequest.getReturnRequestId());
            return ResponseEntity.ok("Validation successful. Return pickup can be processed.");
            
        } catch (RuntimeException e) {
            log.error("Return pickup validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during return pickup validation: {}", e.getMessage(), e);
            throw new RuntimeException("Validation failed: " + e.getMessage());
        }
    }

    /**
     * Extract delivery agent ID from JWT token in the request
     */
    private UUID extractDeliveryAgentId(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                throw new RuntimeException("Authorization token is required");
            }
            
            String username = jwtUtil.extractUsername(token);
            // Find user by email/username and return their ID
            return userRepository.findByUserEmail(username)
                .map(com.ecommerce.entity.User::getId)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + username));
            
        } catch (Exception e) {
            log.error("Error extracting delivery agent ID from token: {}", e.getMessage());
            throw new RuntimeException("Invalid authorization token");
        }
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
