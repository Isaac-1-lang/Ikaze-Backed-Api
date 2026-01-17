package com.ecommerce.repository;

import com.ecommerce.entity.RewardSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RewardSystemRepository extends JpaRepository<RewardSystem, Long> {

    /**
     * Find the active reward system configuration for a shop
     */
    Optional<RewardSystem> findByShopShopIdAndIsActiveTrue(UUID shopId);

    /**
     * Find all reward systems for a shop
     */
    List<RewardSystem> findByShopShopId(UUID shopId);

    /**
     * Find all reward systems for a shop with pagination
     */
    Page<RewardSystem> findByShopShopId(UUID shopId, Pageable pageable);

    /**
     * Check if there's an active reward system for a shop
     */
    boolean existsByShopShopIdAndIsActiveTrue(UUID shopId);

    /**
     * Find reward system by description and shop
     */
    Optional<RewardSystem> findByDescriptionAndShopShopId(String description, UUID shopId);

    /**
     * Find reward system by ID and shop
     */
    Optional<RewardSystem> findByIdAndShopShopId(Long id, UUID shopId);

    /**
     * Find the active reward system configuration (for backward compatibility)
     * 
     * @deprecated Use findByShopShopIdAndIsActiveTrue instead
     */
    @Deprecated
    Optional<RewardSystem> findByIsActiveTrue();
}
