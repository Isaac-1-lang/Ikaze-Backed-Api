package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDTO {
    private String publicId;
    private String url;
    private String secureUrl;
    private String format;
    private String resourceType;
    private long size;
    private String error;
    private String errorMessage;
}