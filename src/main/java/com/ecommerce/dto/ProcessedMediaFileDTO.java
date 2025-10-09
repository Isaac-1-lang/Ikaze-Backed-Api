package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for processed media files after upload to Cloudinary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMediaFileDTO {
    
    private String fileUrl;
    private String publicId;
    private String fileType; // IMAGE or VIDEO
    private String mimeType;
    private Long fileSize;
    private Integer width;  // For images
    private Integer height; // For images
    private Double duration; // For videos (in seconds)
}
