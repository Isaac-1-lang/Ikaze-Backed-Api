package com.ecommerce.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.Exception.CustomException;
import com.ecommerce.dto.UserRegistrationDTO;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AuthService;
import com.ecommerce.dto.LoginResponseDto;

@Service
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
    public String registerUser(UserRegistrationDTO registrationDTO) {
        if (userRepository.findByUserEmail(registrationDTO.getEmail()).isPresent()) {
            throw new CustomException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(registrationDTO.getPassword());

        User user = new User();
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setUserEmail(registrationDTO.getEmail());
        user.setPassword(hashedPassword);
        user.setPhoneNumber(registrationDTO.getPhoneNumber());
        user.setRole(User.UserRole.CUSTOMER); // Default role

        User savedUser = userRepository.save(user);

        emailService.sendSimpleEmail(
                savedUser.getUserEmail(),
                "Welcome to Our App!",
                "Hello " + savedUser.getFirstName() + ",\n\nThank you for signing up!");

        return "User registered successfully with ID: " + savedUser.getId().toString();
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
                        "Login successful",
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
    public String logoutUser(String token) {
        jwtService.invalidateToken(token); // Pass the token to invalidate
        return "User logged out successfully";
    }
}
