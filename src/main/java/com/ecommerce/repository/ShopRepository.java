package com.ecommerce.repository;

import com.ecommerce.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {
    
    Optional<Shop> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    
    List<Shop> findByOwnerId(UUID ownerId);
    
    @Query("SELECT s FROM Shop s WHERE s.owner.id = :ownerId")
    List<Shop> findByOwner(@Param("ownerId") UUID ownerId);
    
    @Query("SELECT s FROM Shop s WHERE s.status = :status")
    List<Shop> findByStatus(@Param("status") Shop.ShopStatus status);
    
    @Query("SELECT s FROM Shop s WHERE s.isActive = true AND s.status = 'ACTIVE'")
    List<Shop> findActiveShops();
    
    boolean existsByOwnerIdAndName(UUID ownerId, String name);
}

