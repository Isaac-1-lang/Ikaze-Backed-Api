package com.ecommerce.controller;

import com.ecommerce.dto.UserRegistrationDTO;
import com.ecommerce.dto.UserDTO;
import com.ecommerce.service.AuthService;
import com.ecommerce.dto.LoginDto;
import com.ecommerce.dto.LoginResponseDto;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.Enum.UserRole;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("api/v1/auth/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        try {
            String response = authService.registerUser(registrationDTO);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", response,
                    "data", Map.of("message", response)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginDto loginDto) {
        try {
            LoginResponseDto response = authService.loginUser(loginDto.getEmail(), loginDto.getPassword());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response,
                    "message", "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", e.getMessage()));
        }
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

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String email = authService.extractEmailFromToken(token.replace("Bearer ", ""));
            UserDTO user = authService.getCurrentUser(email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", user,
                    "message", "User data retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/delivery-agents")
    public ResponseEntity<Map<String, Object>> getDeliveryAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> deliveryAgentsPage = userRepository.findByRole(UserRole.DELIVERY_AGENT, pageable);

            List<UserDTO> deliveryAgents = deliveryAgentsPage.getContent().stream()

                    .map(user -> {
                        UserDTO dto = new UserDTO();
                        dto.setId(user.getId());
                        dto.setFirstName(user.getFirstName());
                        dto.setLastName(user.getLastName());
                        dto.setUserEmail(user.getUserEmail());
                        dto.setPhoneNumber(user.getPhoneNumber());
                        dto.setRole(user.getRole());
                        dto.setEnabled(user.isEnabled());
                        dto.setCreatedAt(user.getCreatedAt());
                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                    "content", deliveryAgents,
                    "totalElements", deliveryAgentsPage.getTotalElements(),
                    "totalPages", deliveryAgentsPage.getTotalPages(),
                    "currentPage", deliveryAgentsPage.getNumber(),
                    "size", deliveryAgentsPage.getSize(),
                    "first", deliveryAgentsPage.isFirst(),
                    "last", deliveryAgentsPage.isLast());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch delivery agents", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch delivery agents"));
        }
    }
}
