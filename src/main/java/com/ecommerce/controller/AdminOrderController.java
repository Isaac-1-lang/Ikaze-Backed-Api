package com.ecommerce.controller;

import com.ecommerce.dto.AdminOrderDTO;
import com.ecommerce.dto.OrderSearchDTO;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

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
    @Operation(summary = "Get all orders with pagination", description = "Retrieve all orders in the system with pagination support")
    public ResponseEntity<?> getAllOrders(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "15")
            @RequestParam(defaultValue = "15") int size,
            @Parameter(description = "Sort by field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            // Create sort object
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            // Create pageable object
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Get paginated orders
            Page<AdminOrderDTO> ordersPage = orderService.getAllAdminOrdersPaginated(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ordersPage.getContent());
            response.put("pagination", Map.of(
                "currentPage", ordersPage.getNumber(),
                "totalPages", ordersPage.getTotalPages(),
                "totalElements", ordersPage.getTotalElements(),
                "pageSize", ordersPage.getSize(),
                "hasNext", ordersPage.hasNext(),
                "hasPrevious", ordersPage.hasPrevious(),
                "isFirst", ordersPage.isFirst(),
                "isLast", ordersPage.isLast()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching all orders: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while fetching orders.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid status: " + status);
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error fetching orders by status: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while fetching orders by status.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error fetching order by ID: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while fetching order by ID.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error fetching order by number: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while fetching order by number.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Update the status of an order")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Status is required");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Missing status field in request body.");
                return ResponseEntity.badRequest().body(response);
            }

            var order = orderService.updateOrderStatus(orderId, status);
            AdminOrderDTO adminOrder = orderService.getAdminOrderById(order.getOrderId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated successfully");
            response.put("data", adminOrder);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while updating order status.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{orderId}/tracking")
    @Operation(summary = "Update order tracking", description = "Update tracking information for an order")
    public ResponseEntity<?> updateOrderTracking(@PathVariable Long orderId, @RequestBody Map<String, String> request) {
        try {
            String trackingNumber = request.get("trackingNumber");
            String estimatedDelivery = request.get("estimatedDelivery");

            if (trackingNumber == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Tracking number is required");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Missing trackingNumber field in request body.");
                return ResponseEntity.badRequest().body(response);
            }

            var order = orderService.updateOrderTracking(orderId, trackingNumber, estimatedDelivery);
            AdminOrderDTO adminOrder = orderService.getAdminOrderById(order.getOrderId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tracking information updated successfully");
            response.put("data", adminOrder);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error updating order tracking: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while updating tracking information.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/search")
    @Operation(summary = "Search orders with filters", description = "Search and filter orders based on various criteria with pagination")
    public ResponseEntity<?> searchOrders(@Valid @RequestBody OrderSearchDTO searchRequest) {
        try {
            log.info("Searching orders with criteria: {}", searchRequest);

            // Validate that at least one filter is provided
            if (!searchRequest.hasAtLeastOneFilter()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "At least one search criterion must be provided");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Please provide at least one filter criterion to search orders.");
                return ResponseEntity.badRequest().body(response);
            }

            // Set default pagination if not provided
            if (searchRequest.getPage() == null) {
                searchRequest.setPage(0);
            }
            if (searchRequest.getSize() == null) {
                searchRequest.setSize(15);
            }
            if (searchRequest.getSortBy() == null) {
                searchRequest.setSortBy("createdAt");
            }
            if (searchRequest.getSortDirection() == null) {
                searchRequest.setSortDirection("desc");
            }

            // Create sort object
            Sort sort = searchRequest.getSortDirection().equalsIgnoreCase("desc") 
                ? Sort.by(searchRequest.getSortBy()).descending() 
                : Sort.by(searchRequest.getSortBy()).ascending();
            
            // Create pageable object
            Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
            
            // Search orders
            Page<AdminOrderDTO> ordersPage = orderService.searchOrders(searchRequest, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ordersPage.getContent());
            response.put("pagination", Map.of(
                "currentPage", ordersPage.getNumber(),
                "totalPages", ordersPage.getTotalPages(),
                "totalElements", ordersPage.getTotalElements(),
                "pageSize", ordersPage.getSize(),
                "hasNext", ordersPage.hasNext(),
                "hasPrevious", ordersPage.hasPrevious(),
                "isFirst", ordersPage.isFirst(),
                "isLast", ordersPage.isLast()
            ));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search criteria: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid search criteria: " + e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error searching orders: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while searching orders.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
