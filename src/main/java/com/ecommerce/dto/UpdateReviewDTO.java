package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewDTO {

    @NotNull(message = "Review ID is required")
    private Long reviewId;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(min = 5, max = 100, message = "Review title must be between 5 and 100 characters")
    private String title;

    @Size(min = 10, max = 1000, message = "Review content must be between 10 and 1000 characters")
    private String content;
}
