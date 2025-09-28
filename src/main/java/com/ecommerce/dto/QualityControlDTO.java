package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityControlDTO {
    
    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;
    
    @NotBlank(message = "QC result is required")
    private String qcResult; // PASSED, FAILED
    
    private String qcNotes;
    private String inspectorId;
    private String recommendedAction; // RESTOCK, DISCARD, REPAIR
    private String newBatchStatus; // If QC failed
}
