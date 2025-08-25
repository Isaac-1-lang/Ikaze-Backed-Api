package com.ecommerce.controller;

import com.ecommerce.service.OrderAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Analytics", description = "APIs for order analytics and reporting")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class OrderAnalyticsController {

    private final OrderAnalyticsService orderAnalyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard analytics", description = "Get key metrics for admin dashboard")
    public ResponseEntity<?> getDashboardAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            // Basic metrics
            analytics.put("totalOrders", orderAnalyticsService.getTotalOrdersCount());
            analytics.put("totalRevenue", orderAnalyticsService.getTotalRevenue());
            analytics.put("averageOrderValue", orderAnalyticsService.getAverageOrderValue());
            analytics.put("ordersByStatus", orderAnalyticsService.getOrdersCountByStatus());
            
            // Customer stats
            analytics.put("customerStats", orderAnalyticsService.getCustomerOrderStats());
            
            // Delivery performance
            analytics.put("deliveryMetrics", orderAnalyticsService.getDeliveryPerformanceMetrics());
            
            // Top selling products (last 10)
            analytics.put("topSellingProducts", orderAnalyticsService.getTopSellingProducts(10));
            
            // Revenue trend (last 30 days)
            analytics.put("revenueTrend", orderAnalyticsService.getRevenueTrend(30));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analytics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching dashboard analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch analytics"
            ));
        }
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue analytics", description = "Get revenue-related analytics")
    public ResponseEntity<?> getRevenueAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            if (startDate != null && endDate != null) {
                analytics.put("periodRevenue", orderAnalyticsService.getRevenueForPeriod(startDate, endDate));
                analytics.put("ordersInPeriod", orderAnalyticsService.getOrdersByDateRange(startDate, endDate));
            }
            
            analytics.put("totalRevenue", orderAnalyticsService.getTotalRevenue());
            analytics.put("averageOrderValue", orderAnalyticsService.getAverageOrderValue());
            analytics.put("revenueTrend30Days", orderAnalyticsService.getRevenueTrend(30));
            analytics.put("revenueTrend7Days", orderAnalyticsService.getRevenueTrend(7));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analytics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching revenue analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch revenue analytics"
            ));
        }
    }

    @GetMapping("/products")
    @Operation(summary = "Get product analytics", description = "Get product-related analytics")
    public ResponseEntity<?> getProductAnalytics(@RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            analytics.put("topSellingProducts", orderAnalyticsService.getTopSellingProducts(limit));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analytics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching product analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch product analytics"
            ));
        }
    }

    @GetMapping("/customers")
    @Operation(summary = "Get customer analytics", description = "Get customer-related analytics")
    public ResponseEntity<?> getCustomerAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            analytics.put("customerStats", orderAnalyticsService.getCustomerOrderStats());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analytics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching customer analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch customer analytics"
            ));
        }
    }

    @GetMapping("/delivery")
    @Operation(summary = "Get delivery analytics", description = "Get delivery performance analytics")
    public ResponseEntity<?> getDeliveryAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            analytics.put("deliveryMetrics", orderAnalyticsService.getDeliveryPerformanceMetrics());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analytics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching delivery analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch delivery analytics"
            ));
        }
    }

    @GetMapping("/trends")
    @Operation(summary = "Get trend analytics", description = "Get trend-related analytics")
    public ResponseEntity<?> getTrendAnalytics(@RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            analytics.put("revenueTrend", orderAnalyticsService.getRevenueTrend(days));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analytics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching trend analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch trend analytics"
            ));
        }
    }
}
