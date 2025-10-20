package com.ecommerce.controller;

import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.CustomerProductDTO;
import com.ecommerce.dto.ProductSearchDTO;
import com.ecommerce.dto.ProductVariantDTO;
import com.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Products", description = "Product endpoints for customers with availability filtering")
public class CustomerProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products available for customers", 
               description = "Returns products that are active, displayToCustomers=true, and have stock available")
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public ResponseEntity<Page<ManyProductsDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            
            Page<ManyProductsDto> products = productService.getAllProductsForCustomers(pageRequest);
            
            log.info("Retrieved {} products for customers", products.getTotalElements());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting products for customers: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/search")
    @Operation(summary = "Search products for customers", 
               description = "Search products with customer availability filtering")
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    public ResponseEntity<Page<ManyProductsDto>> searchProducts(@RequestBody ProductSearchDTO searchDTO) {
        try {
            
            Page<ManyProductsDto> products = productService.searchProductsForCustomers(searchDTO);            
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products for customers", 
               description = "Returns featured products available to customers")
    @ApiResponse(responseCode = "200", description = "Featured products retrieved successfully")
    public ResponseEntity<Page<ManyProductsDto>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        try {
            log.info("Getting featured products for customers - page: {}, size: {}", page, size);
            
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ManyProductsDto> products = productService.getFeaturedProductsForCustomers(pageRequest);
            
            log.info("Retrieved {} featured products for customers", products.getTotalElements());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting featured products for customers: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/bestsellers")
    @Operation(summary = "Get bestseller products for customers", 
               description = "Returns bestseller products available to customers")
    @ApiResponse(responseCode = "200", description = "Bestseller products retrieved successfully")
    public ResponseEntity<Page<ManyProductsDto>> getBestsellerProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        try {
            log.info("Getting bestseller products for customers - page: {}, size: {}", page, size);
            
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ManyProductsDto> products = productService.getBestsellerProductsForCustomers(pageRequest);
            
            log.info("Retrieved {} bestseller products for customers", products.getTotalElements());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting bestseller products for customers: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Get new arrival products for customers", 
               description = "Returns new arrival products available to customers")
    @ApiResponse(responseCode = "200", description = "New arrival products retrieved successfully")
    public ResponseEntity<Page<ManyProductsDto>> getNewArrivalProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        try {
            log.info("Getting new arrival products for customers - page: {}, size: {}", page, size);
            
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ManyProductsDto> products = productService.getNewArrivalProductsForCustomers(pageRequest);
            
            log.info("Retrieved {} new arrival products for customers", products.getTotalElements());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting new arrival products for customers: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category for customers", 
               description = "Returns products in a specific category available to customers")
    @ApiResponse(responseCode = "200", description = "Category products retrieved successfully")
    public ResponseEntity<Page<ManyProductsDto>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        try {
            log.info("Getting products by category {} for customers - page: {}, size: {}", categoryId, page, size);
            
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            
            Page<ManyProductsDto> products = productService.getProductsByCategoryForCustomers(categoryId, pageRequest);
            
            log.info("Retrieved {} products in category {} for customers", products.getTotalElements(), categoryId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting products by category for customers: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/brand/{brandId}")
    @Operation(summary = "Get products by brand for customers", 
               description = "Returns products by a specific brand available to customers")
    @ApiResponse(responseCode = "200", description = "Brand products retrieved successfully")
    public ResponseEntity<Page<ManyProductsDto>> getProductsByBrand(
            @PathVariable UUID brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        try {
            log.info("Getting products by brand {} for customers - page: {}, size: {}", brandId, page, size);
            
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            
            Page<ManyProductsDto> products = productService.getProductsByBrandForCustomers(brandId, pageRequest);
            
            log.info("Retrieved {} products by brand {} for customers", products.getTotalElements(), brandId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting products by brand for customers: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product details for customers", 
               description = "Returns product details if available to customers")
    @ApiResponse(responseCode = "200", description = "Product details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Product not found or not available to customers")
    public ResponseEntity<CustomerProductDTO> getProductById(@PathVariable UUID productId) {
        try {
          boolean isAvailable = productService.isProductAvailableForCustomers(productId);
            if (!isAvailable) {
                log.warn("Product {} is not available for customers", productId);
                return ResponseEntity.notFound().build();
            }
            
            CustomerProductDTO product = productService.getCustomerProductById(productId);
            
            log.info("Retrieved product {} for customers", productId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            log.error("Error getting product {} for customers: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{productId}/variants/available")
    @Operation(summary = "Get available variants for customers", 
               description = "Returns only variants that are active and have stock available")
    @ApiResponse(responseCode = "200", description = "Available variants retrieved successfully")
    public ResponseEntity<List<ProductVariantDTO>> getAvailableVariants(@PathVariable UUID productId) {
        try {
            log.info("Getting available variants for product {} for customers", productId);
            
            List<ProductVariantDTO> variants = productService.getAvailableVariantsForCustomers(productId);
            
            log.info("Retrieved {} available variants for product {} for customers", variants.size(), productId);
            return ResponseEntity.ok(variants);
        } catch (Exception e) {
            log.error("Error getting available variants for product {} for customers: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{productId}/availability")
    @Operation(summary = "Check product availability for customers", 
               description = "Checks if a product is available for customers to purchase")
    @ApiResponse(responseCode = "200", description = "Availability check completed successfully")
    public ResponseEntity<Map<String, Boolean>> checkProductAvailability(@PathVariable UUID productId) {
        try {
            log.info("Checking availability of product {} for customers", productId);
            
            boolean isAvailable = productService.isProductAvailableForCustomers(productId);
            
            Map<String, Boolean> response = Map.of("available", isAvailable);
            
            log.info("Product {} availability for customers: {}", productId, isAvailable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking availability of product {} for customers: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug for customers", 
               description = "Returns product details by slug if available to customers")
    @ApiResponse(responseCode = "200", description = "Product retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Product not found or not available to customers")
    public ResponseEntity<CustomerProductDTO> getProductBySlug(@PathVariable String slug) {
        try {
            log.info("Getting product by slug {} for customers", slug);
            
            CustomerProductDTO product = productService.getCustomerProductBySlug(slug);
            
            // Check if product is available for customers
            boolean isAvailable = productService.isProductAvailableForCustomers(product.getProductId());
            if (!isAvailable) {
                log.warn("Product with slug {} is not available for customers", slug);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Retrieved product by slug {} for customers", slug);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            log.error("Error getting product by slug {} for customers: {}", slug, e.getMessage(), e);
            throw e;
        }
    }
}
