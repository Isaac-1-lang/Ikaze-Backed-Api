package com.ecommerce.dto;

import com.ecommerce.Enum.UserRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String userEmail;
    private String phoneNumber;
    private UserRole role;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean enabled;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed field for full name
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
