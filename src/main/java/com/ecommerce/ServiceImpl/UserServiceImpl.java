package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.CreateUserDTO;
import com.ecommerce.dto.UpdateUserDto;
import com.ecommerce.entity.User;
import com.ecommerce.Enum.UserRole;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final com.ecommerce.service.EmailService emailService;

    @Override
    public User createUserWithRole(CreateUserDTO dto) {
        if (dto.getEmail() == null || dto.getPassword() == null || dto.getRole() == null) {
            throw new IllegalArgumentException("Email, password, and role are required.");
        }
        UserRole role = dto.getRole();
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        if (UserRole.DELIVERY_AGENT.equals(role)) {
            role = UserRole.DELIVERY_AGENT;
        } else if (UserRole.EMPLOYEE.equals(role)) {
            role = UserRole.EMPLOYEE;
        } else {
            throw new IllegalArgumentException("Role must be DELIVERY_AGENT or EMPLOYEE.");
        }
        if (userRepository.existsByUserEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        User user = new User();
        user.setUserEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    public java.util.List<User> getAllAgentsAndEmployees() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.DELIVERY_AGENT || u.getRole() == UserRole.EMPLOYEE)
                .toList();
    }

    @Override
    public User getUserById(String id) {
        return userRepository.findById(java.util.UUID.fromString(id))
                .filter(u -> u.getRole() == UserRole.DELIVERY_AGENT || u.getRole() == UserRole.EMPLOYEE)
                .orElse(null);
    }

    @Override
    public User updateUser(String id, UpdateUserDto dto) {
        java.util.Optional<User> opt = userRepository.findById(java.util.UUID.fromString(id));
        if (opt.isEmpty())
            return null;
        User user = opt.get();
        if (user.getRole() != UserRole.DELIVERY_AGENT && user.getRole() != UserRole.EMPLOYEE)
            return null;
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public boolean deleteUser(String id) {
        java.util.Optional<User> opt = userRepository.findById(java.util.UUID.fromString(id));
        if (opt.isEmpty())
            return false;
        User user = opt.get();
        if (user.getRole() != UserRole.DELIVERY_AGENT && user.getRole() != UserRole.EMPLOYEE)
            return false;
        userRepository.delete(user);
        return true;
    }

    @Override
    public void initiatePasswordReset(String email, String currentPassword) {
        User user = userRepository.findByUserEmail(email)
                .filter(u -> u.getRole() == UserRole.DELIVERY_AGENT || u.getRole() == UserRole.EMPLOYEE)
                .orElseThrow(() -> new IllegalArgumentException("User not found or not allowed."));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        String token = java.util.UUID.randomUUID().toString();
        user.createResetToken(token);
        userRepository.save(user);
        String resetLink = "http://44.201.144.244/api/v1/admin/users/reset-password?token=" + token;
        String subject = "Password Reset Request";
        String body = "Hello " + user.getFirstName() + ",\n\n" +
                "You requested a password reset. Please click the link below to reset your password:\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.";
        emailService.sendEmail(user.getUserEmail(), subject, body);
    }

    @Override
    public void resetPasswordWithToken(String token, String newPassword) {
        User user = userRepository.findAll().stream()
                .filter(u -> (u.getRole() == UserRole.DELIVERY_AGENT || u.getRole() == UserRole.EMPLOYEE)
                        && u.isResetTokenValid(token))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearResetToken();
        userRepository.save(user);
    }
}
