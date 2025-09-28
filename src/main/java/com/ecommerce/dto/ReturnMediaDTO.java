package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecommerce.entity.ReturnMedia;

import java.time.LocalDateTime;

/**
 * DTO for return media files
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnMediaDTO {
    
    private Long id;
    private Long returnRequestId;
    private String fileUrl;
    private String publicId;
    private ReturnMedia.FileType fileType;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper methods
    public boolean isImage() {
        return fileType == ReturnMedia.FileType.IMAGE;
    }
    
    public boolean isVideo() {
        return fileType == ReturnMedia.FileType.VIDEO;
    }
    
    public String getFileExtension() {
        if (fileUrl != null && fileUrl.contains(".")) {
            return fileUrl.substring(fileUrl.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}
