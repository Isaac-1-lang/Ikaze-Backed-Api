package com.ecommerce.controller;

import com.ecommerce.dto.AssignDiscountRequest;
import com.ecommerce.dto.RemoveDiscountRequest;
import com.ecommerce.service.ProductDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductDiscountController {

    private final ProductDiscountService productDiscountService;

    @PostMapping("/{productId}/discount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignDiscountToProduct(
            @PathVariable String productId,
            @Valid @RequestBody AssignDiscountRequest request) {
        try {
            log.info("Assigning discount {} to product {}", request.getDiscountId(), productId);

            productDiscountService.assignDiscountToProduct(productId, request.getDiscountId());

            return ResponseEntity.ok(Map.of(
                    "message", "Discount assigned to product successfully",
                    "productId", productId,
                    "discountId", request.getDiscountId()));
        } catch (Exception e) {
            log.error("Error assigning discount to product: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to assign discount to product",
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/variants/discount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignDiscountToVariants(
            @Valid @RequestBody AssignDiscountRequest request) {
        try {
            log.info("Assigning discount {} to {} variants",
                    request.getDiscountId(), request.getVariantIds().size());

            productDiscountService.assignDiscountToVariants(request.getVariantIds(), request.getDiscountId());

            return ResponseEntity.ok(Map.of(
                    "message", "Discount assigned to variants successfully",
                    "discountId", request.getDiscountId(),
                    "variantCount", request.getVariantIds().size()));
        } catch (Exception e) {
            log.error("Error assigning discount to variants: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to assign discount to variants",
                    "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{productId}/discount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeDiscountFromProduct(
            @PathVariable String productId) {
        try {
            log.info("Removing discount from product {}", productId);

            productDiscountService.removeDiscountFromProduct(productId);

            return ResponseEntity.ok(Map.of(
                    "message", "Discount removed from product successfully",
                    "productId", productId));
        } catch (Exception e) {
            log.error("Error removing discount from product: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to remove discount from product",
                    "message", e.getMessage()));
        }
    }

    @DeleteMapping("/variants/discount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeDiscountFromVariants(
            @Valid @RequestBody RemoveDiscountRequest request) {
        try {
            log.info("Removing discount from {} variants", request.getVariantIds().size());

            productDiscountService.removeDiscountFromVariants(request.getVariantIds());

            return ResponseEntity.ok(Map.of(
                    "message", "Discount removed from variants successfully",
                    "variantCount", request.getVariantIds().size()));
        } catch (Exception e) {
            log.error("Error removing discount from variants: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to remove discount from variants",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/discount/{discountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> getProductsByDiscount(
            @PathVariable String discountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Getting products with discount {}", discountId);

            Pageable pageable = PageRequest.of(page, size);
            Page<Map<String, Object>> products = productDiscountService.getProductsByDiscount(discountId, pageable);

            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting products by discount: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{productId}/discount-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductDiscountStatus(
            @PathVariable String productId) {
        try {
            log.info("Getting discount status for product {}", productId);

            Map<String, Object> status = productDiscountService.getProductDiscountStatus(productId);

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting product discount status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get discount status",
                    "message", e.getMessage()));
        }
    }
}
