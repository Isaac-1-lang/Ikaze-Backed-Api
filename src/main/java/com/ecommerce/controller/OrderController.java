package com.ecommerce.controller;

import java.util.stream.Collectors;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderInfo;
import com.ecommerce.entity.OrderAddress;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.dto.OrderItemDTO;
import com.ecommerce.dto.OrderAddressDTO;
import com.ecommerce.dto.OrderCustomerInfoDTO;
import com.ecommerce.dto.OrderTransactionDTO;
import com.ecommerce.dto.SimpleProductDTO;
import com.ecommerce.dto.CreateOrderDTO;
import com.ecommerce.dto.OrderTrackingRequestDTO;
import com.ecommerce.dto.OrderTrackingResponseDTO;
import com.ecommerce.dto.OrderSummaryDTO;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.OrderTrackingService;
import java.time.LocalDateTime;
import com.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ecommerce.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Get all orders", description = "Retrieve all orders (admin/employee only)", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class)))
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
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','CUSTOMER')")
    @Operation(summary = "Get orders by userId", description = "Retrieve all orders for a specific user (admin/employee only)", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class)))
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @GetMapping("/id/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','CUSTOMER','DELIVERY_AGENT')")
    @Operation(summary = "Get order by orderId", description = "Retrieve an order by its orderId (protected endpoint)", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", false);
                res.put("message", "User not authenticated");
                res.put("errorCode", "UNAUTHORIZED");
                res.put("details", "User ID could not be extracted from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdminOrEmployee = auth.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN") ||
                            grantedAuthority.getAuthority().equals("ROLE_EMPLOYEE"));

            Order order;
            if (isAdminOrEmployee) {
                // Admins and employees can view any order
                order = orderService.getOrderById(orderId);
            } else {
                // Customers can only view their own orders
                order = ((com.ecommerce.ServiceImpl.OrderServiceImpl) orderService)
                        .getOrderByIdWithUserValidation(orderId, userId);
            }

            if (order == null) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", false);
                res.put("message", "Order not found");
                res.put("errorCode", "NOT_FOUND");
                res.put("details", "Order with id " + orderId + " not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
            }
            return ResponseEntity.ok(Map.of("success", true, "data", toDto(order)));
        } catch (SecurityException e) {
            log.warn("Access denied for order {}: {}", orderId, e.getMessage());
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("message", "Access denied");
            res.put("errorCode", "ACCESS_DENIED");
            res.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
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
    private final OrderTrackingService orderTrackingService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "List user orders", description = "Retrieve all orders for the authenticated user", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> listUserOrders() {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", false);
                res.put("message", "User not authenticated");
                res.put("errorCode", "UNAUTHORIZED");
                res.put("details", "User ID could not be extracted from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
            }

            List<Order> orders = orderService.getOrdersForUser(userId);
            List<OrderResponseDTO> dtoList = orders.stream().map(this::toDto).toList();
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("data", dtoList);
            return ResponseEntity.ok(res);
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

    /**
     * Get specific order by ID using token authentication (public endpoint)
     */
    @GetMapping("/track/order/{orderId}")
    @Operation(summary = "Get order by token and ID", description = "Get specific order details using tracking token and order ID", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - order does not belong to email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderByIdWithToken(
            @PathVariable Long orderId,
            @RequestParam String token) {
        try {
            log.info("Fetching order {} with token authentication", orderId);
            
            // Validate token first
            if (!orderTrackingService.isValidToken(token)) {
                log.warn("Invalid or expired token provided for order {}", orderId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or expired token"));
            }
            
            // Get the email associated with this token
            String email = orderTrackingService.getEmailFromToken(token);
            if (email == null) {
                log.warn("No email found for token when accessing order {}", orderId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid token"));
            }
            
            // Get the order
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                log.warn("Order {} not found", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Order not found"));
            }
            
            // Verify the order belongs to the email associated with the token
            String orderEmail = null;
            if (order.getOrderCustomerInfo() != null) {
                orderEmail = order.getOrderCustomerInfo().getEmail();
            } else if (order.getUser() != null) {
                orderEmail = order.getUser().getUserEmail();
            }
            
            if (orderEmail == null || !orderEmail.equalsIgnoreCase(email)) {
                log.warn("Order {} does not belong to email {} (token owner)", orderId, email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied: Order does not belong to your email"));
            }
            
            // Convert to DTO and return
            OrderResponseDTO dto = toDto(order);
            log.info("Successfully retrieved order {} for email {}", orderId, email);
            
            return ResponseEntity.ok(ApiResponse.success(dto));
            
        } catch (Exception e) {
            log.error("Error fetching order {} with token", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch order details"));
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Create a new order", description = "Create a new order with items and shipping information", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
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
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE','DELIVERY_AGENT')")
    @Operation(summary = "Get order by order number", description = "Retrieve an order by its order number (protected endpoint)", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            // Get current authenticated user
            UUID userId = getCurrentUserId();
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not authenticated");
                response.put("errorCode", "UNAUTHORIZED");
                response.put("details", "User ID could not be extracted from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Order order = orderService.getOrderByNumber(userId, orderNumber);
            OrderResponseDTO orderResponse = toDto(order);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orderResponse);

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

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Get order by orderId", description = "Retrieve an order by its orderId for the authenticated user", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not authenticated");
                response.put("errorCode", "UNAUTHORIZED");
                response.put("details", "User ID could not be extracted from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Order order = orderService.getOrderByIdForUser(userId, orderId);
            return ResponseEntity.ok(Map.of("success", true, "data", toDto(order)));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Order not found");
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(404).body(response);
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tracking information retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
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
        dto.setId(order.getOrderId());
        dto.setUserId(
                order.getUser() != null && order.getUser().getId() != null ? order.getUser().getId().toString() : null);
        dto.setOrderNumber(order.getOrderCode());
        dto.setPickupToken(order.getPickupToken());
        dto.setPickupTokenUsed(order.getPickupTokenUsed());
        dto.setStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Set customer info - prioritize OrderCustomerInfo for guest orders, fallback to User for registered users
        if (order.getOrderCustomerInfo() != null) {
            OrderResponseDTO.CustomerInfo customerInfo = new OrderResponseDTO.CustomerInfo();
            customerInfo.setName(order.getOrderCustomerInfo().getFullName());
            customerInfo.setEmail(order.getOrderCustomerInfo().getEmail());
            customerInfo.setPhone(order.getOrderCustomerInfo().getPhoneNumber());
            dto.setCustomerInfo(customerInfo);
        } else if (order.getUser() != null) {
            OrderResponseDTO.CustomerInfo customerInfo = new OrderResponseDTO.CustomerInfo();
            customerInfo.setName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
            customerInfo.setEmail(order.getUser().getUserEmail());
            customerInfo.setPhone(order.getUser().getPhoneNumber());
            dto.setCustomerInfo(customerInfo);
        }

        // Set order info
        if (info != null) {
            dto.setSubtotal(info.getTotalAmount());
            dto.setTax(info.getTaxAmount());
            dto.setShipping(info.getShippingCost());
            dto.setDiscount(info.getDiscountAmount());
            dto.setTotal(info.getFinalAmount());
            dto.setNotes(info.getNotes());
        }

        // Set shipping address with coordinates - prioritize OrderCustomerInfo, fallback to OrderAddress
        OrderResponseDTO.ShippingAddress shippingAddress = new OrderResponseDTO.ShippingAddress();
        boolean hasShippingAddress = false;

        // First try to get address from OrderCustomerInfo (for guest orders)
        if (order.getOrderCustomerInfo() != null) {
            shippingAddress.setStreet(order.getOrderCustomerInfo().getStreetAddress());
            shippingAddress.setCity(order.getOrderCustomerInfo().getCity());
            shippingAddress.setState(order.getOrderCustomerInfo().getState());
            shippingAddress.setCountry(order.getOrderCustomerInfo().getCountry());
            hasShippingAddress = true;
        }

        // Add coordinates from OrderAddress if available
        if (addr != null) {
            shippingAddress.setLatitude(addr.getLatitude());
            shippingAddress.setLongitude(addr.getLongitude());
            
            // If we don't have address from OrderCustomerInfo, use OrderAddress
            if (!hasShippingAddress) {
                shippingAddress.setStreet(addr.getStreet());
                shippingAddress.setCountry(addr.getCountry());

                if (addr.getRegions() != null && !addr.getRegions().isEmpty()) {
                    String[] regions = addr.getRegions().split(",");
                    if (regions.length >= 2) {
                        shippingAddress.setCity(regions[0].trim());
                        shippingAddress.setState(regions[1].trim());
                    } else if (regions.length == 1) {
                        shippingAddress.setCity(regions[0].trim());
                        shippingAddress.setState("");
                    }
                }
                hasShippingAddress = true;
            }
        }

        if (hasShippingAddress) {
            dto.setShippingAddress(shippingAddress);
        }

        // Set payment information
        if (tx != null) {
            dto.setPaymentMethod(tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null);
            dto.setPaymentStatus(tx.getStatus() != null ? tx.getStatus().name() : null);
        }

        if (tx != null) {
            OrderTransactionDTO transactionDTO = new OrderTransactionDTO();
            transactionDTO.setOrderTransactionId(
                    tx.getOrderTransactionId() != null ? tx.getOrderTransactionId().toString() : null);
            transactionDTO.setTransactionRef(tx.getTransactionRef());
            transactionDTO.setPaymentMethod(tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null);
            transactionDTO.setStatus(tx.getStatus() != null ? tx.getStatus().name() : null);
            transactionDTO.setOrderAmount(tx.getOrderAmount());
            transactionDTO.setPointsUsed(tx.getPointsUsed());
            transactionDTO.setPointsValue(tx.getPointsValue());
            transactionDTO.setStripePaymentIntentId(tx.getStripePaymentIntentId());
            transactionDTO.setReceiptUrl(tx.getReceiptUrl());
            transactionDTO.setPaymentDate(tx.getPaymentDate());
            transactionDTO.setCreatedAt(tx.getCreatedAt());
            transactionDTO.setUpdatedAt(tx.getUpdatedAt());
            dto.setTransaction(transactionDTO);
        }

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<OrderResponseDTO.OrderItem> itemDTOs = order.getOrderItems().stream().map(this::mapOrderItemToResponseDTO).toList();
            dto.setItems(itemDTOs);
        }

        return dto;
    }

    private OrderItemDTO mapOrderItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getOrderItemId() != null ? item.getOrderItemId().toString() : null);
        dto.setProductId(item.getProduct() != null ? item.getProduct().getProductId().toString() : null);
        dto.setVariantId(item.getProductVariant() != null ? item.getProductVariant().getId().toString() : null);
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setTotalPrice(item.getSubtotal());

        // Set product information
        if (item.getProduct() != null) {
            SimpleProductDTO productDto = new SimpleProductDTO();
            productDto.setProductId(item.getProduct().getProductId().toString());
            productDto.setName(item.getProduct().getProductName());
            productDto.setDescription(item.getProduct().getDescription());
            productDto.setPrice(item.getProduct().getDiscountedPrice().doubleValue());

            // Set product images
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                productDto.setImages(item.getProduct().getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .toArray(String[]::new));
            }
            dto.setProduct(productDto);
        }

        // Set variant information if it's a variant-based item
        if (item.getProductVariant() != null) {
            SimpleProductDTO variantDto = new SimpleProductDTO();
            variantDto.setProductId(item.getProductVariant().getId().toString());
            variantDto.setName(item.getProductVariant().getVariantName());
            variantDto.setPrice(item.getProductVariant().getPrice().doubleValue());

            // Set variant images
            if (item.getProductVariant().getImages() != null && !item.getProductVariant().getImages().isEmpty()) {
                variantDto.setImages(item.getProductVariant().getImages().stream()
                        .map(img -> img.getImageUrl())
                        .toArray(String[]::new));
            }
            dto.setVariant(variantDto);
        }
        calculateReturnEligibility(dto, item);

        return dto;
    }

    private OrderResponseDTO.OrderItem mapOrderItemToResponseDTO(OrderItem item) {
        OrderResponseDTO.OrderItem dto = new OrderResponseDTO.OrderItem();
        dto.setId(item.getOrderItemId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setTotalPrice(item.getSubtotal());
        
        // Set product info
        if (item.getProduct() != null) {
            OrderResponseDTO.Product product = new OrderResponseDTO.Product();
            product.setId(item.getProduct().getProductId());
            product.setName(item.getProduct().getProductName());
            
            // Add product images if available
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                List<String> imageUrls = item.getProduct().getImages().stream()
                    .sorted((img1, img2) -> {
                        if (img1.isPrimary() && !img2.isPrimary()) return -1;
                        if (!img1.isPrimary() && img2.isPrimary()) return 1;
                        int sortOrder1 = img1.getSortOrder() != null ? img1.getSortOrder() : 0;
                        int sortOrder2 = img2.getSortOrder() != null ? img2.getSortOrder() : 0;
                        return Integer.compare(sortOrder1, sortOrder2);
                    })
                    .map(img -> img.getImageUrl())
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .collect(Collectors.toList());
                product.setImages(imageUrls);
            }
            
            dto.setProduct(product);
        }
        
        // Set variant info if available
        if (item.getProductVariant() != null) {
            OrderResponseDTO.Variant variant = new OrderResponseDTO.Variant();
            variant.setId(item.getProductVariant().getId());
            variant.setName(item.getProductVariant().getVariantName());
            
            // Add variant images if available
            if (item.getProductVariant().getImages() != null && !item.getProductVariant().getImages().isEmpty()) {
                List<String> variantImageUrls = item.getProductVariant().getImages().stream()
                    .sorted((img1, img2) -> {
                        if (img1.isPrimary() && !img2.isPrimary()) return -1;
                        if (!img1.isPrimary() && img2.isPrimary()) return 1;
                        int sortOrder1 = img1.getSortOrder() != null ? img1.getSortOrder() : 0;
                        int sortOrder2 = img2.getSortOrder() != null ? img2.getSortOrder() : 0;
                        return Integer.compare(sortOrder1, sortOrder2);
                    })
                    .map(img -> img.getImageUrl())
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .collect(Collectors.toList());
                variant.setImages(variantImageUrls);
            }
            
            dto.setVariant(variant);
        }
        
        // Set return eligibility (placeholder)
        dto.setReturnEligible(true);
        dto.setMaxReturnDays(30);
        dto.setDaysRemainingForReturn(25);
        
        return dto;
    }

    private void calculateReturnEligibility(OrderItemDTO dto, OrderItem item) {
        Order order = item.getOrder();
        Integer defaultReturnDays = 15;

        // Get return days from product, with null safety
        if (item.getProduct() != null) {
            Integer productReturnDays = item.getProduct().getMaximumDaysForReturn();
            if (productReturnDays != null && productReturnDays > 0) {
                defaultReturnDays = productReturnDays;
            }
        }

        // Get return days from product variant, with null safety
        if (item.getProductVariant() != null) {
            Product effectiveProduct = item.getEffectiveProduct();
            if (effectiveProduct != null) {
                Integer variantReturnDays = effectiveProduct.getMaximumDaysForReturn();
                if (variantReturnDays != null && variantReturnDays > 0) {
                    defaultReturnDays = variantReturnDays;
                }
            }
        }

        // Ensure we have a valid return days value
        if (defaultReturnDays == null || defaultReturnDays <= 0) {
            defaultReturnDays = 15; // Fallback to 15 days
        }

        dto.setMaxReturnDays(defaultReturnDays);
        dto.setDeliveredAt(order.getDeliveredAt());

        if (order.getOrderStatus() == Order.OrderStatus.DELIVERED && order.getDeliveredAt() != null) {
            LocalDateTime deliveredAt = order.getDeliveredAt();
            LocalDateTime returnDeadline = deliveredAt.plusDays(defaultReturnDays);
            LocalDateTime now = LocalDateTime.now();

            boolean isEligible = now.isBefore(returnDeadline);
            dto.setIsReturnEligible(isEligible);

            if (isEligible) {
                long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, returnDeadline);
                dto.setDaysRemainingForReturn((int) Math.max(0, daysRemaining));
            } else {
                dto.setDaysRemainingForReturn(0);
            }
        } else {
            // Order not delivered yet, not eligible for return
            dto.setIsReturnEligible(false);
            dto.setDaysRemainingForReturn(0);
        }
    }

    /**
     * Request secure tracking access via email (public endpoint)
     */
    @PostMapping("/track/request-access")
    @Operation(summary = "Request tracking access", description = "Request secure tracking access via email verification", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Access request processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> requestTrackingAccess(@Valid @RequestBody OrderTrackingRequestDTO request) {
        try {
            OrderTrackingResponseDTO response = orderTrackingService.requestTrackingAccess(request);
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", response.isSuccess());
            responseMap.put("message", response.getMessage());
            
            if (response.isSuccess()) {
                responseMap.put("expiresAt", response.getExpiresAt());
                responseMap.put("trackingUrl", response.getTrackingUrl());
            }
            
            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            log.error("Failed to process tracking access request", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to process tracking request. Please try again later.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get orders by tracking token (public endpoint)
     */
    @GetMapping("/track/orders")
    @Operation(summary = "Get orders by token", description = "Get paginated list of orders using tracking token", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getOrdersByToken(
            @RequestParam String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderSummaryDTO> orders = orderTrackingService.getOrdersByToken(token, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders.getContent());
            response.put("totalElements", orders.getTotalElements());
            response.put("totalPages", orders.getTotalPages());
            response.put("currentPage", orders.getNumber());
            response.put("pageSize", orders.getSize());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            log.error("Failed to get orders by token", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve orders. Please try again later.");
            return ResponseEntity.internalServerError().body(response);
        }
    }


    /**
     * Verify delivery by pickup token (delivery agent only)
     */
    @PostMapping("/delivery/verify/{pickupToken}")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Verify delivery by pickup token", description = "Delivery agent endpoint to verify and mark order as delivered", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pickup token or order already delivered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> verifyDelivery(@PathVariable String pickupToken) {
        try {
            Order order = orderService.getOrderByPickupToken(pickupToken);
            if (order == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Order not found");
                response.put("errorCode", "NOT_FOUND");
                response.put("details", "No order found with pickup token: " + pickupToken);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            log.info("Found order: ID={}, Status={}, PickupTokenUsed={}",
                    order.getOrderId(), order.getOrderStatus(), order.getPickupTokenUsed());

            if (order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Order already delivered");
                response.put("errorCode", "ALREADY_DELIVERED");
                response.put("details", "This order has already been marked as delivered");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (Boolean.TRUE.equals(order.getPickupTokenUsed())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Pickup token already used");
                response.put("errorCode", "TOKEN_USED");
                response.put("details", "This pickup token has already been used for delivery verification");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            log.info("Updating order status to DELIVERED and marking token as used");
            order.setOrderStatus(Order.OrderStatus.DELIVERED);
            order.setPickupTokenUsed(true);
            order.setDeliveredAt(java.time.LocalDateTime.now());
            Order savedOrder = orderService.saveOrder(order);

            if (savedOrder.getReadyForDeliveryGroup() != null) {
                log.info("Checking if all orders in delivery group {} are delivered",
                        savedOrder.getReadyForDeliveryGroup().getDeliveryGroupId());

                boolean allOrdersDelivered = orderService.checkAllOrdersDeliveredInGroup(
                        savedOrder.getReadyForDeliveryGroup().getDeliveryGroupId());

                if (allOrdersDelivered) {
                    log.info("All orders in group {} are delivered, auto-finishing delivery",
                            savedOrder.getReadyForDeliveryGroup().getDeliveryGroupId());
                    orderService.autoFinishDeliveryGroup(savedOrder.getReadyForDeliveryGroup().getDeliveryGroupId());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Delivery verified successfully");
            response.put("data", Map.of(
                    "orderId", order.getOrderId(),
                    "orderCode", order.getOrderCode(),
                    "status", order.getOrderStatus().name(),
                    "pickupTokenUsed", order.getPickupTokenUsed()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to verify delivery for token: {}", pickupToken, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while verifying delivery.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }

            Object principal = auth.getPrincipal();

            // If principal is CustomUserDetails, extract email and find user
            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            // If principal is User entity
            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                return user.getId();
            }

            // If principal is UserDetails
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            // Fallback to auth name
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name).map(com.ecommerce.entity.User::getId).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get order details by order number with token validation (secure endpoint)
     */
    @GetMapping("/track/secure/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderByNumberWithToken(
            @PathVariable String orderNumber,
            @RequestParam String token) {
        try {
            OrderResponseDTO order = orderTrackingService.getOrderByNumberWithToken(orderNumber, token);
            return ResponseEntity.ok(ApiResponse.success(order, "Order details retrieved successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving order by number with token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve order details"));
        }
    }
}