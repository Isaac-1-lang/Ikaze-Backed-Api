package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageCategoryDTO {

    private Long categoryId;
    private String categoryName;
    private String description;
    private String imageUrl;
    private String slug;
    private Long productCount;
    private Boolean isActive;
    private Boolean isFeatured;
    private String colorCode; // For categories without images

    // Helper method to get display color if no image
    public String getDisplayColor() {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return null; // Has image, no color needed
        }

        // Generate consistent color based on category name
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            return colorCode;
        }

        // Default color palette for categories without images
        String[] colors = {
                "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
                "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F"
        };

        int colorIndex = Math.abs(categoryName.hashCode()) % colors.length;
        return colors[colorIndex];
    }
}
