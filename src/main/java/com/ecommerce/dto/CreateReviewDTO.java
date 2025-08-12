package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewDTO {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotBlank(message = "Review title is required")
    @Size(min = 5, max = 100, message = "Review title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Review content is required")
    @Size(min = 10, max = 1000, message = "Review content must be between 10 and 1000 characters")
    private String content;
}
