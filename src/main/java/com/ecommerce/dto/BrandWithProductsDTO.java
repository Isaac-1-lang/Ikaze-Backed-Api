package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandWithProductsDTO {

    private UUID brandId;
    private String brandName;
    private String description;
    private String logoUrl;
    private String slug;
    private Long productCount;
    private Boolean isActive;
    private Boolean isFeatured;
    
    // Products from this brand
    private List<LandingPageProductDTO> products;
}
