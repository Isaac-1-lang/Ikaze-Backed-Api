package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageBrandDTO {

    private UUID brandId;
    private String brandName;
    private String description;
    private String logoUrl;
    private String slug;
    private Long productCount;
    private Boolean isActive;
    private Boolean isFeatured;
    private String colorCode; // For brands without logos

    // Helper method to get display color if no logo
    public String getDisplayColor() {
        if (logoUrl != null && !logoUrl.trim().isEmpty()) {
            return null; // Has logo, no color needed
        }

        // Generate consistent color based on brand name
        if (colorCode != null && !colorCode.trim().isEmpty()) {
            return colorCode;
        }

        // Default color palette for brands without logos
        String[] colors = {
                "#E74C3C", "#3498DB", "#2ECC71", "#F39C12",
                "#9B59B6", "#1ABC9C", "#E67E22", "#34495E"
        };

        int colorIndex = Math.abs(brandName.hashCode()) % colors.length;
        return colors[colorIndex];
    }
}
