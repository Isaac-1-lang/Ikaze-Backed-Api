package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAppealDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotNull(message = "Customer ID is required")
    private String customerId;
    
    private String appealText; // Optional
    
    @NotNull(message = "At least one media file is required")
    private List<MediaUploadDTO> mediaFiles;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaUploadDTO {
        @NotBlank(message = "File URL is required")
        private String fileUrl;
        
        @NotBlank(message = "File type is required")
        private String fileType; // IMAGE or VIDEO
    }
}
