package com.ecommerce.controller;

import com.ecommerce.dto.CreateDiscountDTO;
import com.ecommerce.dto.DiscountDTO;
import com.ecommerce.dto.UpdateDiscountDTO;
import com.ecommerce.entity.Discount;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.repository.DiscountRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.DiscountService;
import com.ecommerce.service.ShopAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Discount Management", description = "APIs for managing discounts, discount codes, and promotional offers")
public class DiscountController {

    private final DiscountService discountService;
    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShopAuthorizationService shopAuthorizationService;
    private final UserRepository userRepository;

    @Operation(summary = "Create a new discount", description = "Creates a new discount with the specified parameters. Requires ADMIN or EMPLOYEE role.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Discount created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiscountDTO.class), examples = @ExampleObject(name = "Created Discount", value = """
                    {
                        "discountId": "550e8400-e29b-41d4-a716-446655440000",
                        "name": "Summer Sale 2024",
                        "description": "20% off on all summer items",
                        "percentage": 20.0,
                        "discountCode": "SUMMER20",
                        "startDate": "2024-06-01T00:00:00",
                        "endDate": "2024-08-31T23:59:59",
                        "usageLimit": 1000,
                        "usedCount": 0,
                        "isActive": true,
                        "isValid": true
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Validation Error", value = """
                    {
                        "error": "Bad Request",
                        "message": "Validation failed",
                        "details": ["Discount percentage must be between 0 and 100"]
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<DiscountDTO> createDiscount(@Valid @RequestBody CreateDiscountDTO createDiscountDTO) {
        log.info("Creating new discount: {} for shop: {}", createDiscountDTO.getName(), createDiscountDTO.getShopId());

        try {
            UUID vendorId = getCurrentUserId();
            if (createDiscountDTO.getShopId() == null) {
                throw new IllegalArgumentException("shopId is required");
            }
            shopAuthorizationService.assertCanManageShop(vendorId, createDiscountDTO.getShopId());

            DiscountDTO createdDiscount = discountService.createDiscount(vendorId, createDiscountDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDiscount);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for discount creation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error creating discount: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Update an existing discount", description = "Updates an existing discount with the provided information. Requires ADMIN or EMPLOYEE role.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discount updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiscountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Discount not found")
    })
    @PutMapping("/{discountId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<DiscountDTO> updateDiscount(
            @Parameter(description = "UUID of the discount to update", required = true) @PathVariable UUID discountId,
            @RequestParam UUID shopId,
            @Valid @RequestBody UpdateDiscountDTO updateDiscountDTO) {
        log.info("Updating discount with ID: {} for shop: {}", discountId, shopId);

        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            DiscountDTO updatedDiscount = discountService.updateDiscount(discountId, vendorId, updateDiscountDTO);
            return ResponseEntity.ok(updatedDiscount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for discount update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating discount: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Delete a discount", description = "Permanently deletes a discount by its ID. Requires ADMIN or EMPLOYEE role.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discount deleted successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Delete Success", value = """
                    {
                        "message": "Discount deleted successfully",
                        "discountId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Discount not found")
    })
    @DeleteMapping("/{discountId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> deleteDiscount(
            @Parameter(description = "UUID of the discount to delete", required = true) @PathVariable UUID discountId,
            @RequestParam UUID shopId) {
        log.info("Deleting discount with ID: {} for shop: {}", discountId, shopId);

        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            // Get discount info before deletion for response
            Discount discount = discountRepository.findById(discountId)
                    .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

            // Verify discount belongs to the shop
            if (discount.getShop() == null || !discount.getShop().getShopId().equals(shopId)) {
                throw new EntityNotFoundException("Discount not found with ID: " + discountId + " for shop: " + shopId);
            }

            // Count affected products and variants
            long affectedProducts = productRepository.countByDiscount(discount);
            long affectedVariants = productVariantRepository.countByDiscount(discount);

            // Perform deletion (service will handle removing discount from
            // products/variants)
            discountService.deleteDiscount(discountId, vendorId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Discount deleted successfully and removed from all associated products");
            response.put("discountId", discountId.toString());
            response.put("discountName", discount.getName());
            response.put("affectedProducts", affectedProducts);
            response.put("affectedVariants", affectedVariants);
            response.put("totalAffected", affectedProducts + affectedVariants);

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting discount: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Get discount by ID", description = "Retrieves a specific discount by its UUID. Accessible to all authenticated users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discount found successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiscountDTO.class))),
            @ApiResponse(responseCode = "404", description = "Discount not found")
    })
    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountDTO> getDiscountById(
            @Parameter(description = "UUID of the discount to retrieve", required = true) @PathVariable UUID discountId,
            @RequestParam(required = false) UUID shopId) {
        log.info("Fetching discount with ID: {} for shop: {}", discountId, shopId);

        try {
            DiscountDTO discount = discountService.getDiscountById(discountId, shopId);
            return ResponseEntity.ok(discount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with ID: {}", discountId);
            throw e;
        } catch (Exception e) {
            log.error("Error fetching discount: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Get all discounts with pagination", description = "Retrieves a paginated list of discounts with optional filtering by active status and sorting options.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discounts retrieved successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Paginated Discounts", value = """
                    {
                        "content": [
                            {
                                "discountId": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "Summer Sale 2024",
                                "description": "20% off on all summer items",
                                "percentage": 20.0,
                                "discountCode": "SUMMER20",
                                "startDate": "2024-06-01T00:00:00",
                                "endDate": "2024-08-31T23:59:59",
                                "usageLimit": 1000,
                                "usedCount": 150,
                                "isActive": true,
                                "isValid": true
                            }
                        ],
                        "pageable": {
                            "sort": {
                                "sorted": true,
                                "unsorted": false
                            },
                            "pageNumber": 0,
                            "pageSize": 10
                        },
                        "totalElements": 25,
                        "totalPages": 3,
                        "last": false,
                        "first": true,
                        "numberOfElements": 10
                    }
                    """)))
    })
    @GetMapping
    public ResponseEntity<Page<DiscountDTO>> getAllDiscounts(
            @Parameter(description = "Shop ID to filter discounts", required = true) @RequestParam UUID shopId,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc") @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "Filter to show only active discounts", example = "false") @RequestParam(defaultValue = "false") boolean activeOnly) {

        log.info("Fetching discounts for shop: {} - page: {}, size: {}, sortBy: {}, sortDirection: {}, activeOnly: {}",
                shopId, page, size, sortBy, sortDirection, activeOnly);

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<DiscountDTO> discounts;
            if (activeOnly) {
                discounts = discountService.getActiveDiscounts(shopId, pageable);
            } else {
                discounts = discountService.getAllDiscounts(shopId, pageable);
            }

            return ResponseEntity.ok(discounts);
        } catch (Exception e) {
            log.error("Error fetching discounts: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Get discount by code", description = "Retrieves a discount by its unique discount code. Useful for applying discount codes during checkout.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discount found successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiscountDTO.class))),
            @ApiResponse(responseCode = "404", description = "Discount code not found")
    })
    @GetMapping("/code/{discountCode}")
    public ResponseEntity<DiscountDTO> getDiscountByCode(
            @Parameter(description = "Discount code to search for", example = "SUMMER20", required = true) @PathVariable String discountCode,
            @RequestParam(required = false) UUID shopId) {
        log.info("Fetching discount by code: {} for shop: {}", discountCode, shopId);

        try {
            DiscountDTO discount = discountService.getDiscountByCode(discountCode, shopId);
            return ResponseEntity.ok(discount);
        } catch (EntityNotFoundException e) {
            log.error("Discount not found with code: {}", discountCode);
            throw e;
        } catch (Exception e) {
            log.error("Error fetching discount by code: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Check if discount is valid by ID", description = "Validates whether a discount is currently active and within its validity period.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation result returned successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Validation Result", value = """
                    {
                        "discountId": "550e8400-e29b-41d4-a716-446655440000",
                        "isValid": true
                    }
                    """)))
    })
    @GetMapping("/{discountId}/valid")
    public ResponseEntity<Map<String, Object>> isDiscountValid(
            @Parameter(description = "UUID of the discount to validate", required = true) @PathVariable UUID discountId,
            @RequestParam(required = false) UUID shopId) {
        log.info("Checking if discount is valid with ID: {} for shop: {}", discountId, shopId);

        try {
            boolean isValid = discountService.isDiscountValid(discountId, shopId);
            Map<String, Object> response = new HashMap<>();
            response.put("discountId", discountId.toString());
            response.put("isValid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking discount validity: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Check if discount code is valid", description = "Validates whether a discount code is currently active and can be applied. Useful for real-time validation during checkout.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation result returned successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Code Validation Result", value = """
                    {
                        "discountCode": "SUMMER20",
                        "isValid": true
                    }
                    """)))
    })
    @GetMapping("/code/{discountCode}/valid")
    public ResponseEntity<Map<String, Object>> isDiscountCodeValid(
            @Parameter(description = "Discount code to validate", example = "SUMMER20", required = true) @PathVariable String discountCode,
            @RequestParam(required = false) UUID shopId) {
        log.info("Checking if discount code is valid: {} for shop: {}", discountCode, shopId);

        try {
            boolean isValid = discountService.isDiscountCodeValid(discountCode, shopId);
            Map<String, Object> response = new HashMap<>();
            response.put("discountCode", discountCode);
            response.put("isValid", isValid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking discount code validity: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Fix discount dates (Admin utility)", description = "Utility endpoint to fix discount dates from 2025 to 2024. This is an administrative function. Requires ADMIN or EMPLOYEE role.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discount dates updated successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Fix Dates Result", value = """
                    {
                        "message": "Updated 15 discount dates to 2024",
                        "updatedCount": "15"
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PostMapping("/fix-dates")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> fixDiscountDates(@RequestParam UUID shopId) {
        log.info("Fixing discount dates to 2024");

        try {
            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            // Get all discounts for the shop and update their dates to 2024
            List<Discount> discounts = discountRepository.findByShopShopId(shopId);
            int updatedCount = 0;

            for (Discount discount : discounts) {
                LocalDateTime startDate = discount.getStartDate();
                LocalDateTime endDate = discount.getEndDate();

                if (startDate != null && startDate.getYear() == 2025) {
                    discount.setStartDate(startDate.withYear(2024));
                    updatedCount++;
                }

                if (endDate != null && endDate.getYear() == 2025) {
                    discount.setEndDate(endDate.withYear(2024));
                }
            }

            // Save all updated discounts
            discountService.saveAllDiscounts(discounts);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Updated " + updatedCount + " discount dates to 2024");
            response.put("updatedCount", String.valueOf(updatedCount));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fixing discount dates: {}", e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Get all active discounts", description = "Retrieves all currently active and valid discounts with product count information. Useful for displaying available promotions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active discounts retrieved successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Active Discounts Response", value = """
                    {
                        "success": true,
                        "data": [
                            {
                                "discountId": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "Summer Sale 2024",
                                "description": "20% off on all summer items",
                                "percentage": 20.0,
                                "discountCode": "SUMMER20",
                                "startDate": "2024-06-01T00:00:00",
                                "endDate": "2024-08-31T23:59:59",
                                "usageLimit": 1000,
                                "usedCount": 150,
                                "isActive": true,
                                "isValid": true,
                                "productCount": 45
                            }
                        ],
                        "total": 5
                    }
                    """))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Error Response", value = """
                    {
                        "success": false,
                        "message": "Failed to fetch active discounts"
                    }
                    """)))
    })
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveDiscounts(@RequestParam(required = false) UUID shopId) {
        try {
            log.info("Fetching active discounts, shopId: {}", shopId);

            List<Discount> activeDiscounts;
            if (shopId != null) {
                activeDiscounts = discountRepository.findActiveAndValidDiscountsByShop(shopId, LocalDateTime.now());
            } else {
                activeDiscounts = discountRepository.findActiveAndValidDiscounts(LocalDateTime.now());
            }

            List<Map<String, Object>> discountData = activeDiscounts.stream()
                    .map(discount -> {
                        Map<String, Object> discountInfo = new HashMap<>();
                        discountInfo.put("discountId", discount.getDiscountId().toString());
                        discountInfo.put("name", discount.getName());
                        discountInfo.put("description", discount.getDescription());
                        discountInfo.put("percentage", discount.getPercentage());
                        discountInfo.put("discountCode", discount.getDiscountCode());
                        discountInfo.put("startDate", discount.getStartDate());
                        discountInfo.put("endDate", discount.getEndDate());
                        discountInfo.put("usageLimit", discount.getUsageLimit());
                        discountInfo.put("usedCount", discount.getUsedCount());
                        discountInfo.put("isActive", discount.isActive());
                        discountInfo.put("isValid", discount.isValid());

                        // Count products with this discount
                        long productCount = productRepository.countByDiscount(discount) +
                                productVariantRepository.countByDiscount(discount);
                        discountInfo.put("productCount", productCount);

                        return discountInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", discountData);
            response.put("total", discountData.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching active discounts: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch active discounts");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Get products associated with a discount", description = "Retrieves all products and variants that have the specified discount applied. Admin-only endpoint for discount management.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Discount Products Response", value = """
                    {
                        "products": [
                            {
                                "productId": "123e4567-e89b-12d3-a456-426614174000",
                                "name": "Summer T-Shirt",
                                "price": 25.99,
                                "discountedPrice": 20.79,
                                "hasVariants": true,
                                "sku": "TSHIRT-001",
                                "isActive": true,
                                "imageUrl": "https://example.com/image.jpg"
                            }
                        ],
                        "variants": [
                            {
                                "variantId": "456e7890-e89b-12d3-a456-426614174001",
                                "variantName": "Red - Large",
                                "variantSku": "TSHIRT-001-RED-L",
                                "price": 25.99,
                                "discountedPrice": 20.79,
                                "productId": "123e4567-e89b-12d3-a456-426614174000",
                                "productName": "Summer T-Shirt",
                                "isActive": true,
                                "imageUrl": "https://example.com/variant-image.jpg"
                            }
                        ],
                        "totalProducts": 15,
                        "totalVariants": 8
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "Discount not found")
    })
    @GetMapping("/{discountId}/products")
    @PreAuthorize("hasAnyRole('VENDOR', 'EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> getProductsByDiscount(
            @Parameter(description = "UUID of the discount to get products for", required = true) @PathVariable String discountId,
            @RequestParam UUID shopId) {
        try {
            log.info("Fetching products for discount: {} in shop: {}", discountId, shopId);

            UUID vendorId = getCurrentUserId();
            shopAuthorizationService.assertCanManageShop(vendorId, shopId);

            UUID discountUuid = UUID.fromString(discountId);
            Discount discount = discountRepository.findById(discountUuid)
                    .orElseThrow(() -> new EntityNotFoundException("Discount not found with ID: " + discountId));

            // Verify discount belongs to the shop
            if (discount.getShop() == null || !discount.getShop().getShopId().equals(shopId)) {
                throw new EntityNotFoundException("Discount not found with ID: " + discountId + " for shop: " + shopId);
            }

            List<Product> products = productRepository.findByDiscount(discount, Pageable.unpaged()).getContent();
            List<ProductVariant> variants = productVariantRepository.findByDiscount(discount, Pageable.unpaged())
                    .getContent();

            log.info("Found {} products and {} variants for discount {}",
                    products.size(), variants.size(), discountId);

            Map<String, Object> response = new HashMap<>();
            response.put("products", products.stream().map(this::mapProductToDTO).collect(Collectors.toList()));
            response.put("variants", variants.stream().map(this::mapVariantToDTO).collect(Collectors.toList()));
            response.put("totalProducts", products.size());
            response.put("totalVariants", variants.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching products for discount {}: {}", discountId, e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> mapProductToDTO(Product product) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("productId", product.getProductId().toString());
        dto.put("name", product.getProductName());
        dto.put("price", product.getPrice());
        dto.put("discountedPrice", product.getDiscountedPrice());
        dto.put("hasVariants", product.hasVariants());
        dto.put("sku", product.getSku());
        dto.put("isActive", product.isActive());
        dto.put("imageUrl", product.getMainImageUrl());
        return dto;
    }

    private Map<String, Object> mapVariantToDTO(ProductVariant variant) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("variantId", variant.getId().toString());
        dto.put("variantName", variant.getVariantName());
        dto.put("variantSku", variant.getVariantSku());
        dto.put("price", variant.getPrice());
        dto.put("discountedPrice", variant.getDiscountedPrice());
        dto.put("productId", variant.getProduct().getProductId().toString());
        dto.put("productName", variant.getProduct().getProductName());
        dto.put("isActive", variant.isActive());
        dto.put("imageUrl", getVariantMainImageUrl(variant));

        // Debug logging
        log.info("Variant {} - Original price: {}, Discounted price: {}, Has discount: {}",
                variant.getId(), variant.getPrice(), variant.getDiscountedPrice(),
                variant.getDiscount() != null);

        return dto;
    }

    private String getVariantMainImageUrl(ProductVariant variant) {
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            return variant.getImages().stream()
                    .filter(img -> img.isPrimary())
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(variant.getImages().get(0).getImageUrl());
        }
        // Fallback to product's main image if variant has no images
        return variant.getProduct().getMainImageUrl();
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email).map(User::getId).orElse(null);
            }

            if (principal instanceof User user && user.getId() != null) {
                return user.getId();
            }

            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email).map(User::getId).orElse(null);
            }

            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name).map(User::getId).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
        }
        return null;
    }
}
