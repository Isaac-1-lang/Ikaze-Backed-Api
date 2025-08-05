package com.ecommerce.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.Exception.CustomException;
import com.ecommerce.dto.LoginDto;
import com.ecommerce.dto.LoginResponseDto;
import com.ecommerce.dto.RegisterDto;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AuthService;

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
    public String registerUser(RegisterDto registerDto) {

        if (userRepository.findByUserEmail(registerDto.getUserEmail()).isPresent()) {
            throw new CustomException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(registerDto.getUserPassword());

        User user = new User();
        user.setFirstName(registerDto.getUserName());
        user.setUserEmail(registerDto.getUserEmail());
        user.setUserPassword(hashedPassword);
        user.setUserPhone(registerDto.getUserPhone());
        user.setRole(User.UserRole.valueOf(registerDto.getRole().toUpperCase()));

        User savedUser = userRepository.save(user);

    
        emailService.sendSimpleEmail(
                savedUser.getUserEmail(),
                "Welcome to Our App!",
                "Hello " + savedUser.getFirstName() + ",\n\nThank you for signing up!");

        return "User registered successfully with ID: " + savedUser.getId()
                + " and role: " + savedUser.getRole();

    }

    @Override
    public LoginResponseDto loginUser(LoginDto loginDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUserEmail(), // Updated to use getUserEmail
                            loginDto.getUserPassword()));

            if (authentication.isAuthenticated()) {
                User user = userRepository.findByUserEmail(loginDto.getUserEmail()) // Updated to use findByUserEmail
                        .orElseThrow(() -> new CustomException("User not found"));

                // Fix: Pass both username and role to generateToken
                String token = jwtService.generateToken(user.getUserEmail(), user.getRole().name());

                return new LoginResponseDto(token, user.getFirstName(), user.getUserEmail(), "Login successful",
                        user.getId(), user.getRole());
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

        user.setUserPassword(passwordEncoder.encode(newPassword));
        user.clearResetToken();
        userRepository.save(user);
    }

    @Override
    public String logoutrUser() {
        return "about to log out";
    }
}
