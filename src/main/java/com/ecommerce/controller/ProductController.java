package com.ecommerce.controller;

import com.ecommerce.dto.CreateProductDTO;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.service.ProductService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Create a new product", description = "Create a new product with variants, images, and videos", responses = {
            @ApiResponse(responseCode = "201", description = "Product created successfully", content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createProduct(@Valid @ModelAttribute CreateProductDTO createProductDTO) {
        try {
            log.info("Creating product with name: {}", createProductDTO.getName());
            ProductDTO createdProduct = productService.createProduct(createProductDTO);
            log.info("Product created successfully with ID: {}", createdProduct.getProductId());
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            log.error("Entity not found while creating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("ENTITY_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument while creating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_ARGUMENT", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Runtime error while creating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to create product: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while creating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("UNEXPECTED_ERROR", "An unexpected error occurred"));
        }
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get a product by ID", description = "Retrieve a product by its UUID", responses = {
            @ApiResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<?> getProductById(@PathVariable UUID productId) {
        try {
            log.debug("Fetching product with ID: {}", productId);
            ProductDTO product = productService.getProductById(productId);
            return ResponseEntity.ok(product);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching product with ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch product"));
        }
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get a product by slug", description = "Retrieve a product by its slug", responses = {
            @ApiResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<?> getProductBySlug(@PathVariable String slug) {
        try {
            log.debug("Fetching product with slug: {}", slug);
            ProductDTO product = productService.getProductBySlug(slug);
            return ResponseEntity.ok(product);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with slug: {}", slug);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching product with slug {}: {}", slug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch product"));
        }
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve all products with pagination", responses = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<?> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            log.debug("Fetching all products with pagination - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                    page, size, sortBy, sortDirection);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Page<ProductDTO> products = productService
                    .getAllProducts(PageRequest.of(page, size, Sort.by(direction, sortBy)));
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid pagination parameters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching all products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch products"));
        }
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Retrieve products by category ID with pagination", responses = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            log.debug("Fetching products for category ID: {} with pagination - page: {}, size: {}",
                    categoryId, page, size);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Page<ProductDTO> products = productService.getProductsByCategory(
                    categoryId, PageRequest.of(page, size, Sort.by(direction, sortBy)));
            return ResponseEntity.ok(products);
        } catch (EntityNotFoundException e) {
            log.warn("Category not found with ID: {}", categoryId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("CATEGORY_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for category products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching products for category {}: {}", categoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch products for category"));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by keyword with pagination", responses = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    })
    public ResponseEntity<?> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            log.debug("Searching products with keyword: {} - page: {}, size: {}", keyword, page, size);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Page<ProductDTO> products = productService.searchProducts(
                    keyword, PageRequest.of(page, size, Sort.by(direction, sortBy)));
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid search parameters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error searching products with keyword {}: {}", keyword, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to search products"));
        }
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Retrieve featured products", responses = {
            @ApiResponse(responseCode = "200", description = "Featured products retrieved successfully")
    })
    public ResponseEntity<?> getFeaturedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.debug("Fetching featured products with limit: {}", limit);
            List<ProductDTO> products = productService.getFeaturedProducts(limit);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid limit parameter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching featured products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch featured products"));
        }
    }

    @GetMapping("/bestsellers")
    @Operation(summary = "Get bestseller products", description = "Retrieve bestseller products", responses = {
            @ApiResponse(responseCode = "200", description = "Bestseller products retrieved successfully")
    })
    public ResponseEntity<?> getBestsellerProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.debug("Fetching bestseller products with limit: {}", limit);
            List<ProductDTO> products = productService.getBestsellerProducts(limit);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid limit parameter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching bestseller products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch bestseller products"));
        }
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Get new arrival products", description = "Retrieve new arrival products", responses = {
            @ApiResponse(responseCode = "200", description = "New arrival products retrieved successfully")
    })
    public ResponseEntity<?> getNewArrivalProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.debug("Fetching new arrival products with limit: {}", limit);
            List<ProductDTO> products = productService.getNewArrivalProducts(limit);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid limit parameter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching new arrival products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch new arrival products"));
        }
    }

    @GetMapping("/on-sale")
    @Operation(summary = "Get on sale products", description = "Retrieve on sale products", responses = {
            @ApiResponse(responseCode = "200", description = "On sale products retrieved successfully")
    })
    public ResponseEntity<?> getOnSaleProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.debug("Fetching on sale products with limit: {}", limit);
            List<ProductDTO> products = productService.getOnSaleProducts(limit);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid limit parameter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching on sale products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch on sale products"));
        }
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Delete a product", description = "Delete a product by its ID", responses = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<?> deleteProduct(@PathVariable UUID productId) {
        try {
            log.info("Deleting product with ID: {}", productId);
            boolean deleted = productService.deleteProduct(productId);
            if (deleted) {
                log.info("Product deleted successfully with ID: {}", productId);
                return ResponseEntity.noContent().build();
            } else {
                log.warn("Product not found for deletion with ID: {}", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("PRODUCT_NOT_FOUND", "Product not found with ID: " + productId));
            }
        } catch (Exception e) {
            log.error("Error deleting product with ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to delete product"));
        }
    }

    /**
     * Helper method to create standardized error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
