package com.ecommerce.controller;

import com.ecommerce.dto.CustomerOrderDTO;
import com.ecommerce.dto.CreateOrderDTO;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import com.ecommerce.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Order Management", description = "APIs for customers to manage their orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get customer orders", description = "Retrieve all orders for the authenticated customer")
    public ResponseEntity<?> getCustomerOrders() {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }

            List<CustomerOrderDTO> orders = orderService.getCustomerOrders(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching customer orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch orders"
            ));
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get customer order by ID", description = "Retrieve a specific order for the authenticated customer")
    public ResponseEntity<?> getCustomerOrderById(@PathVariable Long orderId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }

            CustomerOrderDTO order = orderService.getCustomerOrderById(userId, orderId);
            
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
            log.error("Error fetching customer order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch order"
            ));
        }
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get customer order by number", description = "Retrieve a specific order by order number for the authenticated customer")
    public ResponseEntity<?> getCustomerOrderByNumber(@PathVariable String orderNumber) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }

            CustomerOrderDTO order = orderService.getCustomerOrderByNumber(userId, orderNumber);
            
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
            log.error("Error fetching customer order by number: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch order"
            ));
        }
    }

    @PostMapping
    @Operation(summary = "Create new order", description = "Create a new order for the authenticated customer")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderDTO createOrderDTO) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }

            // Create order and convert to customer DTO
            var order = orderService.createOrder(userId, createOrderDTO);
            CustomerOrderDTO customerOrder = orderService.getCustomerOrderById(userId, order.getOrderId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully");
            response.put("data", customerOrder);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for order creation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "error", "VALIDATION_ERROR"
            ));
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to create order",
                "error", "INTERNAL_ERROR"
            ));
        }
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order for the authenticated customer")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }

            var order = orderService.cancelOrder(userId, orderId);
            CustomerOrderDTO customerOrder = orderService.getCustomerOrderById(userId, order.getOrderId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order cancelled successfully");
            response.put("data", customerOrder);
            
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
            log.error("Error cancelling order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to cancel order"
            ));
        }
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof com.ecommerce.entity.User u && u.getId() != null) {
                return u.getId();
            }
            if (principal instanceof UserDetails ud) {
                String email = ud.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name).map(com.ecommerce.entity.User::getId).orElse(null);
            }
        } catch (Exception ignored) { }
        return null;
    }
}
