package com.ecommerce.controller;

import com.ecommerce.dto.CheckoutRequest;
import com.ecommerce.dto.GuestCheckoutRequest;
import com.ecommerce.dto.CheckoutVerificationResult;
import com.ecommerce.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")

@Slf4j
@Tag(name = "Checkout Management", description = "APIs for managing checkout and payment processes")
@SecurityRequirement(name = "bearerAuth")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/create-user-session")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Create user checkout session", description = "Create a new checkout session for user", responses = {
            @ApiResponse(responseCode = "200", description = "User checkout session created successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createCheckoutSession(@Valid @RequestBody CheckoutRequest request) {
        try {
            log.info("Creating checkout session for user");
            String sessionUrl = checkoutService.createCheckoutSession(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Checkout session created successfully");
            response.put("sessionUrl", sessionUrl);

            log.info("Checkout session created successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for checkout session: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid input: " + e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during checkout: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error creating checkout session: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while creating checkout session.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/guest/create-session")
    @Operation(summary = "Create guest checkout session", description = "Create a checkout session for guest users (no authentication required)", responses = {
            @ApiResponse(responseCode = "200", description = "Guest checkout session created successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Entity not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createGuestCheckout(@Valid @RequestBody GuestCheckoutRequest request) {
        try {
            log.info("Creating guest checkout session");
            String sessionUrl = checkoutService.createGuestCheckoutSession(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Guest checkout session created successfully");
            response.put("sessionUrl", sessionUrl);

            log.info("Guest checkout session created successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for guest checkout: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid input: " + e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during guest checkout: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error creating guest checkout session: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while creating guest checkout session.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verify/{sessionId}")
    @Operation(summary = "Verify checkout session by ID", description = "Verify a completed checkout session by session ID (Stripe)", responses = {
            @ApiResponse(responseCode = "200", description = "Checkout session verified successfully", content = @Content(schema = @Schema(implementation = CheckoutVerificationResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid session or payment not completed"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> verifyCheckoutSessionById(@PathVariable String sessionId) {
        try {
            log.info("Verifying checkout session by ID: {}", sessionId);
            CheckoutVerificationResult result = checkoutService.verifyCheckoutSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Checkout session verified successfully");
            response.put("data", result);

            log.info("Checkout session verified successfully: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Session not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Session not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid session state: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid session state: " + e.getMessage());
            response.put("errorCode", "INVALID_STATE");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error verifying checkout session: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while verifying checkout session.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/webhook/cancel")
    @Operation(summary = "Handle payment cancellation", description = "Handle payment cancellation and cleanup order", responses = {
            @ApiResponse(responseCode = "200", description = "Order cleaned up successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> handlePaymentCancellation(@RequestParam String session_id) {
        try {
            log.info("Handling payment cancellation for session: {}", session_id);
            checkoutService.cleanupFailedOrder(session_id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order cleaned up successfully after payment cancellation");

            log.info("Order cleaned up successfully after payment cancellation: {}", session_id);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error handling payment cancellation: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while handling payment cancellation.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/calculate-shipping")
    public ResponseEntity<BigDecimal> calculateShippingCost(
            @RequestBody com.ecommerce.dto.CalculateOrderShippingRequest request) {
        try {
            BigDecimal shippingCost = checkoutService.calculateShippingCost(
                    request.getDeliveryAddress(),
                    request.getItems(),
                    request.getOrderValue());
            return ResponseEntity.ok(shippingCost);
        } catch (Exception e) {
            log.error("Error calculating shipping cost: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BigDecimal.ZERO);
        }
    }

    @PostMapping("/payment-summary")
    public ResponseEntity<com.ecommerce.dto.PaymentSummaryDTO> getPaymentSummary(
            @RequestBody com.ecommerce.dto.CalculateOrderShippingRequest request) {
        try {
            log.info("Received payment summary request: address={}, items={}, userId={}",
                    request.getDeliveryAddress(), request.getItems().size(), request.getUserId());

            java.util.UUID userId = null;
            if (request.getUserId() != null) {
                userId = java.util.UUID.fromString(request.getUserId());
            }

            com.ecommerce.dto.PaymentSummaryDTO summary = checkoutService.calculatePaymentSummary(
                    request.getDeliveryAddress(),
                    request.getItems(),
                    userId);

            log.info("Payment summary calculated successfully: totalAmount={}", summary.getTotalAmount());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error calculating payment summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/cleanup-expired-locks")
    @Operation(summary = "Cleanup expired batch locks", description = "Manually cleanup expired batch locks for debugging")
    public ResponseEntity<?> cleanupExpiredLocks() {
        try {
            checkoutService.cleanupExpiredBatchLocks();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Expired batch locks cleaned up successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cleaning up expired locks: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error cleaning up expired locks");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/debug/stock-locks/{sessionId}")
    @Operation(summary = "Debug stock locks", description = "Get detailed information about stock locks for debugging")
    public ResponseEntity<?> debugStockLocks(@PathVariable String sessionId) {
        try {
            Map<String, Object> lockInfo = checkoutService.getLockedStockInfo(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("lockInfo", lockInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting stock lock info: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error getting stock lock info");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/stock-locks/{sessionId}")
    @Operation(summary = "Get locked stock information", description = "Get information about locked stock for a session", responses = {
            @ApiResponse(responseCode = "200", description = "Locked stock information retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getLockedStockInfo(@PathVariable String sessionId) {
        try {
            log.info("Getting locked stock info for session: {}", sessionId);

            Map<String, Object> lockInfo = checkoutService.getLockedStockInfo(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Locked stock information retrieved successfully");
            response.put("data", lockInfo);

            log.info("Locked stock info retrieved successfully for session: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting locked stock info: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while getting locked stock info.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}