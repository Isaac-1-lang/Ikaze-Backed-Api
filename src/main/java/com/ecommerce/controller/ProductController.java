package com.ecommerce.controller;

import com.ecommerce.dto.CreateProductDTO;
import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.ProductSearchDTO;
import com.ecommerce.dto.ProductUpdateDTO;
import com.ecommerce.exception.ProductDeletionException;
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
    @Operation(summary = "Get all products", description = "Retrieve all products with pagination for card display", responses = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully", content = @Content(schema = @Schema(implementation = ManyProductsDto.class)))
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
            Page<ManyProductsDto> products = productService
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

    @PostMapping("/search")
    @Operation(summary = "Search products with comprehensive filtering", description = "Search products using various filter criteria and return paginated results", responses = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully", content = @Content(schema = @Schema(implementation = ManyProductsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> searchProducts(@Valid @RequestBody ProductSearchDTO searchDTO) {
        try {
            log.debug("Searching products with criteria: {}", searchDTO);

            // Validate that at least one filter criterion is provided
            if (!searchDTO.hasAtLeastOneFilter()) {
                log.warn("No filter criteria provided for product search");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("INVALID_SEARCH_CRITERIA",
                                "At least one filter criterion must be provided"));
            }

            Page<ManyProductsDto> searchResults = productService.searchProducts(searchDTO);

            if (searchResults.isEmpty()) {
                log.debug("No products found matching the search criteria");
                return ResponseEntity.ok(searchResults); // Return empty page
            }

            log.debug("Found {} products matching search criteria", searchResults.getTotalElements());
            return ResponseEntity.ok(searchResults);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search parameters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error searching products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to search products"));
        }
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Update a product", description = "Update an existing product with optional new variants and images", responses = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully", content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateProduct(@PathVariable UUID productId,
            @Valid @ModelAttribute ProductUpdateDTO updateProductDTO) {
        try {
            log.info("Updating product with ID: {}", productId);
            ProductDTO updatedProduct = productService.updateProduct(productId, updateProductDTO);
            log.info("Product updated successfully with ID: {}", productId);
            return ResponseEntity.ok(updatedProduct);
        } catch (EntityNotFoundException e) {
            log.error("Entity not found while updating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("ENTITY_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument while updating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_ARGUMENT", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Runtime error while updating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to update product: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while updating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("UNEXPECTED_ERROR", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Delete a product variant", description = "Delete a specific variant from a product, including all associated images and attributes", responses = {
            @ApiResponse(responseCode = "200", description = "Product variant deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteProductVariant(@PathVariable UUID productId, @PathVariable Long variantId) {
        try {
            log.info("Deleting product variant. Product ID: {}, Variant ID: {}", productId, variantId);

            boolean deleted = productService.deleteProductVariant(productId, variantId);

            if (deleted) {
                log.info("Product variant deleted successfully. Product ID: {}, Variant ID: {}", productId, variantId);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Product variant deleted successfully");
                response.put("productId", productId);
                response.put("variantId", variantId);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to delete product variant. Product ID: {}, Variant ID: {}", productId, variantId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("DELETION_FAILED", "Failed to delete product variant"));
            }

        } catch (EntityNotFoundException e) {
            log.error("Entity not found while deleting product variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("ENTITY_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument while deleting product variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_ARGUMENT", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Runtime error while deleting product variant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to delete product variant: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while deleting product variant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("UNEXPECTED_ERROR", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Delete a product", description = "Delete a product with all its variants, images, and videos. Also removes the product from carts and wishlists. Cannot delete if there are pending orders.", responses = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Product cannot be deleted due to pending orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteProduct(@PathVariable UUID productId) {
        try {
            log.info("Deleting product with ID: {}", productId);

            boolean deleted = productService.deleteProduct(productId);

            if (deleted) {
                log.info("Product deleted successfully with ID: {}", productId);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Product deleted successfully");
                response.put("productId", productId);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Product not found for deletion with ID: {}", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("PRODUCT_NOT_FOUND", "Product not found with ID: " + productId));
            }

        } catch (EntityNotFoundException e) {
            log.error("Product not found while deleting: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (ProductDeletionException e) {
            log.error("Product deletion blocked: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("DELETION_BLOCKED", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Runtime error while deleting product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to delete product: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while deleting product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("UNEXPECTED_ERROR", "An unexpected error occurred"));
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
