package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailsDTO {
    private String description;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String searchKeywords;
    private String dimensionsCm;
    private BigDecimal weightKg;
    private String material;
    private String careInstructions;
    private String warrantyInfo;
    private String shippingInfo;
    private String returnPolicy;
    private Integer maximumDaysForReturn;
    private Boolean displayToCustomers;
}
