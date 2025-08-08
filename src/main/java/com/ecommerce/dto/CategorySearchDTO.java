package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for category search criteria
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySearchDTO {
    
    private String name;
    private String description;
    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean sortByChildrenCount;
    private Boolean sortByProductCount;
    private String sortDirection; // "asc" or "desc"
    
    // Pagination parameters
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "name";
}