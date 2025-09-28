package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppealDecisionDTO {
    
    @NotNull(message = "Appeal ID is required")
    private Long appealId;
    
    @NotBlank(message = "Decision is required")
    private String decision; // APPROVED or DENIED
    
    private String decisionNotes;
    private String reviewerId;
    
    // For approved appeals
    private ReturnDecisionDTO.RefundDetailsDTO refundDetails;
}
