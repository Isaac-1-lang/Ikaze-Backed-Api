package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecommerce.entity.AppealMedia;

import java.time.LocalDateTime;

/**
 * DTO for appeal media files
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppealMediaDTO {
    
    private Long id;
    private Long appealId;
    private String fileUrl;
    private AppealMedia.FileType fileType;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper methods
    public boolean isImage() {
        return fileType == AppealMedia.FileType.IMAGE;
    }
    
    public boolean isVideo() {
        return fileType == AppealMedia.FileType.VIDEO;
    }
}
