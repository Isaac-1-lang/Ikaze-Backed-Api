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
@RequestMapping("/api/v1/delivery/admin")
@RequiredArgsConstructor
public class AdminDeliveryController {
    private final DeliveryAssignmentService deliveryAssignmentService;

    @Operation(summary = "Assign an order to a delivery agent", description = "Assigns the specified order to the given delivery agent.", responses = {
            @ApiResponse(responseCode = "200", description = "Assignment created", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.ecommerce.dto.DeliveryAssignmentDTO.class)))
    })
    @PostMapping("/assign")
    public ResponseEntity<DeliveryAssignmentDTO> assignAgent(@RequestParam Long orderId,
            @RequestParam String agentId) {
        try {
            DeliveryAssignmentDTO dto = deliveryAssignmentService.assignAgentToOrder(orderId,
                    java.util.UUID.fromString(agentId));
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @Operation(summary = "Change the delivery agent for an order", description = "Reassigns the specified order to a new delivery agent.", responses = {
            @ApiResponse(responseCode = "200", description = "Assignment updated", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.ecommerce.dto.DeliveryAssignmentDTO.class)))
    })
    @PutMapping("/change-agent")
    public ResponseEntity<DeliveryAssignmentDTO> changeAgent(@RequestParam Long orderId,
            @RequestParam String newAgentId) {
        try {
            DeliveryAssignmentDTO dto = deliveryAssignmentService.changeAgentForOrder(orderId,
                    java.util.UUID.fromString(newAgentId));
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @Operation(summary = "Unassign the delivery agent from an order", description = "Removes the delivery agent assignment from the specified order.", responses = {
            @ApiResponse(responseCode = "204", description = "Unassigned successfully")
    })
    @DeleteMapping("/unassign")
    public ResponseEntity<Void> unassignAgent(@RequestParam Long orderId) {
        try {
            deliveryAssignmentService.unassignAgentFromOrder(orderId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Cancel a delivery assignment", description = "Cancels the delivery assignment for the given assignment ID.", responses = {
            @ApiResponse(responseCode = "204", description = "Assignment canceled")
    })
    @PutMapping("/cancel/{assignmentId}")
    public ResponseEntity<Void> cancelAssignment(@PathVariable Long assignmentId) {
        try {
            deliveryAssignmentService.cancelAssignment(assignmentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get all delivery assignments", description = "Retrieves all delivery assignments in the system.", responses = {
            @ApiResponse(responseCode = "200", description = "Assignments retrieved", content = @Content(schema = @Schema(implementation = com.ecommerce.dto.DeliveryAssignmentDTO.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<List<DeliveryAssignmentDTO>> getAllAssignments() {
        try {
            return ResponseEntity.ok(deliveryAssignmentService.getAllAssignments());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @Operation(summary = "Get delivery assignment for an order", description = "Retrieves the delivery assignment for the specified order.", responses = {
            @ApiResponse(responseCode = "200", description = "Assignment retrieved", content = @Content(schema = @Schema(implementation = com.ecommerce.dto.DeliveryAssignmentDTO.class)))
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryAssignmentDTO> getAssignmentByOrder(@PathVariable Long orderId) {
        try {
            DeliveryAssignmentDTO dto = deliveryAssignmentService.getAssignmentByOrderId(orderId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
