package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecommerce.entity.ReturnAppeal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for return appeals
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnAppealDTO {
    
    private Long id;
    private Long returnRequestId;
    private UUID customerId;
    private Integer level;
    private String reason;
    private String description;
    private ReturnAppeal.AppealStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime decisionAt;
    private String decisionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Appeal media files
    private List<AppealMediaDTO> appealMedia;
    
    // Helper methods
    public boolean isPending() {
        return status == ReturnAppeal.AppealStatus.PENDING;
    }
    
    public boolean isApproved() {
        return status == ReturnAppeal.AppealStatus.APPROVED;
    }
    
    public boolean isDenied() {
        return status == ReturnAppeal.AppealStatus.DENIED;
    }
}
