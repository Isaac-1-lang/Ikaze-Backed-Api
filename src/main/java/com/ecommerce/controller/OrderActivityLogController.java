package com.ecommerce.controller;

import com.ecommerce.dto.OrderActivityLogDTO;
import com.ecommerce.service.OrderActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/activity-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Activity Logs", description = "APIs for managing order activity timeline")
@SecurityRequirement(name = "bearerAuth")
public class OrderActivityLogController {

    private final OrderActivityLogService activityLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
            summary = "Get complete order timeline",
            description = "Retrieve all activities and events that happened to an order in chronological order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Timeline retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Order not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> getOrderTimeline(@PathVariable Long orderId) {
        try {
            log.info("Fetching activity timeline for order {}", orderId);
            List<OrderActivityLogDTO> timeline = activityLogService.getOrderTimeline(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("totalActivities", timeline.size());
            response.put("activities", timeline);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching timeline for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch order timeline"));
        }
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
            summary = "Get order timeline by date range",
            description = "Retrieve order activities within a specific date range",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Timeline retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid date range"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> getOrderTimelineByDateRange(
            @PathVariable Long orderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        try {
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("INVALID_DATE_RANGE", "Start date must be before end date"));
            }

            log.info("Fetching activity timeline for order {} between {} and {}", 
                    orderId, startDate, endDate);
            List<OrderActivityLogDTO> timeline = activityLogService.getOrderTimelineByDateRange(
                    orderId, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("totalActivities", timeline.size());
            response.put("activities", timeline);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching timeline for order {} in date range: {}", 
                    orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch order timeline"));
        }
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
            summary = "Get recent order activities",
            description = "Retrieve the most recent activities for an order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recent activities retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> getRecentActivities(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            log.info("Fetching {} recent activities for order {}", limit, orderId);
            List<OrderActivityLogDTO> activities = activityLogService.getRecentActivities(orderId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("limit", limit);
            response.put("activities", activities);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching recent activities for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch recent activities"));
        }
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    @Operation(
            summary = "Get activity count",
            description = "Get the total number of activities for an order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> getActivityCount(@PathVariable Long orderId) {
        try {
            long count = activityLogService.countActivities(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("totalActivities", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error counting activities for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to count activities"));
        }
    }

    @GetMapping("/public")
    @Operation(
            summary = "Get order timeline with tracking token (Public)",
            description = "Retrieve order activity timeline using tracking token for guest users",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Timeline retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Invalid tracking token"),
                    @ApiResponse(responseCode = "404", description = "Order not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> getOrderTimelineWithToken(
            @PathVariable Long orderId,
            @RequestParam String token
    ) {
        try {
            log.info("Fetching activity timeline for order {} with token", orderId);
            List<OrderActivityLogDTO> timeline = activityLogService.getOrderTimelineWithToken(orderId, token);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("totalActivities", timeline.size());
            response.put("activities", timeline);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid token for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("INVALID_TOKEN", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching timeline for order {} with token: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch order timeline"));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", errorCode);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
