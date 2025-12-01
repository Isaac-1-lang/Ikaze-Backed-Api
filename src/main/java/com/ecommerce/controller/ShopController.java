package com.ecommerce.controller;

import com.ecommerce.dto.ShopDTO;
import com.ecommerce.service.ShopService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shops")
@Tag(name = "Shop Management", description = "Endpoints for managing shops")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ShopController {

    private final ShopService shopService;
    private final com.ecommerce.repository.UserRepository userRepository;

    @Autowired
    public ShopController(ShopService shopService, com.ecommerce.repository.UserRepository userRepository) {
        this.shopService = shopService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Create a new shop", description = "Create a new shop (Vendor/Admin only)")
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

