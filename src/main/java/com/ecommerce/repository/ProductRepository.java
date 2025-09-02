package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    @Query("select count(p) from Product p where p.stockQuantity > 0 and p.stockQuantity <= p.lowStockThreshold")
    long countLowStock();

    @Query("select count(p) from Product p where p.isActive = true")
    long countActive();

    @Query("select count(p) from Product p where p.category = :category and p.isActive = true")
    long countByCategoryAndIsActiveTrue(@Param("category") Category category);

    @Query("select count(p) from Product p where p.brand = :brand and p.isActive = true")
    long countByBrandAndIsActiveTrue(@Param("brand") Brand brand);
}
