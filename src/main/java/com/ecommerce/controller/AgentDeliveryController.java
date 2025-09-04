package com.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.ecommerce.dto.DeliveryAssignmentDTO;
import com.ecommerce.Enum.DeliveryStatus;
import com.ecommerce.service.DeliveryAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery/agent")
@RequiredArgsConstructor
public class AgentDeliveryController {
    private final DeliveryAssignmentService deliveryAssignmentService;

    @Operation(summary = "Get all assignments for a delivery agent", description = "Retrieves all orders assigned to the specified delivery agent.", responses = {
            @ApiResponse(responseCode = "200", description = "Assignments retrieved", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.ecommerce.dto.DeliveryAssignmentDTO.class)))
    })
    @GetMapping("/my-assignments/{agentId}")
    public ResponseEntity<List<DeliveryAssignmentDTO>> getAssignmentsByAgent(@PathVariable String agentId) {
        try {
            return ResponseEntity
                    .ok(deliveryAssignmentService.getAssignmentsByAgentId(java.util.UUID.fromString(agentId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @Operation(summary = "Update delivery status", description = "Updates the delivery status (Pending, On the Way, Delivered, Canceled) for the given assignment.", responses = {
            @ApiResponse(responseCode = "200", description = "Status updated", content = @Content(schema = @Schema(implementation = com.ecommerce.dto.DeliveryAssignmentDTO.class)))
    })
    @PutMapping("/update-status/{assignmentId}")
    public ResponseEntity<DeliveryAssignmentDTO> updateStatus(@PathVariable Long assignmentId,
            @RequestParam DeliveryStatus status) {
        try {
            DeliveryAssignmentDTO dto = deliveryAssignmentService.updateDeliveryStatus(assignmentId, status);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
