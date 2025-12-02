package com.ecommerce.controller;

import com.ecommerce.dto.ManyProductsDto;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.dto.ProductSearchDTO;
import com.ecommerce.dto.ProductBasicInfoDTO;
import com.ecommerce.dto.ProductBasicInfoUpdateDTO;
import com.ecommerce.dto.ProductPricingDTO;
import com.ecommerce.dto.ProductPricingUpdateDTO;
import com.ecommerce.dto.ProductMediaDTO;
import com.ecommerce.dto.ProductVideoDTO;
import com.ecommerce.dto.ProductVariantDTO;
import com.ecommerce.dto.ProductVariantImageDTO;
import com.ecommerce.dto.ProductVariantAttributeDTO;
import com.ecommerce.dto.VariantAttributeRequest;
import com.ecommerce.dto.CreateVariantRequest;
import com.ecommerce.dto.WarehouseStockRequest;
import com.ecommerce.dto.WarehouseStockWithBatchesRequest;
import com.ecommerce.dto.ProductDetailsDTO;
import com.ecommerce.dto.ProductDetailsUpdateDTO;
import com.ecommerce.Exception.ProductDeletionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ShopAuthorizationService;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.entity.Product;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.math.BigDecimal;
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
import org.springframework.web.multipart.MultipartFile;

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
    private final ObjectMapper objectMapper;
    private final ShopAuthorizationService shopAuthorizationService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @PostMapping("/create-empty")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Create empty product for editing", description = "Create a minimal product that can be enhanced step by step", responses = {
            @ApiResponse(responseCode = "201", description = "Empty product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createEmptyProduct(
            @RequestParam String name,
            @RequestParam UUID shopId) {
        try {
            log.info("Creating empty product with name: {} for shop: {}", name, shopId);
            UUID currentUserId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(currentUserId, shopId);
            Map<String, Object> response = productService.createEmptyProduct(name, shopId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (com.ecommerce.Exception.CustomException e) {
            log.error("Authorization error creating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating empty product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to create empty product: " + e.getMessage()));
        }
    }

    @GetMapping("/{productId}/has-variants")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Check if product has variants", description = "Check whether a product has variants to determine stock management approach", responses = {
            @ApiResponse(responseCode = "200", description = "Product variant status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> checkProductHasVariants(@PathVariable UUID productId) {
        try {
            log.info("Checking if product {} has variants", productId);
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }
            boolean hasVariants = productService.productHasVariants(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("hasVariants", hasVariants);
            response.put("message", hasVariants ? "Product has variants. Stock should be managed at variant level."
                    : "Product has no variants. Stock can be managed at product level.");

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (com.ecommerce.Exception.CustomException e) {
            log.error("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (Exception e) {
            log.error("Error checking product variants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to check product variants"));
        }
    }

    @PostMapping("/{productId}/assign-stock")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Assign stock to product", description = "Assign stock quantities to warehouses for a product (only when product has no variants)", responses = {
            @ApiResponse(responseCode = "200", description = "Stock assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or product has variants"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> assignProductStock(
            @PathVariable UUID productId,
            @RequestBody List<WarehouseStockRequest> warehouseStocks) {
        try {
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }

            Map<String, Object> result = productService.assignProductStock(productId, warehouseStocks);

            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (com.ecommerce.Exception.CustomException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to assign product stock"));
        }
    }

    @DeleteMapping("/{productId}/unassign-warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Unassign warehouse from product", description = "Remove warehouse assignment from product and delete all associated batches", responses = {
            @ApiResponse(responseCode = "200", description = "Warehouse unassigned successfully"),
            @ApiResponse(responseCode = "404", description = "Product or warehouse not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> unassignWarehouseFromProduct(
            @PathVariable UUID productId,
            @PathVariable Long warehouseId) {
        try {
            log.info("Unassigning warehouse {} from product {}", warehouseId, productId);
            
            Map<String, Object> result = productService.unassignWarehouseFromProduct(productId, warehouseId);
            
            log.info("Successfully unassigned warehouse {} from product {}", warehouseId, productId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for warehouse unassignment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error unassigning warehouse {} from product {}: {}", warehouseId, productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to unassign warehouse from product"));
        }
    }

    @PostMapping("/{productId}/assign-stock-with-batches")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Assign stock with batches to product", description = "Assign stock quantities with batch details to warehouses for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Stock with batches assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> assignProductStockWithBatches(
            @PathVariable UUID productId,
            @RequestBody List<WarehouseStockWithBatchesRequest> warehouseStocks) {
        try {
            log.info("Assigning stock with batches to product {} for {} warehouses", productId, warehouseStocks.size());
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }
            Map<String, Object> response = productService.assignProductStockWithBatches(productId, warehouseStocks);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (com.ecommerce.Exception.CustomException e) {
            log.error("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Validation error assigning product stock with batches: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error assigning product stock with batches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to assign product stock with batches"));
        }
    }

    @PostMapping("/{productId}/variants/{variantId}/assign-stock-with-batches")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Assign stock with batches to product variant", description = "Assign stock quantities with batch details to warehouses for a specific product variant", responses = {
            @ApiResponse(responseCode = "200", description = "Variant stock with batches assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> assignVariantStockWithBatches(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @RequestBody List<WarehouseStockWithBatchesRequest> warehouseStocks) {
        try {
            log.info("Assigning stock with batches to variant {} of product {} for {} warehouses", 
                    variantId, productId, warehouseStocks.size());
            Map<String, Object> response = productService.assignVariantStockWithBatches(productId, variantId, warehouseStocks);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error assigning variant stock with batches: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error assigning variant stock with batches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to assign variant stock with batches"));
        }
    }

    @GetMapping("/{productId}/has-stock")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Check if product has stock", description = "Check whether a product has stock assigned", responses = {
            @ApiResponse(responseCode = "200", description = "Product stock status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> checkProductHasStock(@PathVariable UUID productId) {
        try {
            log.info("Checking if product {} has stock", productId);
            boolean hasStock = productService.productHasStock(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("hasStock", hasStock);
            response.put("message", hasStock ? "Product has stock assigned." : "Product has no stock assigned.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error checking product stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to check product stock"));
        }
    }

    @DeleteMapping("/{productId}/stock")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Remove all product stock", description = "Remove all stock and batches for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product stock removed successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> removeProductStock(@PathVariable UUID productId) {
        try {
            log.info("Removing all stock for product {}", productId);
            productService.removeProductStock(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All product stock removed successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error removing product stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to remove product stock"));
        }
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get a product by ID (Vendor/Employee)", description = "Retrieve a product by its UUID - shows all products regardless of availability status", responses = {
            @ApiResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<?> getProductById(@PathVariable UUID productId) {
        try {
            log.debug("Fetching product with ID: {} for Vendor/employee view", productId);
            
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

    @GetMapping("/{productId}/basic-info")
    @Operation(summary = "Get product basic info", description = "Retrieve basic information of a product for update form", responses = {
            @ApiResponse(responseCode = "200", description = "Product basic info found", content = @Content(schema = @Schema(implementation = ProductBasicInfoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<?> getProductBasicInfo(@PathVariable UUID productId) {
        try {
            log.debug("Fetching product basic info with ID: {}", productId);
            ProductBasicInfoDTO productBasicInfo = productService.getProductBasicInfo(productId);
            return ResponseEntity.ok(productBasicInfo);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching product basic info with ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch product basic info"));
        }
    }

    @PutMapping("/{productId}/basic-info")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Update product basic info", description = "Update basic information of a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product basic info updated successfully", content = @Content(schema = @Schema(implementation = ProductBasicInfoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    public ResponseEntity<?> updateProductBasicInfo(
            @PathVariable UUID productId,
            @RequestBody ProductBasicInfoUpdateDTO updateDTO) {
        try {
            log.debug("Updating product basic info with ID: {}", productId);
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }
            ProductBasicInfoDTO updatedProduct = productService.updateProductBasicInfo(productId, updateDTO);
            return ResponseEntity.ok(updatedProduct);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (com.ecommerce.Exception.CustomException e) {
            log.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for product ID {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating product basic info with ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to update product basic info"));
        }
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get a product by slug (Admin/Employee)", description = "Retrieve a product by its slug - shows all products regardless of availability status", responses = {
            @ApiResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<?> getProductBySlug(@PathVariable String slug) {
        try {
            log.debug("Fetching product with slug: {} for Vendor/employee view", slug);
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
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get all products (Admin/Employee)", description = "Retrieve all products with pagination - shows all products regardless of availability status", responses = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully", content = @Content(schema = @Schema(implementation = ManyProductsDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<?> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            log.debug("Fetching all products for Vendor/employee with pagination - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                    page, size, sortBy, sortDirection);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Page<ManyProductsDto> products = productService
                    .getAllProductsForAdmins(PageRequest.of(page, size, Sort.by(direction, sortBy)));
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            log.error("Invalid pagination parameters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching all products for Vendor/employee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch products"));
        }
    }

    @GetMapping("/search/suggestions")
    @Operation(summary = "Get search suggestions", description = "Get search suggestions based on query for autocomplete", responses = {
            @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getSearchSuggestions(@RequestParam String q) {
        try {
            if (q == null || q.trim().length() < 2) {
                return ResponseEntity.ok(Map.of("suggestions", List.of()));
            }

            List<Map<String, Object>> suggestions = productService.getSearchSuggestions(q.trim());
            return ResponseEntity.ok(Map.of("suggestions", suggestions));

        } catch (Exception e) {
            log.error("Error getting search suggestions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to get search suggestions"));
        }
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Search products with comprehensive filtering (Admin/Employee)", description = "Search all products using various filter criteria - shows all products regardless of availability status", responses = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully", content = @Content(schema = @Schema(implementation = ManyProductsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> searchProducts(@Valid @RequestBody ProductSearchDTO searchDTO) {
        try {
            log.debug("Searching products for Vendor/employee with criteria: {}", searchDTO);

            // Validate that at least one filter criterion is provided
            if (!searchDTO.hasAtLeastOneFilter()) {
                log.warn("No filter criteria provided for product search");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("INVALID_SEARCH_CRITERIA",
                                "At least one filter criterion must be provided"));
            }

            Page<ManyProductsDto> searchResults = productService.searchProducts(searchDTO);

            if (searchResults.isEmpty()) {
                log.debug("No products found matching the search criteria for Vendor/employee");
                return ResponseEntity.ok(searchResults); // Return empty page
            }

            log.debug("Found {} products matching search criteria for Vendor/employee", searchResults.getTotalElements());
            return ResponseEntity.ok(searchResults);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search parameters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_PARAMETERS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error searching products for Vendor/employee: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to search products"));
        }
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Delete a product variant", description = "Delete a specific variant from a product, including all associated images and attributes", responses = {
            @ApiResponse(responseCode = "200", description = "Product variant deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteProductVariant(@PathVariable UUID productId, @PathVariable Long variantId) {
        try {
            log.info("Deleting product variant. Product ID: {}, Variant ID: {}", productId, variantId);
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }

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
        } catch (com.ecommerce.Exception.CustomException e) {
            log.error("Authorization error while deleting product variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
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
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Delete a product", description = "Delete a product with all its variants, images, and videos. Also removes the product from carts and wishlists. Cannot delete if there are pending orders.", responses = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Product cannot be deleted due to pending orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteProduct(@PathVariable UUID productId) {
        try {
            log.info("Deleting product with ID: {}", productId);
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }

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
        } catch (com.ecommerce.Exception.CustomException e) {
            log.error("Authorization error while deleting product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
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


    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("User not authenticated");
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                return user.getId();
            }

            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            }

            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name)
                    .map(com.ecommerce.entity.User::getId)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + name));
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to get current user ID: " + e.getMessage());
        }
        throw new RuntimeException("Unable to get current user ID");
    }

    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }

    @PostMapping("/by-ids")
    @Operation(summary = "Get products by IDs", description = "Fetch multiple products by their IDs for local wishlist/cart management", responses = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getProductsByIds(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> productIds = request.get("productIds");

            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Product IDs list cannot be empty"));
            }

            log.info("Fetching products by IDs: {}", productIds);

            List<ManyProductsDto> products = productService.getProductsByIds(productIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "products", products,
                            "totalProducts", products.size()),
                    "message", "Products retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching products by IDs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch products: " + e.getMessage()));
        }
    }

    @GetMapping("/test-elasticsearch")
    @Operation(summary = "Test Elasticsearch connection", description = "Test endpoint to verify Elasticsearch integration")
    public ResponseEntity<?> testElasticsearch() {
        try {
            List<Map<String, Object>> suggestions = productService.getSearchSuggestions("test");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Elasticsearch integration working",
                    "suggestions_count", suggestions.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Elasticsearch integration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{productId}/pricing")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get product pricing", description = "Retrieve pricing information for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product pricing retrieved successfully", content = @Content(schema = @Schema(implementation = ProductPricingDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getProductPricing(@PathVariable UUID productId) {
        try {
            log.debug("Fetching product pricing for ID: {}", productId);
            ProductPricingDTO pricing = productService.getProductPricing(productId);
            return ResponseEntity.ok(pricing);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching product pricing for ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch product pricing"));
        }
    }

    @PutMapping("/{productId}/pricing")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Update product pricing", description = "Update pricing information for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product pricing updated successfully", content = @Content(schema = @Schema(implementation = ProductPricingDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateProductPricing(
            @PathVariable UUID productId,
            @RequestBody ProductPricingUpdateDTO updateDTO) {
        try {
            log.debug("Updating product pricing for ID: {}", productId);
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }
            ProductPricingDTO updatedPricing = productService.updateProductPricing(productId, updateDTO);
            return ResponseEntity.ok(updatedPricing);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (com.ecommerce.Exception.CustomException e) {
            log.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for product ID {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating product pricing for ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to update product pricing"));
        }
    }

    @GetMapping("/{productId}/variants")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get product variants", description = "Retrieve all variants for a product with pagination", responses = {
            @ApiResponse(responseCode = "200", description = "Product variants retrieved successfully", content = @Content(schema = @Schema(implementation = ProductVariantDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getProductVariants(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            log.debug("Fetching product variants for ID: {} with pagination", productId);

            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            PageRequest pageRequest = PageRequest.of(page, size, sort);

            Page<ProductVariantDTO> variants = productService.getProductVariants(productId, pageRequest);
            return ResponseEntity.ok(variants);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching product variants for ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch product variants"));
        }
    }

    @GetMapping("/{productId}/media/images")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get product images", description = "Retrieve all images for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product images retrieved successfully", content = @Content(schema = @Schema(implementation = ProductMediaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getProductImages(@PathVariable UUID productId) {
        try {
            log.debug("Fetching product images for ID: {}", productId);
            List<ProductMediaDTO> images = productService.getProductImages(productId);
            return ResponseEntity.ok(images);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching product images for ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch product images"));
        }
    }

    @GetMapping("/{productId}/media/videos")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get product videos", description = "Retrieve all videos for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product videos retrieved successfully", content = @Content(schema = @Schema(implementation = ProductVideoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getProductVideos(@PathVariable UUID productId) {
        try {
            log.debug("Fetching product videos for ID: {}", productId);
            List<ProductVideoDTO> videos = productService.getProductVideos(productId);
            return ResponseEntity.ok(videos);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching product videos for ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to fetch product videos"));
        }
    }

    @DeleteMapping("/{productId}/media/images/{imageId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Delete product image", description = "Delete a specific product image", responses = {
            @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product or image not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteProductImage(@PathVariable UUID productId, @PathVariable Long imageId) {
        try {
            log.debug("Deleting product image {} for product ID: {}", imageId, productId);
            productService.deleteProductImage(productId, imageId);
            return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Product or image not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting product image {} for product ID {}: {}", imageId, productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to delete product image"));
        }
    }

    @DeleteMapping("/{productId}/media/videos/{videoId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Delete product video", description = "Delete a specific product video", responses = {
            @ApiResponse(responseCode = "200", description = "Video deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product or video not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteProductVideo(@PathVariable UUID productId, @PathVariable Long videoId) {
        try {
            log.debug("Deleting product video {} for product ID: {}", videoId, productId);
            productService.deleteProductVideo(productId, videoId);
            return ResponseEntity.ok(Map.of("message", "Video deleted successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Product or video not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting product video {} for product ID {}: {}", videoId, productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to delete product video"));
        }
    }

    @PutMapping("/{productId}/media/images/{imageId}/primary")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Set primary image", description = "Set a specific image as the primary image for the product", responses = {
            @ApiResponse(responseCode = "200", description = "Primary image set successfully"),
            @ApiResponse(responseCode = "404", description = "Product or image not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> setPrimaryImage(@PathVariable UUID productId, @PathVariable Long imageId) {
        try {
            log.debug("Setting image {} as primary for product ID: {}", imageId, productId);
            productService.setPrimaryImage(productId, imageId);
            return ResponseEntity.ok(Map.of("message", "Primary image set successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Product or image not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting primary image {} for product ID {}: {}", imageId, productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to set primary image"));
        }
    }

    @PostMapping(value = "/{productId}/media/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Upload product images", description = "Upload new images for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Images uploaded successfully", content = @Content(schema = @Schema(implementation = ProductMediaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> uploadProductImages(
            @PathVariable UUID productId,
            @RequestParam("images") List<MultipartFile> images) {
        try {
            log.debug("Uploading {} images for product ID: {}", images.size(), productId);
            List<ProductMediaDTO> uploadedImages = productService.uploadProductImages(productId, images);
            return ResponseEntity.ok(uploadedImages);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for product ID {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading images for product ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to upload images"));
        }
    }

    @PostMapping(value = "/{productId}/media/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Upload product videos", description = "Upload new videos for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Videos uploaded successfully", content = @Content(schema = @Schema(implementation = ProductVideoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> uploadProductVideos(
            @PathVariable UUID productId,
            @RequestParam("videos") List<MultipartFile> videos) {
        try {
            log.debug("Uploading {} videos for product ID: {}", videos.size(), productId);
            List<ProductVideoDTO> uploadedVideos = productService.uploadProductVideos(productId, videos);
            return ResponseEntity.ok(uploadedVideos);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found with ID: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("PRODUCT_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for product ID {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading videos for product ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to upload videos"));
        }
    }

    @PutMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Update product variant", description = "Update specific fields of a product variant", responses = {
            @ApiResponse(responseCode = "200", description = "Variant updated successfully", content = @Content(schema = @Schema(implementation = ProductVariantDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product or variant not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateProductVariant(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @RequestBody Map<String, Object> updates) {
        try {
            log.debug("Updating variant {} for product {}", variantId, productId);
            ProductVariantDTO updatedVariant = productService.updateProductVariant(productId, variantId, updates);
            return ResponseEntity.ok(updatedVariant);
        } catch (EntityNotFoundException e) {
            log.warn("Product or variant not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for variant {} of product {}: {}", variantId, productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating variant {} of product {}: {}", variantId, productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to update variant"));
        }
    }

    @DeleteMapping("/{productId}/variants/{variantId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Delete variant image", description = "Delete an image from a product variant", responses = {
            @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product, variant, or image not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteVariantImage(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @PathVariable Long imageId) {
        try {
            log.debug("Deleting image {} from variant {} of product {}", imageId, variantId, productId);
            productService.deleteVariantImage(productId, variantId, imageId);
            return ResponseEntity.ok().body(Map.of("message", "Image deleted successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Product, variant, or image not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting image {} from variant {} of product {}: {}", imageId, variantId, productId,
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to delete image"));
        }
    }

    @PutMapping("/{productId}/variants/{variantId}/images/{imageId}/primary")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Set primary variant image", description = "Set an image as primary for a product variant", responses = {
            @ApiResponse(responseCode = "200", description = "Primary image set successfully"),
            @ApiResponse(responseCode = "404", description = "Product, variant, or image not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> setPrimaryVariantImage(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @PathVariable Long imageId) {
        try {
            log.debug("Setting image {} as primary for variant {} of product {}", imageId, variantId, productId);
            productService.setPrimaryVariantImage(productId, variantId, imageId);
            return ResponseEntity.ok().body(Map.of("message", "Primary image set successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Product, variant, or image not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting image {} as primary for variant {} of product {}: {}", imageId, variantId,
                    productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to set primary image"));
        }
    }

    @PostMapping(value = "/{productId}/variants/{variantId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Upload variant images", description = "Upload images for a product variant", responses = {
            @ApiResponse(responseCode = "200", description = "Images uploaded successfully", content = @Content(schema = @Schema(implementation = ProductVariantImageDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product or variant not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input or image limit exceeded"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> uploadVariantImages(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @RequestParam("images") List<MultipartFile> images) {
        try {
            log.debug("Uploading {} images for variant {} of product {}", images.size(), variantId, productId);
            List<ProductVariantImageDTO> uploadedImages = productService.uploadVariantImages(productId, variantId,
                    images);
            return ResponseEntity.ok(uploadedImages);
        } catch (EntityNotFoundException e) {
            log.warn("Product or variant not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for variant {} of product {}: {}", variantId, productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading images for variant {} of product {}: {}", variantId, productId, e.getMessage(),
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to upload images"));
        }
    }

    @DeleteMapping("/{productId}/variants/{variantId}/attributes/{attributeValueId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Remove variant attribute", description = "Remove an attribute from a product variant", responses = {
            @ApiResponse(responseCode = "200", description = "Attribute removed successfully"),
            @ApiResponse(responseCode = "404", description = "Product, variant, or attribute not found"),
            @ApiResponse(responseCode = "400", description = "Cannot remove last attribute"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> removeVariantAttribute(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @PathVariable Long attributeValueId) {
        try {
            log.debug("Removing attribute {} from variant {} of product {}", attributeValueId, variantId, productId);
            productService.removeVariantAttribute(productId, variantId, attributeValueId);
            return ResponseEntity.ok().body(Map.of("message", "Attribute removed successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Product, variant, or attribute not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid operation for variant {} of product {}: {}", variantId, productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("INVALID_OPERATION", e.getMessage()));
        } catch (Exception e) {
            log.error("Error removing attribute {} from variant {} of product {}: {}", attributeValueId, variantId,
                    productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to remove attribute"));
        }
    }

    @PostMapping("/{productId}/variants/{variantId}/attributes")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Add variant attributes", description = "Add attributes to a product variant", responses = {
            @ApiResponse(responseCode = "200", description = "Attributes added successfully", content = @Content(schema = @Schema(implementation = ProductVariantAttributeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product or variant not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> addVariantAttributes(
            @PathVariable UUID productId,
            @PathVariable Long variantId,
            @Valid @RequestBody List<VariantAttributeRequest> attributeRequests) {
        try {
            log.debug("Adding {} attributes to variant {} of product {}", attributeRequests.size(), variantId,
                    productId);
            List<ProductVariantAttributeDTO> addedAttributes = productService.addVariantAttributes(productId, variantId,
                    attributeRequests);
            return ResponseEntity.ok(addedAttributes);
        } catch (EntityNotFoundException e) {
            log.warn("Product or variant not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding attributes to variant {} of product {}: {}", variantId, productId, e.getMessage(),
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to add attributes"));
        }
    }

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Create product variant", description = "Create a new variant for a product", responses = {
            @ApiResponse(responseCode = "201", description = "Variant created successfully", content = @Content(schema = @Schema(implementation = ProductVariantDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createProductVariant(
            @PathVariable UUID productId,
            @RequestParam("variantName") String variantName,
            @RequestParam("variantSku") String variantSku,
            @RequestParam(value = "variantBarcode", required = false) String variantBarcode,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "salePrice", required = false) BigDecimal salePrice,
            @RequestParam(value = "costPrice", required = false) BigDecimal costPrice,
            @RequestParam("isActive") Boolean isActive,
            @RequestParam("attributes") String attributesJson,
            @RequestParam(value = "warehouseStocks", required = false) String warehouseStocksJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            log.debug("Creating new variant for product {}", productId);

            CreateVariantRequest request = CreateVariantRequest.builder()
                    .variantName(variantName)
                    .variantSku(variantSku)
                    .variantBarcode(variantBarcode)
                    .price(price)
                    .salePrice(salePrice)
                    .costPrice(costPrice)
                    .isActive(isActive)
                    .images(images)
                    .build();

            List<VariantAttributeRequest> attributes = objectMapper.readValue(attributesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, VariantAttributeRequest.class));
            request.setAttributes(attributes);

            if (warehouseStocksJson != null && !warehouseStocksJson.isEmpty()) {
                List<WarehouseStockRequest> warehouseStocks = objectMapper.readValue(warehouseStocksJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, WarehouseStockRequest.class));
                request.setWarehouseStocks(warehouseStocks);
            }

            ProductVariantDTO createdVariant = productService.createProductVariant(productId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdVariant);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for product {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating variant for product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to create variant: " + e.getMessage()));
        }
    }

    @GetMapping("/{productId}/details")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Get product details", description = "Get SEO and detailed information for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product details retrieved successfully", content = @Content(schema = @Schema(implementation = ProductDetailsDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getProductDetails(@PathVariable UUID productId) {
        try {
            log.info("Getting product details for product ID: {}", productId);
            ProductDetailsDTO productDetails = productService.getProductDetails(productId);
            return ResponseEntity.ok(productDetails);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting product details for product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to get product details"));
        }
    }

    @PutMapping("/{productId}/details")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    @Operation(summary = "Update product details", description = "Update SEO and detailed information for a product", responses = {
            @ApiResponse(responseCode = "200", description = "Product details updated successfully", content = @Content(schema = @Schema(implementation = ProductDetailsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or no fields provided"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to manage this shop"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateProductDetails(
            @PathVariable UUID productId,
            @RequestBody ProductDetailsUpdateDTO updateDTO) {
        try {
            log.info("Updating product details for product ID: {}", productId);
            UUID currentUserId = getCurrentUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            if (product.getShop() != null) {
                shopAuthorizationService.assertCanManageShop(currentUserId, product.getShop().getShopId());
            }
            ProductDetailsDTO updatedDetails = productService.updateProductDetails(productId, updateDTO);
            return ResponseEntity.ok(updatedDetails);
        } catch (EntityNotFoundException e) {
            log.warn("Product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (com.ecommerce.Exception.CustomException e) {
            log.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("FORBIDDEN", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for product {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating product details for product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "Failed to update product details"));
        }
    }
}
