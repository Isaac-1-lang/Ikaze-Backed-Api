package com.ecommerce.dto;


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
    private String role;

    public LoginResponseDto(String token, String userName, String userEmail, String message, Long userId, String role) {
        this.token = token;
        this.userName = userName;
        this.userEmail = userEmail;
        this.message = message;
        this.userId = userId;
        this.role = role;
    }
}
