package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMediaDTO {
    private Long imageId;
    private String url;
    private String altText;
    private boolean isPrimary;
    private Integer sortOrder;
    private String fileName;
    private Long fileSize;
    private String mimeType;
}
