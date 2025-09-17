package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPricingDTO {
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private BigDecimal profitMargin;
    private BigDecimal profitPercentage;
    private String currency;
}
