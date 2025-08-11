package com.ecommerce.repository;

import com.ecommerce.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * Find wishlist by user ID
     */
    Optional<Wishlist> findByUserId(UUID userId);

    /**
     * Remove wishlist products by product variant ID
     */
    @Modifying
    @Query("DELETE FROM WishlistProduct wp WHERE wp.productVariant.id = :variantId")
    void deleteWishlistProductsByProductVariant(@Param("variantId") Long variantId);
}
