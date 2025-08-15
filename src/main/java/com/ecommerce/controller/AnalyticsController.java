package com.ecommerce.controller;

import com.ecommerce.dto.AnalyticsRequestDTO;
import com.ecommerce.dto.AnalyticsResponseDTO;
import com.ecommerce.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "System analytics with time range filtering")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping
    @Operation(summary = "Get analytics", description = "Returns revenue (admin only), orders, customers, active products, top products, and category performance for the given date range")
    @ApiResponse(responseCode = "200", description = "Analytics data fetched", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnalyticsResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    public ResponseEntity<?> getAnalytics(
            @RequestBody AnalyticsRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            log.info("Fetching analytics: {} - {}", request.getStartDate(), request.getEndDate());
            AnalyticsResponseDTO dto = analyticsService.getAnalytics(request, authorization);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid analytics request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching analytics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("INTERNAL_ERROR", "Failed to fetch analytics data"));
        }
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("errorCode", code);
        m.put("message", message);
        m.put("timestamp", System.currentTimeMillis());
        return m;
    }
}
