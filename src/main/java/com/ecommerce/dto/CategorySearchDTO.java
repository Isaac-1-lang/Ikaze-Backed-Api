package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
    private UUID shopId;
    
    // Pagination parameters
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "name";
}