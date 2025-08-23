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
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            // Backend stores regions and zipcode/country only; map best-effort
            ad.setCity(null);
            ad.setState(null);
            ad.setZipCode(addr.getZipcode());
            ad.setCountry(addr.getCountry());
            ad.setPhone(null);
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