package com.ecommerce.controller;

import com.ecommerce.dto.CreateUserDTO;
import com.ecommerce.entity.User;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @Operation(summary = "Create a new agent or employee", description = "Admin creates a user with role DELIVERY_AGENT or EMPLOYEE.", responses = {
            @ApiResponse(responseCode = "201", description = "User created", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("create")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDTO dto) {
        try {
            User user = userService.createUserWithRole(dto);
            return ResponseEntity.status(201).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage(),
                            "errorCode", "VALIDATION_ERROR",
                            "details", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    java.util.Map.of(
                            "success", false,
                            "message", "Failed to create user.",
                            "errorCode", "INTERNAL_ERROR",
                            "details", e.getMessage()));
        }
    }

    @Operation(summary = "Get all agents and employees", description = "Admin fetches all users with role DELIVERY_AGENT or EMPLOYEE.", responses = {
            @ApiResponse(responseCode = "200", description = "List of users", content = @Content(schema = @Schema(implementation = User.class)))
    })
    @GetMapping("all")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllAgentsAndEmployees());
    }

    @Operation(summary = "Get user by ID", description = "Admin fetches user by ID.", responses = {
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/single/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") String id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(404)
                    .body(java.util.Map.of("success", false, "message", "User not found", "errorCode", "NOT_FOUND"));
        }
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Update user", description = "Admin updates user details.", responses = {
            @ApiResponse(responseCode = "200", description = "User updated", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") String id, @Valid @RequestBody CreateUserDTO dto) {
        try {
            User updated = userService.updateUser(id, dto);
            if (updated == null) {
                return ResponseEntity.status(404).body(
                        java.util.Map.of("success", false, "message", "User not found", "errorCode", "NOT_FOUND"));
            }
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("success", false, "message",
                    "Failed to update user.", "errorCode", "INTERNAL_ERROR", "details", e.getMessage()));
        }
    }

    @Operation(summary = "Delete user", description = "Admin deletes user by ID.", responses = {
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") String id) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) {
            return ResponseEntity.status(404)
                    .body(java.util.Map.of("success", false, "message", "User not found", "errorCode", "NOT_FOUND"));
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request password reset", description = "User requests password reset by providing email and current password. Email with reset link is sent.", responses = {
            @ApiResponse(responseCode = "200", description = "Reset email sent"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestParam String email, @RequestParam String currentPassword) {
        try {
            userService.initiatePasswordReset(email, currentPassword);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Password reset email sent."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("success", false, "message", e.getMessage(), "errorCode", "VALIDATION_ERROR"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("success", false, "message",
                    "Failed to send reset email.", "errorCode", "INTERNAL_ERROR", "details", e.getMessage()));
        }
    }

    @Operation(summary = "Reset password", description = "User resets password using token from email.", responses = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        try {
            userService.resetPasswordWithToken(token, newPassword);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Password reset successful."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("success", false, "message", e.getMessage(), "errorCode", "VALIDATION_ERROR"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("success", false, "message",
                    "Failed to reset password.", "errorCode", "INTERNAL_ERROR", "details", e.getMessage()));
        }
    }
}
