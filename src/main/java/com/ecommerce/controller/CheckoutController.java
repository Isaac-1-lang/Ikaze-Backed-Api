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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during checkout: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error creating checkout session: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create checkout session");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/guest/create-session")
    @Operation(summary = "Create guest checkout session", description = "Create a checkout session for guest users", responses = {
            @ApiResponse(responseCode = "200", description = "Guest checkout session created successfully", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
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
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during guest checkout: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error creating guest checkout session: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create guest checkout session");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verify/{sessionId}")
    @Operation(summary = "Verify checkout session by ID", description = "Verify a completed checkout session by session ID", responses = {
            @ApiResponse(responseCode = "200", description = "Checkout session verified successfully"),
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
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid session state: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "INVALID_STATE");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error verifying checkout session: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to verify checkout session");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}