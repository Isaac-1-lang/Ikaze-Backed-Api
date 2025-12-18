package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminInvitationDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotNull(message = "Assigned role is required")
    private String assignedRole;

    @Size(max = 1000, message = "Invitation message cannot exceed 1000 characters")
    private String invitationMessage;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Position cannot exceed 100 characters")
    private String position;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number should be valid")
    private String phoneNumber;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @Future(message = "Expiration date must be in the future")
    private LocalDateTime expiresAt;

    @NotNull(message = "shopId is required")
    private UUID shopId;
}
