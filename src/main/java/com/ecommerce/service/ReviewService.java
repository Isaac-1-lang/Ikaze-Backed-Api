package com.ecommerce.service;

import com.ecommerce.dto.CreateReviewDTO;
import com.ecommerce.dto.ReviewDTO;
import com.ecommerce.dto.ReviewSearchDTO;
import com.ecommerce.dto.UpdateReviewDTO;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.UUID;

public interface ReviewService {

    /**
     * Create a new review
     */
    ReviewDTO createReview(String jwtToken, CreateReviewDTO createReviewDTO);

    /**
     * Update an existing review
     */
    ReviewDTO updateReview(String jwtToken, UpdateReviewDTO updateReviewDTO);

    /**
     * Delete a review
     */
    boolean deleteReview(String jwtToken, Long reviewId);

    /**
     * Get a specific review by ID
     */
    ReviewDTO getReview(Long reviewId);

    /**
     * Get reviews for a specific product
     */
    Page<ReviewDTO> getProductReviews(UUID productId, int page, int size, String sortBy, String sortDirection);

    /**
     * Get reviews by the authenticated user
     */
    Page<ReviewDTO> getUserReviews(String jwtToken, int page, int size);

    /**
     * Search and filter reviews
     */
    Page<ReviewDTO> searchReviews(ReviewSearchDTO searchDTO);

    /**
     * Get review statistics for a product
     */
    Map<String, Object> getProductReviewStats(UUID productId);

    /**
     * Vote on a review (helpful/not helpful)
     */
    boolean voteReview(String jwtToken, Long reviewId, boolean isHelpful);

    /**
     * Moderate a review (admin/employee only)
     */
    ReviewDTO moderateReview(String jwtToken, Long reviewId, String status, String moderatorNotes);

    /**
     * Check if user can edit a review
     */
    boolean canEditReview(String jwtToken, Long reviewId);

    /**
     * Check if user can delete a review
     */
    boolean canDeleteReview(String jwtToken, Long reviewId);
}
