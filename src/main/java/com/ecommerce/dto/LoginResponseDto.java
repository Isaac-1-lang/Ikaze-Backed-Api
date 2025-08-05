package com.ecommerce.dto;


import com.ecommerce.entity.User.UserRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String token;
    private String userName;
    private String userEmail;
    private String message;
    private Long userId;
    private String userPhone;
    private UserRole role;

    public LoginResponseDto(String token, String userName, String userEmail, String message, Long userId, UserRole role) {
        this.token = token;
        this.userName = userName;
        this.userEmail = userEmail;
        this.message = message;
        this.userId = userId;
        this.role = role;
    }
}
