package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminInvitationSearchDTO {
    private String email;
    private String firstName;
    private String lastName;
    private String assignedRole;
    private String status;
    private UUID invitedById;
    private String department;
    private String position;
    private String phoneNumber;
    private String invitationMessage;
    private String notes;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private LocalDateTime expiresFrom;
    private LocalDateTime expiresTo;
    private LocalDateTime acceptedFrom;
    private LocalDateTime acceptedTo;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
    private Integer page = 0;
    private Integer size = 10;

    public boolean hasAtLeastOneFilter() {
        return email != null || firstName != null || lastName != null ||
                assignedRole != null || status != null || invitedById != null ||
                department != null || position != null || phoneNumber != null ||
                invitationMessage != null || notes != null || createdFrom != null ||
                createdTo != null || expiresFrom != null || expiresTo != null ||
                acceptedFrom != null || acceptedTo != null;
    }
}
