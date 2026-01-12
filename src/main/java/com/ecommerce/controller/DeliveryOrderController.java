package com.ecommerce.controller;

import com.ecommerce.dto.DeliveryOrderDTO;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/delivery/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Delivery Order Management", description = "APIs for delivery agencies to access delivery information")
@PreAuthorize("hasRole('DELIVERY')")
public class DeliveryOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get delivery orders", description = "Retrieve all orders for delivery")
    public ResponseEntity<?> getDeliveryOrders() {
        try {
            List<DeliveryOrderDTO> orders = orderService.getDeliveryOrders();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching delivery orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch orders"));
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get delivery orders by status", description = "Retrieve delivery orders filtered by status")
    public ResponseEntity<?> getDeliveryOrdersByStatus(@PathVariable String status) {
        try {
            List<DeliveryOrderDTO> orders = orderService.getDeliveryOrdersByStatus(status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid status: " + status));
        } catch (Exception e) {
            log.error("Error fetching delivery orders by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch orders"));
        }
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get delivery order by number", description = "Retrieve delivery information for a specific order")
    public ResponseEntity<?> getDeliveryOrderByNumber(@PathVariable String orderNumber) {
        try {
            DeliveryOrderDTO order = orderService.getDeliveryOrderByNumber(orderNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Order not found"));
        } catch (Exception e) {
            log.error("Error fetching delivery order by number: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch order"));
        }
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update delivery status", description = "Update the delivery status of an order")
    public ResponseEntity<?> updateDeliveryStatus(@PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Status is required"));
            }

            // Only allow delivery-related status updates
            if (!isValidDeliveryStatus(status)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid delivery status. Allowed values: SHIPPED, OUT_FOR_DELIVERY, DELIVERED"));
            }

            var shopOrder = orderService.updateShopOrderStatus(orderId, status);
            DeliveryOrderDTO deliveryOrder = orderService.getDeliveryOrderByNumber(shopOrder.getShopOrderCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Delivery status updated successfully");
            response.put("data", deliveryOrder);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Order not found"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating delivery status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to update delivery status"));
        }
    }

    @PutMapping("/{orderId}/tracking")
    @Operation(summary = "Update delivery tracking", description = "Update tracking information for delivery")
    public ResponseEntity<?> updateDeliveryTracking(@PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        try {
            String trackingNumber = request.get("trackingNumber");
            String estimatedDelivery = request.get("estimatedDelivery");

            if (trackingNumber == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Tracking number is required"));
            }

            var shopOrder = orderService.updateShopOrderTracking(orderId, trackingNumber, estimatedDelivery);
            DeliveryOrderDTO deliveryOrder = orderService.getDeliveryOrderByNumber(shopOrder.getShopOrderCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tracking information updated successfully");
            response.put("data", deliveryOrder);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Order not found"));
        } catch (Exception e) {
            log.error("Error updating delivery tracking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to update tracking information"));
        }
    }

    private boolean isValidDeliveryStatus(String status) {
        return status != null && (status.equals("SHIPPED") ||
                status.equals("OUT_FOR_DELIVERY") ||
                status.equals("DELIVERED"));
    }
}
