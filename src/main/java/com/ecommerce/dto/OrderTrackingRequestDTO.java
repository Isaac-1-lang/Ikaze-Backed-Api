package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting order tracking access via email
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingRequestDTO {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}
