package com.ecommerce.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.Exception.CustomException;
import com.ecommerce.dto.UserRegistrationDTO;
import com.ecommerce.dto.UserDTO;
import com.ecommerce.dto.SignupResponseDTO;
import com.ecommerce.dto.UserPointsDTO;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.RewardService;

import lombok.extern.slf4j.Slf4j;

import com.ecommerce.dto.LoginResponseDto;

import java.security.SecureRandom;
import java.util.Base64;
import java.time.LocalDateTime;

@Service
@Slf4j

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RewardService rewardService;
    @Autowired
    private EmailService emailService;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RewardService rewardService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.rewardService = rewardService;
    }

    @Override
    public SignupResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        if (userRepository.findByUserEmail(registrationDTO.getEmail()).isPresent()) {
            throw new CustomException("Email already exists");
        }

        if (registrationDTO.getPhoneNumber() == null || registrationDTO.getPhoneNumber().trim().isEmpty()) {
            throw new CustomException("Phone number is required");
        }
        
        validatePhoneNumber(registrationDTO.getPhoneNumber());

        String hashedPassword = passwordEncoder.encode(registrationDTO.getPassword());

        User user = new User();
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setUserEmail(registrationDTO.getEmail());
        user.setPassword(hashedPassword);
        user.setPhoneNumber(registrationDTO.getPhoneNumber());
        user.setRole(UserRole.ADMIN);

        User savedUser = userRepository.save(user);

        emailService.sendSimpleEmail(
                savedUser.getUserEmail(),
                "Welcome to Our App!",
                "Hello " + savedUser.getFirstName() + ",\n\nThank you for signing up!");

        Integer awardedPoints = 0;
        String pointsDescription = null;

        try {
            UserPointsDTO rewardResult = rewardService.awardPointsForSignup(savedUser.getId());
            if (rewardResult != null) {
                awardedPoints = rewardResult.getPoints();
                pointsDescription = rewardResult.getDescription();
            }
        } catch (Exception e) {
            log.warn("Failed to award signup points for user {}: {}", savedUser.getId(), e.getMessage());
        }

        return new SignupResponseDTO(
                "User registered successfully",
                savedUser.getId(),
                awardedPoints,
                pointsDescription);
    }

    @Override
    public LoginResponseDto loginUser(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

            if (authentication.isAuthenticated()) {
                User user = userRepository.findByUserEmail(email)
                        .orElseThrow(() -> new CustomException("User not found"));

                String token = jwtService.generateToken(user.getUserEmail(), user.getRole().name());

                return new LoginResponseDto(
                        token,
                        user.getFirstName() + " " + user.getLastName(),
                        user.getUserEmail(),
                        "Login Now",
                        user.getId(),
                        user.getPhoneNumber(),
                        user.getRole());
            }
        } catch (BadCredentialsException e) {
            throw new CustomException("Invalid username or password");
        }
        throw new CustomException("Login failed");
    }

    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate secure random token (64 characters)
        String resetToken = generateSecureToken();
        user.createResetToken(resetToken);
        userRepository.save(user);

        // Create password reset link
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        // Send email with reset link
        String emailBody = buildPasswordResetEmail(user.getFirstName(), resetLink);
        emailService.sendSimpleEmail(
                user.getUserEmail(),
                "Password Reset Request",
                emailBody);
        
        log.info("Password reset email sent to: {}", email);
    }
    
    /**
     * Generate secure random token for password reset
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[48]; // 384 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Build password reset email body
     */
    private String buildPasswordResetEmail(String firstName, String resetLink) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(firstName).append(",\n\n");
        body.append("We received a request to reset your password. ");
        body.append("Click the link below to reset your password:\n\n");
        body.append(resetLink).append("\n\n");
        body.append("This link will expire in 15 minutes for security reasons.\n\n");
        body.append("If you didn't request a password reset, please ignore this email. ");
        body.append("Your password will remain unchanged.\n\n");
        body.append("Best regards,\n");
        body.append("The Support Team");
        return body.toString();
    }

    @Override
    public boolean verifyResetCode(String email, String code) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isValid = user.isResetTokenValid(code);
        
        if (!isValid) {
            log.warn("Invalid or expired reset token for email: {}", email);
        }
        
        return isValid;
    }
    
    @Override
    public boolean verifyResetToken(String token) {
        try {
            // Find user by reset token
            User user = userRepository.findByResetToken(token)
                    .orElse(null);
            
            if (user == null) {
                log.warn("No user found with reset token");
                return false;
            }
            
            // Check if token is expired
            if (user.getResetTokenExpiry() == null || 
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                log.warn("Reset token expired for user: {}", user.getUserEmail());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error verifying reset token", e);
            return false;
        }
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearResetToken();
        userRepository.save(user);
        
        log.info("Password reset successful for user: {}", email);
    }
    
    @Override
    public void resetPasswordByToken(String token, String newPassword) {
        // Find user by reset token
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));
        
        // Verify token is not expired
        if (user.getResetTokenExpiry() == null || 
            user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }
        
        // Hash and set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearResetToken();
        userRepository.save(user);
        
        log.info("Password reset successful for user: {}", user.getUserEmail());
    }

    @Override
    public String logoutUser(String token) {
        jwtService.invalidateToken(token);
        return "User logged out successfully";
    }

    @Override
    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setUserEmail(user.getUserEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setRole(user.getRole());
        userDTO.setEmailVerified(user.isEmailVerified());
        userDTO.setPhoneVerified(user.isPhoneVerified());
        userDTO.setEnabled(user.isEnabled());
        userDTO.setPoints(user.getPoints());
        userDTO.setLastLogin(user.getLastLogin());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());

        return userDTO;
    }

    @Override
    public String extractEmailFromToken(String token) {
        return jwtService.extractUsername(token);
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return;
        }

        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        boolean hasCountryCode = cleaned.startsWith("+");
        String digitsOnly = hasCountryCode ? cleaned.substring(1) : cleaned;

        if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
            throw new CustomException("Phone number must be between 7 and 15 digits");
        }

        if (!digitsOnly.matches("\\d+")) {
            throw new CustomException("Phone number contains invalid characters");
        }

        if (digitsOnly.startsWith("0") && digitsOnly.length() > 8) {
            throw new CustomException("Phone number format is invalid");
        }
    }
}
