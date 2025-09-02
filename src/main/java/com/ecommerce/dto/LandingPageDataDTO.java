package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageDataDTO {

    private List<LandingPageProductDTO> topSellingProducts;
    private List<LandingPageProductDTO> newProducts;
    private List<LandingPageProductDTO> discountedProducts;
    private List<LandingPageCategoryDTO> popularCategories;
    private List<LandingPageBrandDTO> popularBrands;

    // Statistics for the landing page
    private Long totalProducts;
    private Long totalCategories;
    private Long totalBrands;
    private Long totalActiveProducts;

    // Metadata
    private String generatedAt;
    private String version;
}
