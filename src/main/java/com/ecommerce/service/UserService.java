package com.ecommerce.service;

import com.ecommerce.dto.UserRegistrationDTO;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDTO registrationDTO) {
        User user = new User();
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setUserEmail(registrationDTO.getEmail());
        user.setPhoneNumber(registrationDTO.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setRole(User.UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    public String loginUser(String email, String password) {
        // Implement login logic here
        return "mock-token"; // Replace with actual token generation logic
    }

    public String requestPasswordReset(String email) {
        // Implement password reset request logic here
        return "Password reset link sent to email";
    }

    public boolean verifyResetCode(String email, String code) {
        // Implement reset code verification logic here
        return true; // Replace with actual verification logic
    }

    public void resetPassword(String email, String code, String newPassword) {
        // Implement password reset logic here
    }
}
