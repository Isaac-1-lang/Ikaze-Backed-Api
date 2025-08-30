package com.ecommerce.controller;

import com.ecommerce.dto.AddToWishlistDTO;
import com.ecommerce.dto.WishlistDTO;
import com.ecommerce.dto.WishlistProductDTO;
import com.ecommerce.dto.UpdateWishlistProductDTO;
import com.ecommerce.service.WishlistService;
import com.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wishlist Management", description = "APIs for managing user wishlists")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Add product to wishlist", description = "Add a product variant to the user's wishlist")
    public ResponseEntity<?> addToWishlist(@Valid @RequestBody AddToWishlistDTO addToWishlistDTO) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Adding product to wishlist for user: {}", userId);
            WishlistProductDTO wishlistProduct = wishlistService.addToWishlist(userId, addToWishlistDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("su    ccess", true);
            response.put("message", "Product added to wishlist successfully");
            response.put("data", wishlistProduct);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to add product to wishlist: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while adding product to wishlist: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error adding product to wishlist: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to add product to wishlist");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Update wishlist product", description = "Update the notes or priority of a wishlist product")
    public ResponseEntity<?> updateWishlistProduct(
            @Valid @RequestBody UpdateWishlistProductDTO updateWishlistProductDTO) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Updating wishlist product for user: {}", userId);
            WishlistProductDTO wishlistProduct = wishlistService.updateWishlistProduct(userId,
                    updateWishlistProductDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wishlist product updated successfully");
            response.put("data", wishlistProduct);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to update wishlist product: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while updating wishlist product: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error updating wishlist product: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update wishlist product");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/remove/{wishlistProductId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Remove product from wishlist", description = "Remove a specific product from the user's wishlist")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long wishlistProductId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Removing product from wishlist for user: {}, product: {}", userId, wishlistProductId);
            boolean removed = wishlistService.removeFromWishlist(userId, wishlistProductId);

            if (removed) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Product removed from wishlist successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to remove product from wishlist");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while removing product from wishlist: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error removing product from wishlist: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to remove product from wishlist");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/view")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "View wishlist", description = "View the user's wishlist with pagination")
    public ResponseEntity<?> viewWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "addedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Viewing wishlist for user: {}, page: {}, size: {}", userId, page, size);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            WishlistDTO wishlist = wishlistService.viewWishlist(userId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wishlist retrieved successfully");
            response.put("data", wishlist);
            response.put("pagination", Map.of(
                    "page", page,
                    "size", size,
                    "totalElements", wishlist.getTotalProducts(),
                    "totalPages", (int) Math.ceil((double) wishlist.getTotalProducts() / size)));

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while viewing wishlist: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error viewing wishlist: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve wishlist");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Clear wishlist", description = "Remove all products from the user's wishlist")
    public ResponseEntity<?> clearWishlist() {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Clearing wishlist for user: {}", userId);
            boolean cleared = wishlistService.clearWishlist(userId);

            if (cleared) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Wishlist cleared successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to clear wishlist");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while clearing wishlist: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error clearing wishlist: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to clear wishlist");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/product/{wishlistProductId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Get wishlist product", description = "Get details of a specific wishlist product")
    public ResponseEntity<?> getWishlistProduct(@PathVariable Long wishlistProductId) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Getting wishlist product for user: {}, product: {}", userId, wishlistProductId);
            WishlistProductDTO wishlistProduct = wishlistService.getWishlistProduct(userId, wishlistProductId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wishlist product retrieved successfully");
            response.put("data", wishlistProduct);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while getting wishlist product: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error getting wishlist product: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve wishlist product");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/has-products")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Check if wishlist has products", description = "Check if the user's wishlist contains any products")
    public ResponseEntity<?> hasProducts() {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Checking if user has products in wishlist: {}", userId);
            boolean hasProducts = wishlistService.hasProducts(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wishlist status checked successfully");
            response.put("data", Map.of("hasProducts", hasProducts));

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while checking wishlist products: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error checking wishlist products: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to check wishlist products");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/move-to-cart/{wishlistProductId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','EMPLOYEE')")
    @Operation(summary = "Move product to cart", description = "Move a product from wishlist to cart and remove it from wishlist if it has no variants")
    public ResponseEntity<?> moveToCart(
            @PathVariable Long wishlistProductId,
            @RequestParam(defaultValue = "1") int quantity) {
        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "User not authenticated"));
            }

            log.info("Moving product from wishlist to cart for user: {}, product: {}, quantity: {}", userId,
                    wishlistProductId, quantity);
            boolean moved = wishlistService.moveToCart(userId, wishlistProductId, quantity);

            if (moved) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Product moved to cart successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to move product to cart");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while moving product to cart: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error moving product to cart: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to move product to cart");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }

            Object principal = auth.getPrincipal();

            // If principal is CustomUserDetails, extract email and find user
            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            // If principal is User entity
            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                return user.getId();
            }

            // If principal is UserDetails
            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            // Fallback to auth name
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name).map(com.ecommerce.entity.User::getId).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
        }
        return null;
    }
}
