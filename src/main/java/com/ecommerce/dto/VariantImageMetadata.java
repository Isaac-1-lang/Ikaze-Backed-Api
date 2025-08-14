package com.ecommerce.dto;

import lombok.Data;

@Data
public class VariantImageMetadata {
    private String altText;
    private Boolean isPrimary;
    private Integer sortOrder;
}
