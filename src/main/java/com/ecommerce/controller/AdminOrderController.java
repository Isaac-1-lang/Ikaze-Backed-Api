package com.ecommerce.controller;

import com.ecommerce.dto.AdminOrderDTO;
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
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Order Management", description = "APIs for administrators to manage all orders")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieve all orders in the system")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<AdminOrderDTO> orders = orderService.getAllOrders();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching all orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch orders"
            ));
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieve orders filtered by status")
    public ResponseEntity<?> getOrdersByStatus(@PathVariable String status) {
        try {
            List<AdminOrderDTO> orders = orderService.getOrdersByStatus(status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid status: " + status
            ));
        } catch (Exception e) {
            log.error("Error fetching orders by status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch orders"
            ));
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order by ID")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            AdminOrderDTO order = orderService.getAdminOrderById(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order);
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Order not found"
            ));
        } catch (Exception e) {
            log.error("Error fetching order by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch order"
            ));
        }
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by number", description = "Retrieve a specific order by order number")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            AdminOrderDTO order = orderService.getAdminOrderByNumber(orderNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order);
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Order not found"
            ));
        } catch (Exception e) {
            log.error("Error fetching order by number: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch order"
            ));
        }
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Update the status of an order")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Status is required"
                ));
            }

            var order = orderService.updateOrderStatus(orderId, status);
            AdminOrderDTO adminOrder = orderService.getAdminOrderById(order.getOrderId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated successfully");
            response.put("data", adminOrder);
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Order not found"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to update order status"
            ));
        }
    }

    @PutMapping("/{orderId}/tracking")
    @Operation(summary = "Update order tracking", description = "Update tracking information for an order")
    public ResponseEntity<?> updateOrderTracking(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String trackingNumber = request.get("trackingNumber");
            String estimatedDelivery = request.get("estimatedDelivery");

            if (trackingNumber == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tracking number is required"
                ));
            }

            var order = orderService.updateOrderTracking(orderId, trackingNumber, estimatedDelivery);
            AdminOrderDTO adminOrder = orderService.getAdminOrderById(order.getOrderId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tracking information updated successfully");
            response.put("data", adminOrder);
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Order not found"
            ));
        } catch (Exception e) {
            log.error("Error updating order tracking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to update tracking information"
            ));
        }
    }
}
