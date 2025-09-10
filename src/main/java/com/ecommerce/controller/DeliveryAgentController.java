package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.service.ReadyForDeliveryGroupService;
import com.ecommerce.ServiceImpl.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery-agent")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Delivery Agent", description = "Delivery Agent Portal APIs")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryAgentController {

    private final ReadyForDeliveryGroupService readyForDeliveryGroupService;

    @Operation(summary = "Get delivery agent dashboard data", description = "Get all data required for delivery agent dashboard")
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully", content = @Content(schema = @Schema(implementation = DeliveryAgentDashboardDTO.class)))
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<DeliveryAgentDashboardDTO> getDashboardData(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UUID agentId = userDetails.getUserId();
            log.info("Getting dashboard data for delivery agent: {}", agentId);

            DeliveryAgentDashboardDTO dashboardData = readyForDeliveryGroupService.getDeliveryAgentDashboard(agentId);

            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error getting dashboard data for delivery agent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get orders for a delivery group", description = "Get all orders belonging to a specific delivery group")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @GetMapping("/groups/{groupId}/orders")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<List<OrderDTO>> getOrdersForGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UUID agentId = userDetails.getUserId();
            log.info("Getting orders for group {} by agent {}", groupId, agentId);

            List<OrderDTO> orders = readyForDeliveryGroupService.getOrdersForGroup(groupId, agentId);

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error getting orders for group {}: {}", groupId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Mark delivery as started", description = "Mark that delivery has started for a group")
    @ApiResponse(responseCode = "200", description = "Delivery marked as started successfully")
    @PutMapping("/groups/{groupId}/start-delivery")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<DeliveryGroupDto> startDelivery(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UUID agentId = userDetails.getUserId();
            log.info("Starting delivery for group {} by agent {}", groupId, agentId);

            DeliveryGroupDto updatedGroup = readyForDeliveryGroupService.startDelivery(groupId, agentId);

            return ResponseEntity.ok(updatedGroup);
        } catch (Exception e) {
            log.error("Error starting delivery for group {}: {}", groupId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Mark delivery as finished", description = "Mark that delivery has finished for a group")
    @ApiResponse(responseCode = "200", description = "Delivery marked as finished successfully")
    @PutMapping("/groups/{groupId}/finish-delivery")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<DeliveryGroupDto> finishDelivery(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UUID agentId = userDetails.getUserId();
            log.info("Finishing delivery for group {} by agent {}", groupId, agentId);

            DeliveryGroupDto updatedGroup = readyForDeliveryGroupService.finishDelivery(groupId, agentId);

            return ResponseEntity.ok(updatedGroup);
        } catch (Exception e) {
            log.error("Error finishing delivery for group {}: {}", groupId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get order details", description = "Get detailed information about a specific order")
    @ApiResponse(responseCode = "200", description = "Order details retrieved successfully")
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public ResponseEntity<OrderDTO> getOrderDetails(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UUID agentId = userDetails.getUserId();
            log.info("Getting order details for order {} by agent {}", orderId, agentId);

            OrderDTO order = readyForDeliveryGroupService.getOrderDetailsForAgent(orderId, agentId);

            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error getting order details for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
