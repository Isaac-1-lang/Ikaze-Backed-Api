package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;
    
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String imageUrl;
    
    private String slug;
    
    private Long parentId;
    
    private String parentName;
    
    private Integer sortOrder = 0;
    
    private boolean isActive = true;
    
    private boolean isFeatured = false;
    
    private String metaTitle;
    
    private String metaDescription;
    
    private String metaKeywords;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<CategoryDTO> children = new ArrayList<>();
    
    private int level;
    
    private Long productCount = 0L;
    
    /**
     * Simplified constructor for creating a new category
     * 
     * @param name the name of the category
     * @param parentId the ID of the parent category (null for top-level categories)
     */
    public CategoryDTO(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }
}