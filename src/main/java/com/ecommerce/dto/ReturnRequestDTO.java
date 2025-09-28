package com.ecommerce.dto;

import com.ecommerce.entity.ReturnRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDTO {
    
    private Long id;
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    private UUID customerId;
    
    @NotBlank(message = "Return reason is required")
    private String reason;
    
    private ReturnRequest.ReturnStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime decisionAt;
    private String decisionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Related data
    private List<ReturnMediaDTO> returnMedia;
    private List<ReturnItemDTO> returnItems;
    private ReturnAppealDTO returnAppeal;
    private String customerName;
    private String customerEmail;
    private String orderNumber;
    
    // Helper fields
    private boolean canBeAppealed;
    private int daysUntilExpiry;
    private boolean isEligibleForReturn;
}
