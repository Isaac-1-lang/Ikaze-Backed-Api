package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeliveryNoteRequest {
    
    @NotBlank(message = "Note text is required")
    @Size(min = 1, max = 2000, message = "Note text must be between 1 and 2000 characters")
    private String noteText;
    
    @NotNull(message = "Note type is required")
    private String noteType; 
    
    private String noteCategory; 
    
    private Long orderId;
    
    // For group-general notes
    private Long deliveryGroupId;
}
