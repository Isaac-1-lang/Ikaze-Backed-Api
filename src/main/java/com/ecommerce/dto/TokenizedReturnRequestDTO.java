package com.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for tokenized return request submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenizedReturnRequestDTO {
    
    @NotBlank(message = "Order number is required")
    private String orderNumber;
    
    @NotBlank(message = "Tracking token is required")
    private String trackingToken;
    
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;
    
    @NotEmpty(message = "At least one return item is required")
    @Valid
    private List<ReturnItemDTO> returnItems;
}
