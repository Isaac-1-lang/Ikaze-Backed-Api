package com.ecommerce.controller;

import com.ecommerce.dto.DashboardResponseDTO;
import com.ecommerce.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Admin/Employee dashboard metrics and alerts")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Returns totals, recent orders, and alerts. Revenue is included only for ADMIN users.")
    @ApiResponse(responseCode = "200", description = "Dashboard data fetched", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    public ResponseEntity<?> getDashboard(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            log.info("Fetching dashboard data");
            DashboardResponseDTO dto = dashboardService.getDashboardData(authorization);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid dashboard request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("INTERNAL_ERROR", "Failed to fetch dashboard data"));
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
