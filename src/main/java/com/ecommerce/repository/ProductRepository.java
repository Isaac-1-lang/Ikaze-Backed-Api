package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isFeatured = true ORDER BY p.createdAt DESC")
    List<Product> findFeaturedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isBestseller = true ORDER BY p.createdAt DESC")
    List<Product> findBestsellerProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isNewArrival = true ORDER BY p.createdAt DESC")
    List<Product> findNewArrivalProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isOnSale = true ORDER BY p.createdAt DESC")
    List<Product> findOnSaleProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
            "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "EXISTS (SELECT pd FROM ProductDetail pd WHERE pd.product = p AND " +
            "(LOWER(pd.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(pd.searchKeywords) LIKE LOWER(CONCAT('%', :keyword, '%')))))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
}
