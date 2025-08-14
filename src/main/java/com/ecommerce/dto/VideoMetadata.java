package com.ecommerce.dto;

import lombok.Data;

@Data
public class VideoMetadata {
    private String url;
    private String filename;
    private String publicId;
    private String format;
    private Long size;
    private Integer duration;
    private Integer width;
    private Integer height;
    private String title;
    private String description;
    private Integer sortOrder;
}
