package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for unassigning a delivery agent from a return request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnassignDeliveryAgentRequestDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotBlank(message = "Reason for unassignment is required")
    private String reason;
}
