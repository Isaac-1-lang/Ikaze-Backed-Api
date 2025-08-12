package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private Long id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID productId;
    private String productName;
    private Integer rating;
    private String title;
    private String content;
    private String status;
    private boolean isVerifiedPurchase;
    private Integer helpfulVotes;
    private Integer notHelpfulVotes;
    private String moderatorNotes;
    private String moderatedBy;
    private LocalDateTime moderatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canEdit;
    private boolean canDelete;
}
