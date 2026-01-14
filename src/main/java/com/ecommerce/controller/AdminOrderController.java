package com.ecommerce.controller;

import com.ecommerce.dto.AdminOrderDTO;
import com.ecommerce.dto.OrderSearchDTO;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ShopAuthorizationService;
import com.ecommerce.ServiceImpl.CustomUserDetails;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Order Management", description = "APIs for administrators to manage all orders")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'VENDOR')")
public class AdminOrderController {

    private final OrderService orderService;
    private final ShopAuthorizationService shopAuthorizationService;

    @GetMapping
    @Operation(summary = "Get all orders with pagination", description = "Retrieve all orders in the system with pagination support")
    public ResponseEntity<?> getAllOrders(
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "15") @RequestParam(defaultValue = "15") int size,
            @Parameter(description = "Sort by field", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Shop ID to filter orders by shop") @RequestParam(required = false) String shopId,
            @Parameter(description = "Order status filter") @RequestParam(required = false) String orderStatus,
            @Parameter(description = "Payment status filter") @RequestParam(required = false) String paymentStatus,
            @Parameter(description = "City filter") @RequestParam(required = false) String city,
            @Parameter(description = "Country filter") @RequestParam(required = false) String country,
            @Parameter(description = "Start date filter (ISO format)") @RequestParam(required = false) String startDate,
            @Parameter(description = "End date filter (ISO format)") @RequestParam(required = false) String endDate,
            @Parameter(description = "Search keyword filter") @RequestParam(required = false) String searchKeyword) {
        try {
            UUID shopUuid = null;
            if (shopId != null && !shopId.trim().isEmpty()) {
                try {
                    shopUuid = UUID.fromString(shopId);
                    UUID currentUserId = getCurrentUserId();
                    if (currentUserId != null) {
                        shopAuthorizationService.assertCanManageShop(currentUserId, shopUuid);
                    }
                } catch (IllegalArgumentException e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Invalid shopId format");
                    response.put("errorCode", "VALIDATION_ERROR");
                    return ResponseEntity.badRequest().body(response);
                } catch (com.ecommerce.Exception.CustomException e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Access denied to this shop");
                    response.put("errorCode", "ACCESS_DENIED");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            }

            // Create sort object
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            // Create pageable object
            Pageable pageable = PageRequest.of(page, size, sort);

            // Create search request DTO
            OrderSearchDTO.OrderSearchDTOBuilder searchBuilder = OrderSearchDTO.builder()
                    .shopId(shopUuid)
                    .orderStatus(orderStatus)
                    .paymentStatus(paymentStatus)
                    .city(city)
                    .country(country)
                    .searchKeyword(searchKeyword)
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDir);

            // Parse dates if provided
            try {
                if (startDate != null && !startDate.trim().isEmpty()) {
                    searchBuilder.startDate(java.time.OffsetDateTime.parse(startDate).toLocalDateTime());
                }
                if (endDate != null && !endDate.trim().isEmpty()) {
                    searchBuilder.endDate(java.time.OffsetDateTime.parse(endDate).toLocalDateTime());
                }
            } catch (Exception e) {
                log.warn("Error parsing date in getAllOrders: {}", e.getMessage());
                // Fallback to simple date parse if OffsetDateTime fails
                try {
                    if (startDate != null && !startDate.trim().isEmpty()) {
                        searchBuilder.startDate(java.time.LocalDateTime.parse(startDate));
                    }
                    if (endDate != null && !endDate.trim().isEmpty()) {
                        searchBuilder.endDate(java.time.LocalDateTime.parse(endDate));
                    }
                } catch (Exception e2) {
                    log.error("Failed to parse date after fallback: {}", e2.getMessage());
                }
            }

            // Get paginated orders using search method
            OrderSearchDTO searchRequest = searchBuilder.build();
            Page<AdminOrderDTO> ordersPage = orderService.searchOrders(searchRequest, pageable);
            java.math.BigDecimal totalPaidAmount = orderService.calculateTotalAmount(searchRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ordersPage.getContent());
            response.put("totalPaidAmount", totalPaidAmount);
            response.put("pagination", Map.of(
                    "currentPage", ordersPage.getNumber(),
                    "totalPages", ordersPage.getTotalPages(),
                    "totalElements", ordersPage.getTotalElements(),
                    "pageSize", ordersPage.getSize(),
                    "hasNext", ordersPage.hasNext(),
                    "hasPrevious", ordersPage.hasPrevious(),
                    "isFirst", ordersPage.isFirst(),
                    "isLast", ordersPage.isLast()));

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
    public ResponseEntity<?> getOrderById(
            @PathVariable Long orderId,
            @RequestParam(required = false) String shopId) {
        try {
            UUID shopUuid = null;
            if (shopId != null && !shopId.trim().isEmpty()) {
                shopUuid = UUID.fromString(shopId);
                // Validate shop access
                UUID currentUserId = getCurrentUserId();
                if (currentUserId != null) {
                    shopAuthorizationService.assertCanManageShop(currentUserId, shopUuid);
                }
            }

            AdminOrderDTO order = orderService.getAdminOrderById(orderId, shopUuid);

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
    public ResponseEntity<?> getOrderByNumber(
            @PathVariable String orderNumber,
            @RequestParam(required = false) String shopId) {
        try {
            UUID shopUuid = null;
            if (shopId != null && !shopId.trim().isEmpty()) {
                shopUuid = UUID.fromString(shopId);
                // Validate shop access
                UUID currentUserId = getCurrentUserId();
                if (currentUserId != null) {
                    shopAuthorizationService.assertCanManageShop(currentUserId, shopUuid);
                }
            }

            AdminOrderDTO order = orderService.getAdminOrderByNumber(orderNumber, shopUuid);

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

            var shopOrder = orderService.updateShopOrderStatus(orderId, status);
            // Returning the updated ShopOrder DTO would be ideal, or the parent Order.
            // Since this is AdminOrderController, assuming we want to see the specific shop
            // order update?
            // Or the whole order? Let's return the whole order for consistency with
            // existing code,
            // or just the ShopOrderDTO if possible.
            // Existing code: AdminOrderDTO adminOrder =
            // orderService.getAdminOrderById(order.getOrderId());
            // Now shopOrder.getOrder().getId() gives us the parent order ID.
            AdminOrderDTO adminOrder = orderService.getAdminOrderById(shopOrder.getOrder().getId());

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

            var shopOrder = orderService.updateShopOrderTracking(orderId, trackingNumber, estimatedDelivery);
            AdminOrderDTO adminOrder = orderService.getAdminOrderById(shopOrder.getOrder().getId());

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

            // Validate shop access if shopId is provided
            if (searchRequest.getShopId() != null) {
                UUID currentUserId = getCurrentUserId();
                if (currentUserId != null) {
                    try {
                        shopAuthorizationService.assertCanManageShop(currentUserId, searchRequest.getShopId());
                    } catch (com.ecommerce.Exception.CustomException e) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Access denied to this shop");
                        response.put("errorCode", "ACCESS_DENIED");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
                }
            }

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
                    "isLast", ordersPage.isLast()));

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

    @GetMapping("/count/pending")
    @Operation(summary = "Get pending orders count", description = "Get count of orders with PROCESSING status and no delivery group assigned")
    public ResponseEntity<?> getPendingOrdersCount() {
        try {
            long count = orderService.countProcessingOrdersWithoutDeliveryGroup();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting pending orders count: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get pending orders count");
            response.put("count", 0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof CustomUserDetails customUserDetails) {
                return customUserDetails.getUserId();
            }

            return null;
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
            return null;
        }
    }
}
