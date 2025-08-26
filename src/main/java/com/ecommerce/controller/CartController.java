package com.ecommerce.controller;

import com.ecommerce.dto.AddToCartDTO;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.UpdateCartItemDTO;
import com.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart Management", description = "APIs for managing shopping cart operations")
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
   @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','CUSTOMER')")
    @Operation(summary = "Add item to cart", description = "Add a product variant to the user's shopping cart")
    public ResponseEntity<?> addToCart(@RequestParam UUID userId, @Valid @RequestBody AddToCartDTO addToCartDTO) {
        try {
            log.info("Adding item to cart for user: {}", userId);
            CartItemDTO cartItem = cartService.addToCart(userId, addToCartDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item added to cart successfully");
            response.put("data", cartItem);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to add item to cart: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while adding item to cart: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            log.error("Error adding item to cart: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to add item to cart");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Update cart item quantity", description = "Update the quantity of an item in the user's cart")
    public ResponseEntity<?> updateCartItem(@RequestParam UUID userId, @Valid @RequestBody UpdateCartItemDTO updateCartItemDTO) {
        try {
            log.info("Updating cart item for user: {}", userId);
            CartItemDTO cartItem = cartService.updateCartItem(userId, updateCartItemDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart item updated successfully");
            response.put("data", cartItem);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to update cart item: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while updating cart item: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            log.error("Error updating cart item: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update cart item");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/remove/{cartItemId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Remove item from cart", description = "Remove a specific item from the user's cart")
    public ResponseEntity<?> removeFromCart(@RequestParam UUID userId, @PathVariable Long cartItemId) {
        try {
            log.info("Removing cart item for user: {}, item: {}", userId, cartItemId);
            boolean removed = cartService.removeFromCart(userId, cartItemId);
            
            if (removed) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Item removed from cart successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to remove item from cart");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while removing cart item: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            log.error("Error removing cart item: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to remove item from cart");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/view")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','CUSTOMER')")
    @Operation(summary = "View cart", description = "View the user's cart with pagination")
    public ResponseEntity<?> viewCart(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "addedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            log.info("Viewing cart for user: {}, page: {}, size: {}", userId, page, size);
            
            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            CartDTO cart = cartService.viewCart(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart retrieved successfully");
            response.put("data", cart);
            response.put("pagination", Map.of(
                "page", page,
                "size", size,
                "totalElements", cart.getTotalItems(),
                "totalPages", (int) Math.ceil((double) cart.getTotalItems() / size)
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while viewing cart: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            log.error("Error viewing cart: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve cart");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Clear cart", description = "Remove all items from the user's cart")
    public ResponseEntity<?> clearCart(@RequestParam UUID userId) {
        try {
            log.info("Clearing cart for user: {}", userId);
            boolean cleared = cartService.clearCart(userId);
            
            if (cleared) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Cart cleared successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to clear cart");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while clearing cart: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            log.error("Error clearing cart: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to clear cart");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/item/{cartItemId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Get cart item", description = "Get details of a specific cart item")
    public ResponseEntity<?> getCartItem(@RequestParam UUID userId, @PathVariable Long cartItemId) {
        try {
            log.info("Getting cart item for user: {}, item: {}", userId, cartItemId);
            CartItemDTO cartItem = cartService.getCartItem(userId, cartItemId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart item retrieved successfully");
            response.put("data", cartItem);
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while getting cart item: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            log.error("Error getting cart item: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve cart item");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/has-items")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Check if cart has items", description = "Check if the user's cart contains any items")
    public ResponseEntity<?> hasItems(@RequestParam UUID userId) {
        try {
            log.info("Checking if user has items in cart: {}", userId);
            boolean hasItems = cartService.hasItems(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart status checked successfully");
            response.put("data", Map.of("hasItems", hasItems));
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while checking cart items: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            log.error("Error checking cart items: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to check cart items");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
