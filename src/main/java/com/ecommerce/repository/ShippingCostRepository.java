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

@Repository
public interface ShippingCostRepository extends JpaRepository<ShippingCost, Long> {

    /**
     * Find all active shipping costs
     */
    List<ShippingCost> findByIsActiveTrue();

    /**
     * Find the first active shipping cost configuration
     */
    Optional<ShippingCost> findFirstByIsActiveTrue();

        /**
         * Find shipping costs by name (case insensitive)
         */
        Page<ShippingCost> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

        /**
         * Find shipping costs by name (case insensitive, including inactive)
         */
        Page<ShippingCost> findByNameContainingIgnoreCase(String name, Pageable pageable);

        /**
         * Check if a shipping cost with the same name exists (excluding current ID)
         */
        boolean existsByNameAndIdNot(String name, Long id);

        /**
         * Check if a shipping cost with the same name exists
         */
        boolean existsByName(String name);

    /**
     * Deactivate all shipping costs
     */
    @Modifying
    @Query("UPDATE ShippingCost sc SET sc.isActive = false")
    void deactivateAllShippingCosts();
}
