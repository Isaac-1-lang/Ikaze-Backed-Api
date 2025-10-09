package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
    private boolean success;
    private String message;
    private String error;
    private int status;
    private long timestamp;
    
    public static ErrorResponseDTO of(String message, int status) {
        return ErrorResponseDTO.builder()
                .success(false)
                .message(message)
                .error(message)
                .status(status)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
