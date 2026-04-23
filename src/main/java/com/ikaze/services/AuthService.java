package com.ikaze.services;

import com.ikaze.dtos.RegisterDTO;
import com.ikaze.model.UserModel;
import com.ikaze.repositories.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
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
        userRepo.save(user);

        return "User registered successfully! Please check your email for verification.";
    }
}
