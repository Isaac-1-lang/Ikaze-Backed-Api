package com.ecommerce.repository;

import com.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    /**
     * Find reviews by product ID with pagination
     */
    Page<Review> findByProduct_ProductId(UUID productId, Pageable pageable);

    /**
     * Find reviews by user ID with pagination
     */
    Page<Review> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find reviews by product ID and status
     */
    Page<Review> findByProduct_ProductIdAndStatus(UUID productId, Review.ReviewStatus status, Pageable pageable);

    /**
     * Find review by user ID and product ID
     */
    Optional<Review> findByUserIdAndProduct_ProductId(UUID userId, UUID productId);

    /**
     * Check if user has reviewed a product
     */
    boolean existsByUserIdAndProduct_ProductId(UUID userId, UUID productId);

    /**
     * Find reviews by status
     */
    Page<Review> findByStatus(Review.ReviewStatus status, Pageable pageable);

    /**
     * Find verified purchase reviews by product ID
     */
    Page<Review> findByProduct_ProductIdAndIsVerifiedPurchaseTrue(UUID productId, Pageable pageable);

    /**
     * Get average rating for a product
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId AND r.status = 'APPROVED'")
    Double getAverageRatingByProductId(@Param("productId") UUID productId);

    /**
     * Get review count for a product
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.productId = :productId AND r.status = 'APPROVED'")
    Long getReviewCountByProductId(@Param("productId") UUID productId);

    /**
     * Get rating distribution for a product
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.productId = :productId AND r.status = 'APPROVED' GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") UUID productId);

    /**
     * Find reviews by keyword in title or content
     */
    @Query("SELECT r FROM Review r WHERE (LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND r.status = 'APPROVED'")
    Page<Review> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find reviews by rating range
     */
    @Query("SELECT r FROM Review r WHERE r.rating BETWEEN :minRating AND :maxRating AND r.status = 'APPROVED'")
    Page<Review> findByRatingRange(@Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating,
            Pageable pageable);
}
