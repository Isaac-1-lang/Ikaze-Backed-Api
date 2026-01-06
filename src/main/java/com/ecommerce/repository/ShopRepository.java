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

        @Query(value = "SELECT s.* FROM shops s " +
                        "JOIN users o ON o.id = s.owner_id " +
                        "WHERE s.is_active = true AND s.status = 'ACTIVE' " +
                        "AND (:category IS NULL OR s.category = :category) " +
                        "AND (:search IS NULL OR " +
                        "LOWER(COALESCE(CAST(s.shop_name AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                        +
                        "LOWER(COALESCE(CAST(s.description AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                        +
                        "LOWER(COALESCE(CAST(s.address AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                        +
                        "LOWER(COALESCE(CAST(o.first_name AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                        +
                        "LOWER(COALESCE(CAST(o.last_name AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')))", countQuery = "SELECT COUNT(*) FROM shops s "
                                        +
                                        "JOIN users o ON o.id = s.owner_id " +
                                        "WHERE s.is_active = true AND s.status = 'ACTIVE' " +
                                        "AND (:category IS NULL OR s.category = :category) " +
                                        "AND (:search IS NULL OR " +
                                        "LOWER(COALESCE(CAST(s.shop_name AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                                        +
                                        "LOWER(COALESCE(CAST(s.description AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                                        +
                                        "LOWER(COALESCE(CAST(s.address AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                                        +
                                        "LOWER(COALESCE(CAST(o.first_name AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')) OR "
                                        +
                                        "LOWER(COALESCE(CAST(o.last_name AS TEXT), '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%')))", nativeQuery = true)
        org.springframework.data.domain.Page<Shop> searchShops(
                        @Param("search") String search,
                        @Param("category") String category,
                        org.springframework.data.domain.Pageable pageable);

        boolean existsByOwnerIdAndName(UUID ownerId, String name);
}
