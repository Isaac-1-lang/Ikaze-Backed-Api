package com.ecommerce.controller;

import com.ecommerce.dto.SimilarProductsRequestDTO;
import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class SimilarProductsController {

    @Autowired
    private ProductService productService;

    @PostMapping("/similar")
    public ResponseEntity<?> getSimilarProducts(@RequestBody SimilarProductsRequestDTO request) {
        try {
            log.info("Getting similar products for product ID: {}", request.getProductId());

            Page<ManyProductsDto> similarProducts = productService.getSimilarProducts(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", similarProducts,
                    "message", "Similar products retrieved successfully"));
        } catch (Exception e) {
            log.error("Error getting similar products for product ID: {}", request.getProductId(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to get similar products"));
        }
    }

    @GetMapping("/{productId}/similar")
    public ResponseEntity<?> getSimilarProductsByProductId(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "false") boolean includeOutOfStock,
            @RequestParam(defaultValue = "mixed") String algorithm) {
        try {
            log.info("Getting similar products for product ID: {} with algorithm: {}", productId, algorithm);

            SimilarProductsRequestDTO request = new SimilarProductsRequestDTO();
            request.setProductId(java.util.UUID.fromString(productId));
            request.setPage(page);
            request.setSize(size);
            request.setIncludeOutOfStock(includeOutOfStock);
            request.setAlgorithm(algorithm);

            Page<ManyProductsDto> similarProducts = productService.getSimilarProducts(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", similarProducts,
                    "message", "Similar products retrieved successfully"));
        } catch (Exception e) {
            log.error("Error getting similar products for product ID: {}", productId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to get similar products"));
        }
    }
}
