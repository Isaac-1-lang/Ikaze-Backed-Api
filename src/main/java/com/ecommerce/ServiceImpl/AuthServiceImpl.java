package com.ecommerce.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AuthService;

import lombok.extern.slf4j.Slf4j;

import com.ecommerce.dto.LoginResponseDto;

@Service
@Slf4j

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Autowired
    private EmailService emailService;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public SignupResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        if (userRepository.findByUserEmail(registrationDTO.getEmail()).isPresent()) {
            throw new CustomException("Email already exists");
        }

        if (registrationDTO.getPhoneNumber() != null && !registrationDTO.getPhoneNumber().trim().isEmpty()) {
            validatePhoneNumber(registrationDTO.getPhoneNumber());
        }

        String hashedPassword = passwordEncoder.encode(registrationDTO.getPassword());

        User user = new User();
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setUserEmail(registrationDTO.getEmail());
        user.setPassword(hashedPassword);
        user.setPhoneNumber(registrationDTO.getPhoneNumber());
        user.setRole(UserRole.CUSTOMER);

        User savedUser = userRepository.save(user);

        emailService.sendSimpleEmail(
                savedUser.getUserEmail(),
                "Welcome to Our App!",
                "Hello " + savedUser.getFirstName() + ",\n\nThank you for signing up!");

        return new SignupResponseDTO(
                "User registered successfully",
                savedUser.getId());
    }


    //login user
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

        String resetCode = String.valueOf((int) (Math.random() * 99999));
        user.createResetToken(resetCode);
        userRepository.save(user);

        emailService.sendSimpleEmail(
                user.getUserEmail(),
                "Password Reset Request",
                "Your reset code is: " + resetCode);
    }

    @Override
    public boolean verifyResetCode(String email, String code) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.isResetTokenValid(code);
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearResetToken();
        userRepository.save(user);
    }

    @Override
    public boolean verifyResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        User user = userRepository.findByResetToken(token)
                .orElse(null);
        
        if (user == null) {
            return false;
        }
        
        return user.isResetTokenValid(token);
    }

    @Override
    public void resetPasswordByToken(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new CustomException("Reset token is required");
        }
        
        if (newPassword == null || newPassword.length() < 8) {
            throw new CustomException("Password must be at least 8 characters long");
        }
        
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new CustomException("Invalid or expired reset token"));
        
        if (!user.isResetTokenValid(token)) {
            throw new CustomException("Invalid or expired reset token");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearResetToken();
        userRepository.save(user);
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
        // Points are now handled by UserPoints entity per shop, not directly on User
        userDTO.setPoints(0);
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
