package com.ecommerce.service;

import com.ecommerce.dto.AddToWishlistDTO;
import com.ecommerce.dto.WishlistDTO;
import com.ecommerce.dto.WishlistProductDTO;
import com.ecommerce.dto.UpdateWishlistProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WishlistService {

    /**
     * Add a product to the user's wishlist
     */
    WishlistProductDTO addToWishlist(UUID userId, AddToWishlistDTO addToWishlistDTO);

    /**
     * Update the notes or priority of a wishlist product
     */
    WishlistProductDTO updateWishlistProduct(UUID userId, UpdateWishlistProductDTO updateWishlistProductDTO);

    /**
     * Remove a product from the wishlist
     */
    boolean removeFromWishlist(UUID userId, Long wishlistProductId);

    /**
     * View the user's wishlist with pagination
     */
    WishlistDTO viewWishlist(UUID userId, Pageable pageable);

    /**
     * Clear all products from the user's wishlist
     */
    boolean clearWishlist(UUID userId);

    /**
     * Get wishlist product by ID for the user
     */
    WishlistProductDTO getWishlistProduct(UUID userId, Long wishlistProductId);

    /**
     * Check if user has products in wishlist
     */
    boolean hasProducts(UUID userId);

    /**
     * Move product from wishlist to cart
     */
    boolean moveToCart(UUID userId, Long wishlistProductId, int quantity);
}
