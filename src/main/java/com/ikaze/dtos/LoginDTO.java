package com.ikaze.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User login request")
public class LoginDTO {

    @Schema(description = "Username or Email for login", example = "Elias", required = true)
    @NotBlank(message = "Username or Email is required")
    private String usernameOrEmail;
    
    @Schema(description = "Password for login", example = "SecurePass123!", required = true)
    @NotBlank(message = "Password is required")
    private String password;

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String username) {
        this.usernameOrEmail = usernameOrEmail;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
