package com.ecommerce.controller;

import com.ecommerce.dto.WarehouseStockPageResponse;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductWarehouseController {

    private final ProductService productService;

    /**
     * Get warehouse stock information for a product with pagination
     * 
     * @param productId The product ID
     * @param page      Page number (0-based)
     * @param size      Page size
     * @param sort      Sort field (default: warehouseName)
     * @param direction Sort direction (asc/desc, default: asc)
     * @return Paginated warehouse stock information
     */
    @GetMapping("/{productId}/warehouse-stock")
    public ResponseEntity<WarehouseStockPageResponse> getProductWarehouseStock(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "warehouseName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        try {
            log.info("Getting warehouse stock for product ID: {}, page: {}, size: {}", productId, page, size);

            // Validate parameters
            if (page < 0)
                page = 0;
            if (size < 1)
                size = 10;
            if (size > 100)
                size = 100; // Limit max page size

            // Create pageable
            Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            WarehouseStockPageResponse response = productService.getProductWarehouseStock(productId, pageable);

            log.info("Successfully retrieved warehouse stock for product ID: {}, total elements: {}",
                    productId, response.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting warehouse stock for product ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get warehouse stock information for a specific product variant with
     * pagination
     * 
     * @param productId The product ID
     * @param variantId The variant ID
     * @param page      Page number (0-based)
     * @param size      Page size
     * @param sort      Sort field (default: warehouseName)
     * @param direction Sort direction (asc/desc, default: asc)
     * @return Paginated warehouse stock information for the variant
     */
    @GetMapping("/{productId}/variants/{variantId}/warehouse-stock")
    public ResponseEntity<WarehouseStockPageResponse> getVariantWarehouseStock(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "warehouseName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        try {
            log.info("Getting warehouse stock for product ID: {} and variant ID: {}, page: {}, size: {}",
                    productId, variantId, page, size);

            // Validate parameters
            if (page < 0)
                page = 0;
            if (size < 1)
                size = 10;
            if (size > 100)
                size = 100; // Limit max page size

            // Create pageable
            Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            WarehouseStockPageResponse response = productService.getVariantWarehouseStock(productId, variantId,
                    pageable);

            log.info("Successfully retrieved warehouse stock for product ID: {} and variant ID: {}, total elements: {}",
                    productId, variantId, response.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting warehouse stock for product ID {} and variant ID {}: {}",
                    productId, variantId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
