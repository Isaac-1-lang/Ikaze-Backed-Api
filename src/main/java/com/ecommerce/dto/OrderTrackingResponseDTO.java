package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for order tracking token response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponseDTO {
    
    private boolean success;
    private String message;
    private String token;
    private LocalDateTime expiresAt;
}
