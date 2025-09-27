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
public class CategoryWithProductsDTO {

    private Long categoryId;
    private String categoryName;
    private String description;
    private String imageUrl;
    private String slug;
    private Long productCount;
    private Boolean isActive;
    private Boolean isFeatured;
    
    // Products from this category
    private List<LandingPageProductDTO> products;
}
