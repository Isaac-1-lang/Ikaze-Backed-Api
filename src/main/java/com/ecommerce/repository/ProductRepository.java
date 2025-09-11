package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

        Optional<Product> findBySlug(String slug);

        Optional<Product> findBySku(String sku);

        @Query("select count(p) from Product p where p.id in " +
                        "(select s.product.id from Stock s where s.product is not null and s.quantity > 0 and s.quantity <= s.lowStockThreshold)")
        long countLowStock();

        @Query("select count(p) from Product p where p.isActive = true")
        long countActive();

        @Query("select count(p) from Product p where p.category = :category and p.isActive = true")
        long countByCategoryAndIsActiveTrue(@Param("category") Category category);

        @Query("select count(p) from Product p where p.brand = :brand and p.isActive = true")
        long countByBrandAndIsActiveTrue(@Param("brand") Brand brand);

        // Search suggestion methods
        @Query("SELECT p FROM Product p LEFT JOIN p.productDetail pd " +
                        "WHERE (LOWER(p.productName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(pd.metaKeywords) LIKE LOWER(CONCAT('%', :metaQuery, '%'))) " +
                        "AND p.isActive = true ORDER BY p.productName")
        List<Product> findTop10ByProductNameContainingIgnoreCaseOrProductDetail_MetaKeywordsContainingIgnoreCase(
                        @Param("query") String query, @Param("metaQuery") String metaQuery);

        @Query("SELECT DISTINCT pd.metaKeywords FROM ProductDetail pd " +
                        "WHERE pd.metaKeywords IS NOT NULL " +
                        "AND LOWER(pd.metaKeywords) LIKE LOWER(CONCAT('%', :query, '%'))")
        List<String> findDistinctMetaKeywordsByQuery(@Param("query") String query);
}
