package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPricingUpdateDTO {
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
}
