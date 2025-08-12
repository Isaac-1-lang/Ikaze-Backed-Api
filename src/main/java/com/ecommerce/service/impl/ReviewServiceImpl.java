package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateReviewDTO;
import com.ecommerce.dto.ReviewDTO;
import com.ecommerce.dto.ReviewSearchDTO;
import com.ecommerce.dto.UpdateReviewDTO;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ReviewService;
import com.ecommerce.ServiceImpl.JwtService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public ReviewDTO createReview(String jwtToken, CreateReviewDTO createReviewDTO) {
        // Extract user info from JWT token
        String username = jwtService.extractUsername(jwtToken);
        User user = userRepository.findByUserEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check if user has already reviewed this product
        if (reviewRepository.existsByUserIdAndProduct_ProductId(user.getId(), createReviewDTO.getProductId())) {
            throw new IllegalArgumentException("You have already reviewed this product");
        }

        // Find the product
        Product product = productRepository.findById(createReviewDTO.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        // Create new review
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(createReviewDTO.getRating());
        review.setTitle(createReviewDTO.getTitle());
        review.setContent(createReviewDTO.getContent());
        review.setStatus(Review.ReviewStatus.PENDING); // Default to pending for moderation
        review.setVerifiedPurchase(checkIfVerifiedPurchase(user.getId(), product.getProductId()));

        Review savedReview = reviewRepository.save(review);
        return mapReviewToDTO(savedReview, user.getId());
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(String jwtToken, UpdateReviewDTO updateReviewDTO) {
        // Extract user info from JWT token
        String username = jwtService.extractUsername(jwtToken);
        User user = userRepository.findByUserEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Find the review
        Review review = reviewRepository.findById(updateReviewDTO.getReviewId())
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        // Check if user can edit this review
        if (!canEditReview(jwtToken, updateReviewDTO.getReviewId())) {
            throw new IllegalArgumentException("You are not authorized to edit this review");
        }

        // Update fields if provided
        if (updateReviewDTO.getRating() != null) {
            review.setRating(updateReviewDTO.getRating());
        }
        if (updateReviewDTO.getTitle() != null) {
            review.setTitle(updateReviewDTO.getTitle());
        }
        if (updateReviewDTO.getContent() != null) {
            review.setContent(updateReviewDTO.getContent());
        }

        // Reset status to pending for re-moderation if content changed
        if (updateReviewDTO.getTitle() != null || updateReviewDTO.getContent() != null) {
            review.setStatus(Review.ReviewStatus.PENDING);
        }

        Review updatedReview = reviewRepository.save(review);
        return mapReviewToDTO(updatedReview, user.getId());
    }

    @Override
    @Transactional
    public boolean deleteReview(String jwtToken, Long reviewId) {
        // Extract user info from JWT token
        String username = jwtService.extractUsername(jwtToken);
        User user = userRepository.findByUserEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Find the review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        // Check if user can delete this review
        if (!canDeleteReview(jwtToken, reviewId)) {
            throw new IllegalArgumentException("You are not authorized to delete this review");
        }

        reviewRepository.delete(review);
        return true;
    }

    @Override
    @Transactional
    public ReviewDTO getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        return mapReviewToDTO(review, null);
    }

    @Override
    @Transactional
    public Page<ReviewDTO> getProductReviews(UUID productId, int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Review> reviews = reviewRepository.findByProduct_ProductIdAndStatus(productId,
                Review.ReviewStatus.APPROVED, pageable);
        return reviews.map(review -> mapReviewToDTO(review, null));
    }

    @Override
    @Transactional
    public Page<ReviewDTO> getUserReviews(String jwtToken, int page, int size) {
        // Extract user info from JWT token
        String username = jwtService.extractUsername(jwtToken);
        User user = userRepository.findByUserEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = reviewRepository.findByUserId(user.getId(), pageable);
        return reviews.map(review -> mapReviewToDTO(review, user.getId()));
    }

    @Override
    @Transactional
    public Page<ReviewDTO> searchReviews(ReviewSearchDTO searchDTO) {
        if (!searchDTO.hasAtLeastOneFilter()) {
            throw new IllegalArgumentException("At least one filter criterion must be provided");
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(searchDTO.getSortDirection()) ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortBy = searchDTO.getSortBy() != null ? searchDTO.getSortBy() : "createdAt";
        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(),
                Sort.by(direction, sortBy));

        Specification<Review> spec = buildReviewSearchSpecification(searchDTO);
        Page<Review> reviews = reviewRepository.findAll(spec, pageable);
        return reviews.map(review -> mapReviewToDTO(review, null));
    }

    @Override
    @Transactional
    public Map<String, Object> getProductReviewStats(UUID productId) {
        Double averageRating = reviewRepository.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.getReviewCountByProductId(productId);
        List<Object[]> ratingDistribution = reviewRepository.getRatingDistributionByProductId(productId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", averageRating != null ? averageRating : 0.0);
        stats.put("reviewCount", reviewCount != null ? reviewCount : 0L);
        stats.put("ratingDistribution", ratingDistribution);

        return stats;
    }

    @Override
    @Transactional
    public boolean voteReview(String jwtToken, Long reviewId, boolean isHelpful) {
        // Extract user info from JWT token
        String username = jwtService.extractUsername(jwtToken);
        User user = userRepository.findByUserEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        if (isHelpful) {
            review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        } else {
            review.setNotHelpfulVotes(review.getNotHelpfulVotes() + 1);
        }

        reviewRepository.save(review);
        return true;
    }

    @Override
    @Transactional
    public ReviewDTO moderateReview(String jwtToken, Long reviewId, String status, String moderatorNotes) {
        // Extract user info from JWT token
        String username = jwtService.extractUsername(jwtToken);
        User user = userRepository.findByUserEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Check if user has admin or employee role
        if (!hasAdminOrEmployeeRole(jwtToken)) {
            throw new IllegalArgumentException("Only admins and employees can moderate reviews");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        review.setStatus(Review.ReviewStatus.valueOf(status.toUpperCase()));
        review.setModeratorNotes(moderatorNotes);
        review.setModeratedBy(user.getUserEmail());
        review.setModeratedAt(LocalDateTime.now());

        Review moderatedReview = reviewRepository.save(review);
        return mapReviewToDTO(moderatedReview, null);
    }

    @Override
    public boolean canEditReview(String jwtToken, Long reviewId) {
        try {
            String username = jwtService.extractUsername(jwtToken);
            User user = userRepository.findByUserEmail(username)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review not found"));

            // User can edit if they are the author or have admin/employee role
            return review.getUser().getId().equals(user.getId()) || hasAdminOrEmployeeRole(jwtToken);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canDeleteReview(String jwtToken, Long reviewId) {
        try {
            String username = jwtService.extractUsername(jwtToken);
            User user = userRepository.findByUserEmail(username)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new EntityNotFoundException("Review not found"));

            // User can delete if they are the author or have admin/employee role
            return review.getUser().getId().equals(user.getId()) || hasAdminOrEmployeeRole(jwtToken);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkIfVerifiedPurchase(UUID userId, UUID productId) {
        // TODO: Implement logic to check if user has purchased this product
        // This would involve checking the order history
        return false; // For now, return false
    }

    private boolean hasAdminOrEmployeeRole(String jwtToken) {
        try {
            String username = jwtService.extractUsername(jwtToken);
            User user = userRepository.findByUserEmail(username)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            return user.getRole().name().equals("ADMIN") || user.getRole().name().equals("EMPLOYEE");
        } catch (Exception e) {
            return false;
        }
    }

    private Specification<Review> buildReviewSearchSpecification(ReviewSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDTO.getProductId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("product").get("id"), searchDTO.getProductId()));
            }

            if (searchDTO.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), searchDTO.getUserId()));
            }

            if (searchDTO.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), searchDTO.getMinRating()));
            }

            if (searchDTO.getMaxRating() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("rating"), searchDTO.getMaxRating()));
            }

            if (searchDTO.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"),
                        Review.ReviewStatus.valueOf(searchDTO.getStatus().toUpperCase())));
            }

            if (searchDTO.getIsVerifiedPurchase() != null) {
                predicates
                        .add(criteriaBuilder.equal(root.get("isVerifiedPurchase"), searchDTO.getIsVerifiedPurchase()));
            }

            if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().trim().isEmpty()) {
                String keyword = "%" + searchDTO.getKeyword().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), keyword)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ReviewDTO mapReviewToDTO(Review review, UUID currentUserId) {
        boolean canEdit = currentUserId != null && canEditReview(null, review.getId());
        boolean canDelete = currentUserId != null && canDeleteReview(null, review.getId());

        return ReviewDTO.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .userEmail(review.getUser().getUserEmail())
                .productId(review.getProduct().getProductId())
                .productName(review.getProduct().getProductName())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .status(review.getStatus().name())
                .isVerifiedPurchase(review.isVerifiedPurchase())
                .helpfulVotes(review.getHelpfulVotes())
                .notHelpfulVotes(review.getNotHelpfulVotes())
                .moderatorNotes(review.getModeratorNotes())
                .moderatedBy(review.getModeratedBy())
                .moderatedAt(review.getModeratedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .canEdit(canEdit)
                .canDelete(canDelete)
                .build();
    }
}
