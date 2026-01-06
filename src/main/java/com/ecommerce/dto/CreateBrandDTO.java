package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBrandDTO {

    @NotBlank(message = "Brand name is required")
    @Size(min = 2, max = 100, message = "Brand name must be between 2 and 100 characters")
    private String brandName;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String logoUrl;
    
    private String websiteUrl;
    
    private boolean isActive = true;
    
    private boolean isFeatured = false;
    
    private Integer sortOrder = 0;
    
    private String metaTitle;
    
    private String metaDescription;
    
    private String metaKeywords;
    
    private UUID shopId;
}
