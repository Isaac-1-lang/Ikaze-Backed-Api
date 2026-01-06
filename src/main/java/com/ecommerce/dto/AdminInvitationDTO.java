package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInvitationDTO {
    private UUID invitationId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String assignedRole;
    private String invitationToken;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID invitedById;
    private String invitedByName;
    private String invitedByEmail;
    private UUID acceptedById;
    private String acceptedByName;
    private String acceptedByEmail;
    private String invitationMessage;
    private String department;
    private String position;
    private String phoneNumber;
    private String notes;
    private UUID shopId;
    private String shopName;
    private boolean isExpired;
    private boolean canBeAccepted;
    private boolean canBeCancelled;
}
