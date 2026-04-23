package com.ikaze.services;

import com.ikaze.dtos.LoginDTO;
import com.ikaze.dtos.LoginResponse;
import com.ikaze.dtos.RegisterDTO;
import com.ikaze.model.UserModel;
import com.ikaze.repositories.UserRepo;
import com.ikaze.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, 
                      AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public String register(RegisterDTO request) {
        // Check if the username already exists
        if(userRepo.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken!");
        }
        
        // Check if the email already exists
        if(userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }
        
        // Create and save user
        UserModel user = new UserModel();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        
        // Encrypt the password using BCrypt
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true); // Auto-enable for now (you can add email verification later)
        userRepo.save(user);

        return "User registered successfully! Please check your email for verification.";
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginDTO request) {
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );

            // If authentication is successful, find the user
            UserModel user = userRepo.findByUsername(request.getUsernameOrEmail())
                    .or(() -> userRepo.findByEmail(request.getUsernameOrEmail()))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Check if user is enabled
            if (!user.isEnabled()) {
                throw new RuntimeException("Account is not activated. Please verify your email.");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getUsername());

            // Return login response
            return new LoginResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                "Login successful"
            );

        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username/email or password");
        }
    }
}
