package com.ecommerce.controller;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderInfo;
import com.ecommerce.entity.OrderAddress;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.dto.OrderItemDTO;
import com.ecommerce.dto.OrderAddressDTO;
import com.ecommerce.dto.SimpleProductDTO;
import com.ecommerce.dto.CreateOrderDTO;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Collections;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "APIs for viewing user orders")
public class OrderController {

    /**
     * Get all orders (admin/employee only)
     */
    @GetMapping("/all")
    // @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Get all orders", description = "Retrieve all orders (admin/employee only)", responses = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class)))
    })
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            List<OrderResponseDTO> dtoList = orders.stream().map(this::toDto).toList();
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("data", dtoList);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Failed to fetch all orders", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "An unexpected error occurred while fetching all orders.");
            res.put("errorCode", "INTERNAL_ERROR");
            res.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/by-user/{userId}")
    // @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Get orders by userId", description = "Retrieve all orders for a specific user (admin/employee only)", responses = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class)))
    })
    public ResponseEntity<?> getOrdersByUserId(@PathVariable String userId) {
        try {
            UUID uuid = UUID.fromString(userId);
            List<Order> orders = orderService.getOrdersForUser(uuid);
            List<OrderResponseDTO> dtoList = orders.stream().map(this::toDto).toList();
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("data", dtoList);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "Invalid userId format");
            res.put("errorCode", "VALIDATION_ERROR");
            res.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            log.error("Failed to fetch orders by userId", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "An unexpected error occurred while fetching orders by userId.");
            res.put("errorCode", "INTERNAL_ERROR");
            res.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/id/{orderId}")
    // @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Get order by orderId", description = "Retrieve an order by its orderId (admin/employee only)", responses = {
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", false);
                res.put("message", "Order not found");
                res.put("errorCode", "NOT_FOUND");
                res.put("details", "Order with id " + orderId + " not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
            }
            return ResponseEntity.ok(Map.of("success", true, "data", toDto(order)));
        } catch (Exception e) {
            log.error("Failed to fetch order by id", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "An unexpected error occurred while fetching order by id.");
            res.put("errorCode", "INTERNAL_ERROR");
            res.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List user orders", description = "Retrieve all orders for a specific user by userId (customer, admin, or employee)", responses = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or missing userId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> listUserOrders(@RequestParam(name = "userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", false);
                res.put("message", "User ID is required as a request parameter");
                res.put("errorCode", "VALIDATION_ERROR");
                res.put("details", "Missing userId parameter");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
            }
            UUID uuid = UUID.fromString(userId);
            List<Order> orders = orderService.getOrdersForUser(uuid);
            List<OrderResponseDTO> dtoList = orders.stream().map(this::toDto).toList();
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("data", dtoList);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "Invalid userId format");
            res.put("errorCode", "VALIDATION_ERROR");
            res.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            log.error("Failed to fetch orders", e);
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "An unexpected error occurred while fetching orders.");
            res.put("errorCode", "INTERNAL_ERROR");
            res.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Create a new order", description = "Create a new order with items and shipping information", responses = {
            @ApiResponse(responseCode = "201", description = "Order created successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderDTO createOrderDTO) {
        try {
            log.info("Creating new order");
            if (createOrderDTO.getUserId() == null || createOrderDTO.getUserId().isBlank()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User ID is required in the request body");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Missing userId in request body");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            UUID userId = java.util.UUID.fromString(createOrderDTO.getUserId());
            Order order = orderService.createOrder(userId, createOrderDTO);
            OrderResponseDTO orderResponse = toDto(order);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully");
            response.put("data", orderResponse);

            log.info("Order created successfully with ID: {}", order.getOrderId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for order creation: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while creating order.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/number/{orderNumber}")
    // @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Get order by order number", description = "Retrieve an order by its order number", responses = {
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber,
            @RequestParam(name = "userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User ID is required as a request parameter");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Missing userId parameter");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            UUID uuid = UUID.fromString(userId);
            Order order = orderService.getOrderByNumber(uuid, orderNumber);
            OrderResponseDTO orderResponse = toDto(order);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orderResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid userId format");
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
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

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Get order by orderId and userId", description = "Retrieve an order by its orderId and userId (customer, admin, or employee)", responses = {
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or missing userId"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getOrder(@RequestParam(name = "userId") String userId,
            @PathVariable Long orderId) {
        try {
            if (userId == null || userId.isBlank()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User ID is required as a request parameter");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Missing userId parameter");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            UUID uuid = UUID.fromString(userId);
            Order order = orderService.getOrderByIdForUser(uuid, orderId);
            return ResponseEntity.ok(Map.of("success", true, "data", toDto(order)));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid userId format");
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Failed to fetch order", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while fetching order.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Cancel an order", description = "Cancel an existing order", responses = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, @RequestParam(name = "userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User ID is required as a request parameter");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Missing userId parameter");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            UUID uuid = UUID.fromString(userId);
            Order order = orderService.cancelOrder(uuid, orderId);
            OrderResponseDTO orderResponse = toDto(order);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order cancelled successfully");
            response.put("data", orderResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (EntityNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while cancelling order.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{orderId}/status")
    // @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Update order status", description = "Update the status of an order", responses = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

            Order order = orderService.updateOrderStatus(orderId, status);
            OrderResponseDTO orderResponse = toDto(order);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated successfully");
            response.put("data", orderResponse);

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

    @GetMapping("/{orderId}/tracking")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Get order tracking", description = "Get tracking information for an order", responses = {
            @ApiResponse(responseCode = "200", description = "Tracking information retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getOrderTracking(@PathVariable Long orderId,
            @RequestParam(name = "userId") String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User ID is required as a request parameter");
                response.put("errorCode", "VALIDATION_ERROR");
                response.put("details", "Missing userId parameter");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            UUID uuid = UUID.fromString(userId);
            Map<String, Object> trackingInfo = orderService.getOrderTracking(uuid, orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", trackingInfo);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (EntityNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error getting order tracking: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting order tracking.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private OrderResponseDTO toDto(Order order) {
        OrderInfo info = order.getOrderInfo();
        OrderAddress addr = order.getOrderAddress();
        OrderTransaction tx = order.getOrderTransaction();

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getOrderId() != null ? order.getOrderId().toString() : null);
        dto.setUserId(
                order.getUser() != null && order.getUser().getId() != null ? order.getUser().getId().toString() : null);
        dto.setOrderNumber(order.getOrderCode());
        dto.setStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        if (info != null) {
            dto.setSubtotal(info.getTotalAmount());
            dto.setTax(info.getTaxAmount());
            dto.setShipping(info.getShippingCost());
            dto.setDiscount(info.getDiscountAmount());
            dto.setTotal(info.getFinalAmount());
            dto.setNotes(info.getNotes());
        }

        if (addr != null) {
            OrderAddressDTO ad = new OrderAddressDTO();
            ad.setId(addr.getOrderAddressId() != null ? addr.getOrderAddressId().toString() : null);
            ad.setStreet(addr.getStreet());
            ad.setZipCode(addr.getZipcode());
            ad.setCountry(addr.getCountry());

            if (addr.getRegions() != null && !addr.getRegions().isEmpty()) {
                String[] regions = addr.getRegions().split(",");
                if (regions.length >= 2) {
                    ad.setCity(regions[0].trim());
                    ad.setState(regions[1].trim());
                } else if (regions.length == 1) {
                    ad.setCity(regions[0].trim());
                    ad.setState("");
                }
            }
        }
        return dto;
    }
}