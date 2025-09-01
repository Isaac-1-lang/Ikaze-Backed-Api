package com.ecommerce.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBrandDTO {

    @Size(min = 2, max = 100, message = "Brand name must be between 2 and 100 characters")
    private String brandName;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String logoUrl;
    
    private String websiteUrl;
    
    private Boolean isActive;
    
    private Boolean isFeatured;
    
    private Integer sortOrder;
    
    private String metaTitle;
    
    private String metaDescription;
    
    private String metaKeywords;
}
