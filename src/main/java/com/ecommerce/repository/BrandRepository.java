package com.ecommerce.repository;

import com.ecommerce.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID>, JpaSpecificationExecutor<Brand> {

    Optional<Brand> findBySlug(String slug);

    Optional<Brand> findByBrandName(String brandName);

    List<Brand> findByIsActiveTrue();

    List<Brand> findByIsFeaturedTrue();

    // Search suggestion method
    List<Brand> findTop5ByBrandNameContainingIgnoreCase(String brandName);
}
