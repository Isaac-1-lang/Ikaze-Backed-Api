package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationDTO {

    @NotBlank(message = "Invitation token is required")
    private String invitationToken;

    // Password is optional - required only when creating a new user account
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number should be valid")
    private String phoneNumber;

    @Size(max = 500, message = "Additional notes cannot exceed 500 characters")
    private String additionalNotes;
}
