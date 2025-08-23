package com.ecommerce.controller;

import com.ecommerce.dto.UserRegistrationDTO;
import com.ecommerce.service.AuthService;
import com.ecommerce.dto.LoginDto;
import com.ecommerce.dto.LoginResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/auth/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        String response = authService.registerUser(registrationDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> loginUser(@Valid @RequestBody LoginDto loginDto) {
        LoginResponseDto response = authService.loginUser(loginDto.getEmail(), loginDto.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<String> requestPasswordReset(@RequestBody Map<String, String> request) {
        authService.requestPasswordReset(request.get("email"));
        return ResponseEntity.ok("Password reset request sent");
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<String> verifyResetCode(@RequestBody Map<String, String> request) {
        boolean isValid = authService.verifyResetCode(request.get("email"), request.get("code"));
        return ResponseEntity.ok(isValid ? "Valid code" : "Invalid code");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        authService.resetPassword(request.get("email"), request.get("newPassword"));
        return ResponseEntity.ok("Password reset successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(@RequestHeader("Authorization") String token) {
        String response = authService.logoutUser(token.replace("Bearer ", ""));
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/welcome")
    public ResponseEntity<String> welcomeUser(@RequestHeader("Authorization") String token) {
            return ResponseEntity.ok("Welcome back ");
    }
}
