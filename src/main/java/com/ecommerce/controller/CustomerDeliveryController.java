package com.ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.ecommerce.dto.DeliveryAssignmentDTO;
import com.ecommerce.service.DeliveryAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery/customer")
@RequiredArgsConstructor
public class CustomerDeliveryController {
    private final DeliveryAssignmentService deliveryAssignmentService;

    @Operation(summary = "Get delivery info for an order", description = "Retrieves the delivery assignment and agent info for the specified order.", responses = {
            @ApiResponse(responseCode = "200", description = "Delivery info retrieved", content = @Content(schema = @Schema(implementation = com.ecommerce.dto.DeliveryAssignmentDTO.class)))
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryAssignmentDTO> getDeliveryInfoForOrder(@PathVariable Long orderId) {
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
