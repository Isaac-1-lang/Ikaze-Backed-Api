package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVideoDTO {
    private Long videoId;
    private String url;
    private String title;
    private String description;
    private Integer sortOrder;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private Integer duration;
}
