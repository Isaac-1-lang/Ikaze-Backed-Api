package com.ecommerce.repository;

import com.ecommerce.entity.RewardSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardSystemRepository extends JpaRepository<RewardSystem, Long> {

    /**
     * Find the active reward system configuration
     */
    Optional<RewardSystem> findByIsActiveTrue();

    /**
     * Check if there's an active reward system
     */
    boolean existsByIsActiveTrue();

    /**
     * Find reward system by description
     */
    Optional<RewardSystem> findByDescription(String description);
}
