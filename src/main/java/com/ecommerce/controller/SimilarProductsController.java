package com.ecommerce.controller;

import com.ecommerce.dto.SimilarProductsRequestDTO;
import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/customer/products")
public class SimilarProductsController {

    @Autowired
    private ProductService productService;

    @PostMapping("/similar")
    public ResponseEntity<?> getSimilarProducts(@RequestBody SimilarProductsRequestDTO request) {
        try {
            log.info("Getting similar products for product ID: {} (customer view)", request.getProductId());

            boolean isAvailable = productService.isProductAvailableForCustomers(request.getProductId());
            if (!isAvailable) {
                log.warn("Base product {} is not available for customers", request.getProductId());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Product not available",
                        "message", "Base product is not available for customers"));
            }

            Page<ManyProductsDto> similarProducts = productService.getSimilarProducts(request);
            
            List<ManyProductsDto> availableProducts = similarProducts.getContent().stream()
                    .filter(product -> {
                        try {
                            UUID productUuid = product.getProductId();
                            return productService.isProductAvailableForCustomers(productUuid);
                        } catch (Exception e) {
                            log.warn("Error checking availability for product {}: {}", product.getProductId(), e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            
            Page<ManyProductsDto> filteredSimilarProducts = new PageImpl<>(
                    availableProducts,
                    similarProducts.getPageable(),
                    availableProducts.size()
            );
            
            log.info("Filtered {} similar products to {} available products for customers", 
                    similarProducts.getContent().size(), availableProducts.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", filteredSimilarProducts,
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
            log.info("Getting similar products for product ID: {} with algorithm: {} (customer view)", productId, algorithm);

            java.util.UUID productUuid = java.util.UUID.fromString(productId);
            
            // Check if the base product is available for customers
            boolean isAvailable = productService.isProductAvailableForCustomers(productUuid);
            if (!isAvailable) {
                log.warn("Base product {} is not available for customers", productId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Product not available",
                        "message", "Base product is not available for customers"));
            }

            SimilarProductsRequestDTO request = new SimilarProductsRequestDTO();
            request.setProductId(productUuid);
            request.setPage(page);
            request.setSize(size);
            request.setIncludeOutOfStock(includeOutOfStock);
            request.setAlgorithm(algorithm);

            Page<ManyProductsDto> similarProducts = productService.getSimilarProducts(request);
            
            // Filter similar products to only include those available for customers
            List<ManyProductsDto> availableProducts = similarProducts.getContent().stream()
                    .filter(product -> {
                        try {
                            UUID productUuid2 = product.getProductId();
                            return productService.isProductAvailableForCustomers(productUuid2);
                        } catch (Exception e) {
                            log.warn("Error checking availability for product {}: {}", product.getProductId(), e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            
            // Create new page with filtered results
            Page<ManyProductsDto> filteredSimilarProducts = new PageImpl<>(
                    availableProducts,
                    similarProducts.getPageable(),
                    availableProducts.size()
            );
            
            log.info("Filtered {} similar products to {} available products for customers", 
                    similarProducts.getContent().size(), availableProducts.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", filteredSimilarProducts,
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
