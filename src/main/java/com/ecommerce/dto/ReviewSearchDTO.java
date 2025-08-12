package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSearchDTO {

    private UUID productId;
    private Long userId;
    private Integer minRating;
    private Integer maxRating;
    private String status; // PENDING, APPROVED, REJECTED
    private Boolean isVerifiedPurchase;
    private String keyword; // Search in title and content
    private String sortBy; // rating, createdAt, helpfulVotes
    private String sortDirection; // asc, desc
    private Integer page = 0;
    private Integer size = 10;

    public boolean hasAtLeastOneFilter() {
        return productId != null || userId != null || minRating != null ||
                maxRating != null || status != null || isVerifiedPurchase != null ||
                keyword != null;
    }
}
