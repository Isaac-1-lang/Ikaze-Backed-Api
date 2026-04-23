package com.ikaze.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration request")
public class RegisterDTO {



    @Schema(description="Full naem for the new account", example="Elias Papias", required=true)
    @NotBlank(message="Full name is required")
    @Size(min=3,max=30,message="Full name must be between 3 and 30 characters")
    private String fullName;   
    @Schema(description = "Username for the new account", example = "Elias", required = true)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @Schema(description = "Email address for the new account", example = "elias250@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @Schema(description = "Password for the new account (will be encrypted)", example = "SecurePass123!", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;



    public String getFullName() {
        return fullName;
    }

    public void setFullname(String fullName) {
        this.fullName=fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
