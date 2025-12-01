package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDecisionDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotBlank(message = "Decision is required")
    private String decision;
    
    private String decisionNotes;

}
