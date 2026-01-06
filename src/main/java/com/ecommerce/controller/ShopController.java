package com.ecommerce.controller;

import com.ecommerce.dto.ShopDTO;
import com.ecommerce.service.ShopService;
import com.ecommerce.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shops")
@Tag(name = "Shop Management", description = "Endpoints for managing shops")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ShopController {

    private final ShopService shopService;
    private final com.ecommerce.repository.UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final com.ecommerce.service.ProductService productService;

    @Autowired
    public ShopController(ShopService shopService, com.ecommerce.repository.UserRepository userRepository,
            CloudinaryService cloudinaryService, com.ecommerce.service.ProductService productService) {
        this.shopService = shopService;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.productService = productService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")
    @Operation(summary = "Create a new shop with logo URL", description = "Create a new shop with logo URL. CUSTOMER role will be automatically changed to VENDOR after shop creation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Shop created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid shop data or shop with same slug/name already exists", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - VENDOR or ADMIN role required", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createShop(@Valid @RequestBody ShopDTO shopDTO) {
        try {
            UUID ownerId = getCurrentUserId();
            ShopDTO createdShop = shopService.createShop(shopDTO, ownerId);
            return new ResponseEntity<>(createdShop, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Failed to create shop", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to create shop"));
        }
    }

    @PostMapping(value = "/with-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")
    @Operation(summary = "Create a new shop with logo file upload", description = "Create a new shop with logo file upload. CUSTOMER role will be automatically changed to VENDOR after shop creation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Shop created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid shop data, invalid logo file, or shop with same slug/name already exists", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - VENDOR or ADMIN role required", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createShopWithLogo(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam("contactPhone") String contactPhone,
            @RequestParam("address") String address,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            @RequestParam(value = "logo", required = false) MultipartFile logoFile) {
        try {
            UUID ownerId = getCurrentUserId();

            ShopDTO shopDTO = new ShopDTO();
            shopDTO.setName(name);
            shopDTO.setDescription(description);
            shopDTO.setContactEmail(contactEmail);
            shopDTO.setContactPhone(contactPhone);
            shopDTO.setAddress(address);
            shopDTO.setIsActive(isActive);

            if (logoFile != null && !logoFile.isEmpty()) {
                if (!logoFile.getContentType().startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            java.util.Map.of(
                                    "success", false,
                                    "message", "Logo must be an image file"));
                }

                if (logoFile.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            java.util.Map.of(
                                    "success", false,
                                    "message", "Logo file size must be less than 5MB"));
                }

                try {
                    Map<String, String> uploadResult = cloudinaryService.uploadImage(logoFile);
                    String logoUrl = uploadResult.get("url");
                    if (logoUrl != null) {
                        shopDTO.setLogoUrl(logoUrl);
                    }
                } catch (IOException e) {
                    log.error("Failed to upload logo to Cloudinary", e);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            java.util.Map.of(
                                    "success", false,
                                    "message", "Failed to upload logo: " + e.getMessage()));
                }
            }

            ShopDTO createdShop = shopService.createShop(shopDTO, ownerId);
            return new ResponseEntity<>(createdShop, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Failed to create shop", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to create shop"));
        }
    }

    @PutMapping("/{shopId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Update a shop", description = "Update an existing shop (Vendor/Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid shop data or unauthorized to update this shop", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - VENDOR or ADMIN role required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Shop not found", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateShop(
            @PathVariable UUID shopId,
            @Valid @RequestBody ShopDTO shopDTO) {
        try {
            UUID ownerId = getCurrentUserId();
            ShopDTO updatedShop = shopService.updateShop(shopId, shopDTO, ownerId);
            return ResponseEntity.ok(updatedShop);
        } catch (Exception e) {
            log.error("Failed to update shop {}", shopId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to update shop"));
        }
    }

    @DeleteMapping("/{shopId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Delete a shop", description = "Delete a shop (Vendor/Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop deleted successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad request - Unauthorized to delete this shop", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - VENDOR or ADMIN role required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Shop not found", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> deleteShop(@PathVariable UUID shopId) {
        try {
            UUID ownerId = getCurrentUserId();
            shopService.deleteShop(shopId, ownerId);
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", true,
                            "message", "Shop deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete shop {}", shopId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to delete shop"));
        }
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Get shop by ID", description = "Retrieve a shop by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid shop ID", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Shop not found", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getShopById(@PathVariable UUID shopId) {
        try {
            ShopDTO shop = shopService.getShopById(shopId);
            return ResponseEntity.ok(shop);
        } catch (Exception e) {
            log.error("Failed to get shop {}", shopId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to get shop"));
        }
    }

    @GetMapping("/{shopId}/details")
    @Operation(summary = "Get shop details for profile", description = "Retrieve detailed shop information for profile page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop details retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.ecommerce.dto.ShopDetailsDTO.class))),
            @ApiResponse(responseCode = "404", description = "Shop not found", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getShopDetails(@PathVariable UUID shopId) {
        try {
            com.ecommerce.dto.ShopDetailsDTO details = shopService.getShopDetails(shopId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            log.error("Failed to get shop details {}", shopId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    java.util.Map.of(
                            "success", false,
                            "message", "Shop not found: " + e.getMessage()));
        }
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get shop by slug", description = "Retrieve a shop by its slug")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid shop slug", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Shop not found", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getShopBySlug(@PathVariable String slug) {
        try {
            ShopDTO shop = shopService.getShopBySlug(slug);
            return ResponseEntity.ok(shop);
        } catch (Exception e) {
            log.error("Failed to get shop by slug {}", slug, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to get shop by slug"));
        }
    }

    @GetMapping("/my-shops")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Get my shops", description = "Retrieve all shops owned by the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shops retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Failed to retrieve shops", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - VENDOR or ADMIN role required", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getMyShops() {
        try {
            UUID ownerId = getCurrentUserId();
            List<ShopDTO> shops = shopService.getShopsByOwner(ownerId);
            return ResponseEntity.ok(shops);
        } catch (Exception e) {
            log.error("Failed to get shops for current user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to get shops"));
        }
    }

    @GetMapping("/user-shops")
    @PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER', 'EMPLOYEE', 'DELIVERY_AGENT', 'ADMIN')")
    @Operation(summary = "Get user shops", description = "Retrieve all shops associated with the current user (owned shops for VENDOR/CUSTOMER, assigned shop for EMPLOYEE/DELIVERY_AGENT)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shops retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Failed to retrieve shops", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden - VENDOR, CUSTOMER, EMPLOYEE, DELIVERY_AGENT or ADMIN role required", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getUserShops() {
        log.info("ShopController.getUserShops: Request received");

        try {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext()
                    .getAuthentication();
            log.info("ShopController.getUserShops: SecurityContext authentication - present: {}", auth != null);

            if (auth == null) {
                log.error("ShopController.getUserShops: No authentication found in SecurityContext");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        java.util.Map.of(
                                "success", false,
                                "message", "Authentication required"));
            }

            log.info("ShopController.getUserShops: Authentication details - name: {}, principal: {}, authenticated: {}",
                    auth.getName(), auth.getPrincipal().getClass().getSimpleName(), auth.isAuthenticated());
            log.info("ShopController.getUserShops: Authorities: {}", auth.getAuthorities());

            if (auth.getAuthorities() != null) {
                auth.getAuthorities().forEach(authority -> {
                    log.info("ShopController.getUserShops: Authority - {}", authority.getAuthority());
                });
            }

            UUID userId = getCurrentUserId();
            log.info("ShopController.getUserShops: Extracted userId: {}", userId);

            List<ShopDTO> shops = shopService.getUserShops(userId);
            log.info("ShopController.getUserShops: Retrieved {} shops for user {}", shops.size(), userId);

            return ResponseEntity.ok(shops);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.error("ShopController.getUserShops: Access denied exception", e);
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext()
                    .getAuthentication();
            if (auth != null) {
                log.error("ShopController.getUserShops: User: {}, Authorities: {}",
                        auth.getName(), auth.getAuthorities());
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    java.util.Map.of(
                            "success", false,
                            "message", "Access denied: " + e.getMessage()));
        } catch (Exception e) {
            log.error("ShopController.getUserShops: Exception occurred", e);
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext()
                    .getAuthentication();
            if (auth != null) {
                log.error("ShopController.getUserShops: Exception context - User: {}, Authorities: {}",
                        auth.getName(), auth.getAuthorities());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to get shops"));
        }
    }

    @GetMapping
    @Operation(summary = "Get all shops", description = "Retrieve all shops with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shops retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid pagination parameters", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAllShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ShopDTO> shops = shopService.getAllShops(pageable);
            return ResponseEntity.ok(shops);
        } catch (Exception e) {
            log.error("Failed to get all shops", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to get shops"));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search shops", description = "Search shops by name, description, address, or owner with filtering and sorting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shops retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid parameters", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> searchShops(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "rating-desc") String sort) {
        try {
            String[] sortParams = sort.split("-");
            String sortBy = sortParams[0];
            String sortDirection = sortParams.length > 1 ? sortParams[1] : "desc";

            // Map frontend sort keys to DATABASE COLUMN NAMES (for native SQL)
            String sortField = switch (sortBy) {
                case "rating" -> "rating";
                case "products" -> "product_count"; // Database column name
                case "name" -> "shop_name"; // Database column name
                default -> "created_at"; // Database column name
            };

            Sort sorting = sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending()
                    : Sort.by(sortField).descending();

            Pageable pageable = PageRequest.of(page, size, sorting);
            Page<ShopDTO> shops = shopService.searchShops(search, category, pageable);
            return ResponseEntity.ok(shops);
        } catch (Exception e) {
            log.error("Failed to search shops", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to search shops"));
        }
    }

    @GetMapping("/{shopId}/products")
    @Operation(summary = "Get shop products", description = "Retrieve products for a specific shop with pagination (available for customers)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop products retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid shop ID", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Shop not found", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getShopProducts(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<com.ecommerce.dto.ManyProductsDto> products = productService.getProductsByShopForCustomers(shopId,
                    pageable);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Failed to get products for shop {}", shopId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to get shop products"));
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get active shops", description = "Retrieve all active shops")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active shops retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShopDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Failed to retrieve active shops", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getActiveShops() {
        try {
            List<ShopDTO> shops = shopService.getActiveShops();
            return ResponseEntity.ok(shops);
        } catch (Exception e) {
            log.error("Failed to get active shops", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : "Failed to get active shops"));
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
}
