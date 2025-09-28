package com.ecommerce.controller;

import com.ecommerce.dto.PointsPaymentPreviewDTO;
import com.ecommerce.dto.PointsPaymentRequest;
import com.ecommerce.dto.PointsPaymentResult;
import com.ecommerce.service.PointsPaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/points-payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Points Payment", description = "APIs for payment using reward points")
public class PointsPaymentController {

    private final PointsPaymentService pointsPaymentService;

    @PostMapping("/preview")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PointsPaymentPreviewDTO> previewPointsPayment(@Valid @RequestBody PointsPaymentRequest request) {
        try {
            log.info("Previewing points payment for user: {}", request.getUserId());
            PointsPaymentPreviewDTO preview = pointsPaymentService.previewPointsPayment(request);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            log.error("Error previewing points payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PointsPaymentResult> processPointsPayment(@Valid @RequestBody PointsPaymentRequest request) {
        try {
            log.info("Processing points payment for user: {}", request.getUserId());
            PointsPaymentResult result = pointsPaymentService.processPointsPayment(request);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
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
    @PreAuthorize("hasRole('CUSTOMER')")
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
}
