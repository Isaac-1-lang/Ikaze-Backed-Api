package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandSearchDTO {

    private String brandName;
    
    private String description;
    
    private Boolean isActive;
    
    private Boolean isFeatured;
    
    private String slug;
    
    private UUID shopId;
    
    private Integer page = 0;
    
    private Integer size = 10;
    
    private String sortBy = "brandName";
    
    private String sortDir = "asc";
}
