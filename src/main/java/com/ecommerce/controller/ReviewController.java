package com.ecommerce.controller;

import com.ecommerce.dto.CreateReviewDTO;
import com.ecommerce.dto.ReviewDTO;
import com.ecommerce.dto.ReviewSearchDTO;
import com.ecommerce.dto.UpdateReviewDTO;
import com.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review Management", description = "APIs for managing product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Create a review", description = "Create a new product review")
    public ResponseEntity<?> createReview(HttpServletRequest request,
            @Valid @RequestBody CreateReviewDTO createReviewDTO) {
        try {
            String jwtToken = extractJwtToken(request);
            log.info("Creating review for product: {}", createReviewDTO.getProductId());

            ReviewDTO review = reviewService.createReview(jwtToken, createReviewDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review created successfully");
            response.put("data", review);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to create review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while creating review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create review");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Update a review", description = "Update an existing review")
    public ResponseEntity<?> updateReview(HttpServletRequest request,
            @Valid @RequestBody UpdateReviewDTO updateReviewDTO) {
        try {
            String jwtToken = extractJwtToken(request);
            log.info("Updating review: {}", updateReviewDTO.getReviewId());

            ReviewDTO review = reviewService.updateReview(jwtToken, updateReviewDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review updated successfully");
            response.put("data", review);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to update review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while updating review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error updating review: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update review");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Delete a review", description = "Delete a review")
    public ResponseEntity<?> deleteReview(HttpServletRequest request, @PathVariable Long reviewId) {
        try {
            String jwtToken = extractJwtToken(request);
            log.info("Deleting review: {}", reviewId);

            boolean deleted = reviewService.deleteReview(jwtToken, reviewId);

            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Review deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to delete review");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to delete review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while deleting review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error deleting review: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete review");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get a review", description = "Get a specific review by ID")
    public ResponseEntity<?> getReview(@PathVariable Long reviewId) {
        try {
            log.info("Getting review: {}", reviewId);

            ReviewDTO review = reviewService.getReview(reviewId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review retrieved successfully");
            response.put("data", review);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("Review not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error getting review: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve review");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get product reviews", description = "Get all reviews for a specific product")
    public ResponseEntity<?> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            log.info("Getting reviews for product: {}, page: {}, size: {}", productId, page, size);

            Page<ReviewDTO> reviews = reviewService.getProductReviews(productId, page, size, sortBy, sortDirection);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product reviews retrieved successfully");
            response.put("data", reviews.getContent());
            response.put("pagination", Map.of(
                    "page", page,
                    "size", size,
                    "totalElements", reviews.getTotalElements(),
                    "totalPages", reviews.getTotalPages()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting product reviews: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve product reviews");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Get user reviews", description = "Get all reviews by the authenticated user")
    public ResponseEntity<?> getUserReviews(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String jwtToken = extractJwtToken(request);
            log.info("Getting user reviews, page: {}, size: {}", page, size);

            Page<ReviewDTO> reviews = reviewService.getUserReviews(jwtToken, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User reviews retrieved successfully");
            response.put("data", reviews.getContent());
            response.put("pagination", Map.of(
                    "page", page,
                    "size", size,
                    "totalElements", reviews.getTotalElements(),
                    "totalPages", reviews.getTotalPages()));

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("User not found: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error getting user reviews: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve user reviews");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/search")
    @Operation(summary = "Search reviews", description = "Search and filter reviews")
    public ResponseEntity<?> searchReviews(@Valid @RequestBody ReviewSearchDTO searchDTO) {
        try {
            log.info("Searching reviews with filters: {}", searchDTO);

            Page<ReviewDTO> reviews = reviewService.searchReviews(searchDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reviews search completed successfully");
            response.put("data", reviews.getContent());
            response.put("pagination", Map.of(
                    "page", searchDTO.getPage(),
                    "size", searchDTO.getSize(),
                    "totalElements", reviews.getTotalElements(),
                    "totalPages", reviews.getTotalPages()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid search request: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error searching reviews: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to search reviews");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/product/{productId}/stats")
    @Operation(summary = "Get product review stats", description = "Get review statistics for a product")
    public ResponseEntity<?> getProductReviewStats(@PathVariable UUID productId) {
        try {
            log.info("Getting review stats for product: {}", productId);

            Map<String, Object> stats = reviewService.getProductReviewStats(productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product review stats retrieved successfully");
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting product review stats: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve product review stats");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{reviewId}/vote")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Vote on a review", description = "Vote helpful or not helpful on a review")
    public ResponseEntity<?> voteReview(
            HttpServletRequest request,
            @PathVariable Long reviewId,
            @RequestParam boolean isHelpful) {
        try {
            String jwtToken = extractJwtToken(request);
            log.info("Voting on review: {}, isHelpful: {}", reviewId, isHelpful);

            boolean voted = reviewService.voteReview(jwtToken, reviewId, isHelpful);

            if (voted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Vote recorded successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to record vote");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while voting: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error voting on review: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to record vote");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{reviewId}/moderate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Moderate a review", description = "Moderate a review (admin/employee only)")
    public ResponseEntity<?> moderateReview(
            HttpServletRequest request,
            @PathVariable Long reviewId,
            @RequestParam String status,
            @RequestParam(required = false) String moderatorNotes) {
        try {
            String jwtToken = extractJwtToken(request);
            log.info("Moderating review: {}, status: {}", reviewId, status);

            ReviewDTO review = reviewService.moderateReview(jwtToken, reviewId, status, moderatorNotes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review moderated successfully");
            response.put("data", review);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid moderation request: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.warn("Entity not found while moderating: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error moderating review: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to moderate review");
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Authorization header with Bearer token is required");
    }
}
