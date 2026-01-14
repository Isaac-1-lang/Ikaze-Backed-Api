package com.ecommerce.controller;

import com.ecommerce.dto.PointsPaymentPreviewDTO;
import com.ecommerce.dto.PointsPaymentRequest;
import com.ecommerce.dto.PointsPaymentResult;
import com.ecommerce.service.PointsPaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/points-payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Points Payment", description = "APIs for payment using reward points")
public class PointsPaymentController {

    private final PointsPaymentService pointsPaymentService;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('CUSTOMER','DELIVERY_AGENT','ADMIN')")
    public ResponseEntity<?> previewPointsPayment(@Valid @RequestBody PointsPaymentRequest request) {
        try {
            log.info("Previewing points payment for user: {}", request.getUserId());
            PointsPaymentPreviewDTO preview = pointsPaymentService.previewPointsPayment(request);
            return ResponseEntity.ok(preview);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for points payment preview: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "VALIDATION_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during points payment preview: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Resource not found: " + e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (RuntimeException e) {
            log.error("Runtime error previewing points payment: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "RUNTIME_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error previewing points payment: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred while previewing points payment.");
            response.put("errorCode", "INTERNAL_ERROR");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('CUSTOMER','DELIVERY_AGENT','ADMIN')")
    public ResponseEntity<?> processPointsPayment(@Valid @RequestBody PointsPaymentRequest request) {
        try {
            log.info("Processing points payment for user: {}", request.getUserId());
            PointsPaymentResult result = pointsPaymentService.processPointsPayment(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for points payment processing: {}", e.getMessage());
            PointsPaymentResult errorResult = new PointsPaymentResult(false, e.getMessage(),
                    null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, false);
            return ResponseEntity.badRequest().body(errorResult);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during points payment processing: {}", e.getMessage());
            PointsPaymentResult errorResult = new PointsPaymentResult(false, "Resource not found: " + e.getMessage(),
                    null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResult);

        } catch (RuntimeException e) {
            log.error("Error processing points payment: {}", e.getMessage(), e);
            PointsPaymentResult errorResult = new PointsPaymentResult(false, e.getMessage(),
                    null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, false);
            return ResponseEntity.badRequest().body(errorResult);

        } catch (Exception e) {
            log.error("Unexpected error processing points payment: {}", e.getMessage(), e);
            PointsPaymentResult errorResult = new PointsPaymentResult(false, "Payment processing failed",
                    null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, false);
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @PostMapping("/complete-hybrid/{userId}/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','DELIVERY_AGENT','ADMIN', 'VENDOR')")
    public ResponseEntity<PointsPaymentResult> completeHybridPayment(
            @PathVariable UUID userId,
            @PathVariable Long orderId,
            @RequestParam String stripeSessionId) {
        try {
            log.info("Completing hybrid payment for user: {}, order: {}", userId, orderId);
            PointsPaymentResult result = pointsPaymentService.completeHybridPayment(userId, orderId, stripeSessionId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error completing hybrid payment: {}", e.getMessage(), e);
            PointsPaymentResult errorResult = new PointsPaymentResult(false, "Failed to complete payment",
                    null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, false);
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    @PostMapping("/check-eligibility")
    @PreAuthorize("hasAnyRole('CUSTOMER','DELIVERY_AGENT','ADMIN')")
    public ResponseEntity<?> checkPointsEligibility(@RequestBody com.ecommerce.dto.PointsEligibilityRequest request) {
        try {
            log.info("Checking points eligibility for user: {}", request.getUserId());
            com.ecommerce.dto.PointsEligibilityResponse response = pointsPaymentService.checkPointsEligibility(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking points eligibility: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
