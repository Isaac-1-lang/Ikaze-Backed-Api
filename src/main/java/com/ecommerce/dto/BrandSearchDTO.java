package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandSearchDTO {

    private String brandName;
    
    private String description;
    
    private Boolean isActive;
    
    private Boolean isFeatured;
    
    private String slug;
    
    private Integer page = 0;
    
    private Integer size = 10;
    
    private String sortBy = "brandName";
    
    private String sortDir = "asc";
}
