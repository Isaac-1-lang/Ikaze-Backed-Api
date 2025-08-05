package com.ecommerce.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class LoginDto {

    @NotBlank(message = "User name is required.")
    private String userName;

    @NotBlank(message = "User password is required.")
    @Size(min = 6, message = "Password must be at least 6 characters long.")
    private String userPassword;
    
}















