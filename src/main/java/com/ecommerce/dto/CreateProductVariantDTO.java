package com.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CreateProductVariantDTO {
    private String variantSku;
    private String variantBarcode;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean isActive;
    private Integer sortOrder;
    private Map<String, String> attributes; // attribute_name -> attribute_value
    private List<String> variantImages;
    private String imageMetadata;
    private BigDecimal salePrice;
}
