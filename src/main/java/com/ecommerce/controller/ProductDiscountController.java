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

    @PostMapping("/discount/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignDiscount(
            @Valid @RequestBody AssignDiscountRequest request) {
        try {
            log.info("Assigning discount {} to products/variants", request.getDiscountId());

            productDiscountService.assignDiscount(request);

            return ResponseEntity.ok(Map.of(
                    "message", "Discount assigned successfully",
                    "discountId", request.getDiscountId(),
                    "productCount", request.getProductIds() != null ? request.getProductIds().size() : 0,
                    "variantCount", request.getVariantIds() != null ? request.getVariantIds().size() : 0));
        } catch (Exception e) {
            log.error("Error assigning discount: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to assign discount",
                    "message", e.getMessage()));
        }
    }

    @DeleteMapping("/discount/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeDiscount(
            @Valid @RequestBody RemoveDiscountRequest request) {
        try {
            log.info("Removing discount from products/variants");

            productDiscountService.removeDiscount(request);

            return ResponseEntity.ok(Map.of(
                    "message", "Discount removed successfully",
                    "productCount", request.getProductIds() != null ? request.getProductIds().size() : 0,
                    "variantCount", request.getVariantIds() != null ? request.getVariantIds().size() : 0));
        } catch (Exception e) {
            log.error("Error removing discount: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to remove discount",
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
