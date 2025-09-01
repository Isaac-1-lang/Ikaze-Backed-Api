package com.ecommerce.repository;

import com.ecommerce.entity.WishlistProduct;
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
public interface WishlistProductRepository extends JpaRepository<WishlistProduct, Long> {

    /**
     * Find wishlist products by wishlist ID with pagination
     */
    Page<WishlistProduct> findByWishlistId(Long wishlistId, Pageable pageable);

    /**
     * Find wishlist products by wishlist ID
     */
    List<WishlistProduct> findByWishlistId(Long wishlistId);

    /**
     * Find wishlist product by wishlist ID and product ID
     */
    Optional<WishlistProduct> findByWishlistIdAndProductProductId(Long wishlistId, UUID productId);

    /**
     * Check if wishlist product exists by wishlist ID and product ID
     */
    boolean existsByWishlistIdAndProductProductId(Long wishlistId, UUID productId);

    /**
     * Delete wishlist product by ID
     */
    void deleteById(Long wishlistProductId);

    /**
     * Delete wishlist products by wishlist ID
     */
    @Modifying
    @Query("DELETE FROM WishlistProduct wp WHERE wp.wishlist.id = :wishlistId")
    void deleteByWishlistId(@Param("wishlistId") Long wishlistId);

    /**
     * Count products in a wishlist
     */
    long countByWishlistId(Long wishlistId);
}
