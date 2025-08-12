package com.ecommerce.service;

import com.ecommerce.dto.AddToCartDTO;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.UpdateCartItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CartService {

    /**
     * Add an item to the user's cart
     */
    CartItemDTO addToCart(UUID userId, AddToCartDTO addToCartDTO);

    /**
     * Update the quantity of a cart item
     */
    CartItemDTO updateCartItem(UUID userId, UpdateCartItemDTO updateCartItemDTO);

    /**
     * Remove an item from the cart
     */
    boolean removeFromCart(UUID userId, Long cartItemId);

    /**
     * View the user's cart with pagination
     */
    CartDTO viewCart(UUID userId, Pageable pageable);

    /**
     * Clear all items from the user's cart
     */
    boolean clearCart(UUID userId);

    /**
     * Get cart item by ID for the user
     */
    CartItemDTO getCartItem(UUID userId, Long cartItemId);

    /**
     * Check if user has items in cart
     */
    boolean hasItems(UUID userId);
}

