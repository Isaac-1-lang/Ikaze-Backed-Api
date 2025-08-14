package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @Column(nullable = false)
    private Integer rating;

    @NotBlank(message = "Review title is required")
    @Size(min = 5, max = 100, message = "Review title must be between 5 and 100 characters")
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank(message = "Review content is required")
    @Size(min = 10, max = 1000, message = "Review content must be between 10 and 1000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "is_verified_purchase")
    private boolean isVerifiedPurchase = false;

    @Column(name = "helpful_votes")
    private Integer helpfulVotes = 0;

    @Column(name = "not_helpful_votes")
    private Integer notHelpfulVotes = 0;

    @Column(name = "moderator_notes")
    private String moderatorNotes;

    @Column(name = "moderated_by")
    private String moderatedBy;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return status == ReviewStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == ReviewStatus.REJECTED;
    }

    public boolean isPending() {
        return status == ReviewStatus.PENDING;
    }

    public int getTotalVotes() {
        return helpfulVotes + notHelpfulVotes;
    }

    public double getHelpfulPercentage() {
        if (getTotalVotes() == 0) {
            return 0.0;
        }
        return (double) helpfulVotes / getTotalVotes() * 100;
    }

    public enum ReviewStatus {
        PENDING, APPROVED, REJECTED
    }

    /**
     * Gets the rating
     * 
     * @return The rating
     */
    public Integer getRating() {
        return rating;
    }
}