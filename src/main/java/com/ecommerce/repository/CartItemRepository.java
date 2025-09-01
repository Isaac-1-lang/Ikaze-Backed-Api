package com.ecommerce.repository;

import com.ecommerce.entity.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find cart items by cart ID with pagination
     */
    Page<CartItem> findByCartId(Long cartId, Pageable pageable);

    /**
     * Find cart items by cart ID
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * Find cart item by cart ID and variant ID
     */
    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long variantId);

    /**
     * Find cart item by cart ID and product ID
     */
    Optional<CartItem> findByCartIdAndProductProductId(Long cartId, UUID productId);

    /**
     * Check if cart item exists by cart ID and variant ID
     */
    boolean existsByCartIdAndProductVariantId(Long cartId, Long variantId);

    /**
     * Check if cart item exists by cart ID and product ID
     */
    boolean existsByCartIdAndProductProductId(Long cartId, UUID productId);

    /**
     * Delete cart item by ID
     */
    void deleteById(Long cartItemId);

    /**
     * Delete cart items by cart ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);

    /**
     * Count items in a cart
     */
    long countByCartId(Long cartId);
}
