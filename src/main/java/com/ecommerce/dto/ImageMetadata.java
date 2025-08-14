package com.ecommerce.dto;

import lombok.Data;

@Data
public class ImageMetadata {
    private String altText;
    private Boolean isPrimary;
    private Integer sortOrder;
}
