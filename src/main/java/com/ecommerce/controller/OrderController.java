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

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ResponseEntity<?> listUserOrders(@RequestParam(name = "userId", required = false) String userId) {
        try {
            UUID effectiveUserId = resolveUserId(userId);
            if (effectiveUserId == null) {
                // Return empty list for invalid/missing userId to avoid 500s during dev/hardcoded logins
                Map<String,Object> res = new HashMap<>();
                res.put("success", true);
                res.put("data", List.of());
                return ResponseEntity.ok(res);
            }
            List<Order> orders = orderService.getOrdersForUser(effectiveUserId);
            List<OrderResponseDTO> dtoList = orders.stream().map(this::toDto).toList();
            Map<String,Object> res = new HashMap<>();
            res.put("success", true);
            res.put("data", dtoList);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Failed to fetch orders", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch orders"
            ));
        }
    }

    @PostMapping
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
            
            // Get current user ID from authentication context
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }
            
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
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create order");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Get order by order number", description = "Retrieve an order by its order number", responses = {
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }
            
            Order order = orderService.getOrderByNumber(userId, orderNumber);
            OrderResponseDTO orderResponse = toDto(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orderResponse);
            
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

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    public ResponseEntity<?> getOrder(@RequestParam(name = "userId", required = false) String userId, @PathVariable Long orderId) {
        try {
            UUID effectiveUserId = resolveUserId(userId);
            if (effectiveUserId == null) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Order not found"));
            }
            Order order = orderService.getOrderByIdForUser(effectiveUserId, orderId);
            return ResponseEntity.ok(Map.of("success", true, "data", toDto(order)));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Order not found"));
        } catch (Exception e) {
            log.error("Failed to fetch order", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Failed to fetch order"));
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
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }
            
            Order order = orderService.cancelOrder(userId, orderId);
            OrderResponseDTO orderResponse = toDto(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order cancelled successfully");
            response.put("data", orderResponse);
            
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

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
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
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Status is required"
                ));
            }
            
            Order order = orderService.updateOrderStatus(orderId, status);
            OrderResponseDTO orderResponse = toDto(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated successfully");
            response.put("data", orderResponse);
            
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

    @GetMapping("/{orderId}/tracking")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Get order tracking", description = "Get tracking information for an order", responses = {
            @ApiResponse(responseCode = "200", description = "Tracking information retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getOrderTracking(@PathVariable Long orderId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "User not authenticated"
                ));
            }
            
            Map<String, Object> trackingInfo = orderService.getOrderTracking(userId, orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", trackingInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Order not found"
            ));
        } catch (Exception e) {
            log.error("Error getting order tracking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to get order tracking"
            ));
        }
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.ecommerce.entity.User u && u.getId() != null) {
                return u.getId();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private UUID resolveUserId(String userIdParam) {
        try {
            if (userIdParam != null && !userIdParam.isBlank()) {
                return java.util.UUID.fromString(userIdParam);
            }
        } catch (IllegalArgumentException ignored) { }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.ecommerce.entity.User u && u.getId() != null) {
                return u.getId();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private OrderResponseDTO toDto(Order order) {
        OrderInfo info = order.getOrderInfo();
        OrderAddress addr = order.getOrderAddress();
        OrderTransaction tx = order.getOrderTransaction();

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getOrderId() != null ? order.getOrderId().toString() : null);
        dto.setUserId(order.getUser() != null && order.getUser().getId() != null ? order.getUser().getId().toString() : null);
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
            
            // Extract city and state from regions field
            if (addr.getRegions() != null && !addr.getRegions().isEmpty()) {
                String[] regions = addr.getRegions().split(",");
                if (regions.length >= 2) {
                    ad.setCity(regions[0].trim());
                    ad.setState(regions[1].trim());
                } else if (regions.length == 1) {
                    ad.setCity(regions[0].trim());
                    ad.setState("");
                }
            } else {
                ad.setCity("");
                ad.setState("");
            }
            
            // Get phone from customer info
            if (order.getOrderCustomerInfo() != null) {
                ad.setPhone(order.getOrderCustomerInfo().getPhoneNumber());
            } else {
                ad.setPhone("");
            }
            
            dto.setShippingAddress(ad);
            dto.setBillingAddress(ad);
        }

        if (tx != null) {
            dto.setPaymentMethod(tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null);
            dto.setPaymentStatus(tx.getStatus() != null ? tx.getStatus().name() : null);
        }

        List<OrderItemDTO> items = (order.getOrderItems() != null ? order.getOrderItems() : Collections.<OrderItem>emptyList())
            .stream().map(this::toItemDto).toList();
        dto.setItems(items);

        // estimatedDelivery / trackingNumber not present in current schema
        dto.setEstimatedDelivery(null);
        dto.setTrackingNumber(null);
        return dto;
    }

    private OrderItemDTO toItemDto(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getOrderItemId() != null ? item.getOrderItemId().toString() : null);
        ProductVariant variant = item.getProductVariant();
        if (variant != null) {
            Product product = variant.getProduct();
            dto.setProductId(product != null && product.getProductId() != null ? product.getProductId().toString() : null);
            SimpleProductDTO sp = new SimpleProductDTO();
            if (product != null) {
                sp.setProductId(product.getProductId() != null ? product.getProductId().toString() : null);
                sp.setName(product.getProductName());
                sp.setDescription(product.getDescription());
                sp.setPrice(product.getPrice() != null ? product.getPrice().doubleValue() : null);
                // Collect primary product image and any others
                java.util.List<String> imgs = new java.util.ArrayList<>();
                // Avoid lazy loading issues for images here; set empty list or main image elsewhere if needed
                sp.setImages(imgs.toArray(new String[0]));
            }
            dto.setProduct(sp);
        }
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        BigDecimal price = item.getPrice();
        Integer qty = item.getQuantity();
        BigDecimal total = BigDecimal.ZERO;
        if (price != null && qty != null) {
            total = price.multiply(BigDecimal.valueOf(qty.longValue()));
        } else if (price != null) {
            total = price;
        }
        dto.setTotalPrice(total);
        return dto;
    }
}