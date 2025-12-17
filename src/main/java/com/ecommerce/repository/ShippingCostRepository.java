package com.ecommerce.repository;

import com.ecommerce.entity.ShippingCost;
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
public interface ShippingCostRepository extends JpaRepository<ShippingCost, Long> {

    /**
     * Find all active shipping costs
     */
    List<ShippingCost> findByIsActiveTrue();

    /**
     * Find all active shipping costs for a shop
     */
    List<ShippingCost> findByShopShopIdAndIsActiveTrue(UUID shopId);

    /**
     * Find the first active shipping cost configuration
     */
    Optional<ShippingCost> findFirstByIsActiveTrue();

    /**
     * Find the first active shipping cost configuration for a shop
     */
    Optional<ShippingCost> findFirstByShopShopIdAndIsActiveTrue(UUID shopId);

    /**
     * Find all shipping costs for a shop
     */
    Page<ShippingCost> findByShopShopId(UUID shopId, Pageable pageable);

        /**
         * Find shipping costs by name (case insensitive)
         */
        Page<ShippingCost> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

        /**
         * Find shipping costs by name (case insensitive) for a shop
         */
        Page<ShippingCost> findByNameContainingIgnoreCaseAndShopShopIdAndIsActiveTrue(String name, UUID shopId, Pageable pageable);

        /**
         * Find shipping costs by name (case insensitive, including inactive)
         */
        Page<ShippingCost> findByNameContainingIgnoreCase(String name, Pageable pageable);

        /**
         * Find shipping costs by name (case insensitive, including inactive) for a shop
         */
        Page<ShippingCost> findByNameContainingIgnoreCaseAndShopShopId(String name, UUID shopId, Pageable pageable);

        /**
         * Check if a shipping cost with the same name exists (excluding current ID)
         */
        boolean existsByNameAndIdNot(String name, Long id);

        /**
         * Check if a shipping cost with the same name exists for a shop (excluding current ID)
         */
        boolean existsByNameAndShopShopIdAndIdNot(String name, UUID shopId, Long id);

        /**
         * Check if a shipping cost with the same name exists
         */
        boolean existsByName(String name);

        /**
         * Check if a shipping cost with the same name exists for a shop
         */
        boolean existsByNameAndShopShopId(String name, UUID shopId);

    /**
     * Deactivate all shipping costs
     */
    @Modifying
    @Query("UPDATE ShippingCost sc SET sc.isActive = false")
    void deactivateAllShippingCosts();

    /**
     * Deactivate all shipping costs for a shop
     */
    @Modifying
    @Query("UPDATE ShippingCost sc SET sc.isActive = false WHERE sc.shop.shopId = :shopId")
    void deactivateAllShippingCostsByShopId(@Param("shopId") UUID shopId);
}
