package com.ecommerce.repository;

import com.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find cart by user ID
     */
    Optional<Cart> findByUserId(UUID userId);

    /**
     * Remove cart items by product variant ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.productVariant.id = :variantId")
    void deleteCartItemsByProductVariant(@Param("variantId") Long variantId);
}
