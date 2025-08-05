package com.ecommerce.ServiceImpl;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.BadCredentialsException;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.userdetails.UsernameNotFoundException;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;

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
                JwtService jwtService
            ) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.authenticationManager = authenticationManager;
            this.jwtService = jwtService;
        
        }

        @Override
        public String registerUser(RegisterDto registerDto) {
         
            if (userRepository.findByUserEmail(registerDto.getUserEmail()).isPresent()) {
                return "Email already exists";
            }

            String hashedPassword = passwordEncoder.encode(registerDto.getUserPassword());

            User user = new User();
            user.setUserName(registerDto.getUserName());
            user.setUserEmail(registerDto.getUserEmail());
            user.setUserPassword(hashedPassword);
            user.setUserPhone(registerDto.getUserPhone());
            user.setRole(registerDto.getRole());

            User savedUser = userRepository.save(user);

  //  after successful registration,I  send a welcome email 
        emailService.sendSimpleEmail(
            savedUser.getUserEmail(),
            "Welcome to Our App!",
            "Hello " + savedUser.getUserName() + ",\n\nThank you for signing up!"
        );

        return "User registered successfully with ID: " + savedUser.getUserId() 
                + " and role: " + savedUser.getRole();
        
        }



        @Override
        public LoginResponseDto loginUser(LoginDto loginDto) {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginDto.getUserName(),
                                loginDto.getUserPassword()));
        
                if (authentication.isAuthenticated()) {
                    User user = userRepository.findByUserName(loginDto.getUserName())
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
                    String token = jwtService.generateToken(loginDto.getUserName());
        
                    return new LoginResponseDto(token, user.getUserName(), user.getUserEmail(), "Login successful", user.getUserId(), user.getRole());
                }
            } catch (BadCredentialsException e) {
                throw new BadCredentialsException("Invalid username or password");
            } catch (UsernameNotFoundException e) {
                throw new UsernameNotFoundException("User not found");
            }
            throw new RuntimeException("Login failed");
        }
        





        //     @Override
        //     public void requestPasswordReset(String email) {
        //         User user = userRepository.findByUserEmail(email)
        //                 .orElseThrow(() -> new RuntimeException("User not found"));

        //         String resetCode = String.valueOf((int) (Math.random() * 99999));

        //         user.createResetToken(resetCode);
        //         userRepository.save(user);

        //         String userName = user.getUserName();

        //         String body = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px; background-color: #f9f9f9;\">"
        //         + "<h2 style=\"color: #2a9d8f; text-align: center;\">Password Reset Request</h2>"
        //         + "<p style=\"font-size: 16px; color: #333;\">Hello <b>" + userName + "</b>,</p>"
        //         + "<p style=\"font-size: 16px; color: #333;\">You have requested to reset your password. Please use the code below to reset your password:</p>"
        //         + "<div style=\"background-color: #2a9d8f; padding: 10px; border-radius: 5px; margin: 20px 0; text-align: center;\">"
        //         + "<span style=\"color: #fff; font-size: 24px; font-weight: bold;\">" + resetCode + "</span>"
        //         + "</div>"
        //         + "<p style=\"font-size: 16px; color: #333;\">If you did not request a password reset, please ignore this email.</p>"
        //         + "<p style=\"font-size: 16px; color: #333;\">Thank you,<br><b>The Agrovolve Team</b></p>"
        //         + "<footer style=\"margin-top: 20px; font-size: 12px; color: #999; text-align: center;\">"
        //         + "© " + java.time.Year.now().getValue() + " Agrovolve. All rights reserved."
        //         + "</footer>"
        //         + "</div>";
                
        //         mailService.sendEmail(user.getUserEmail(), "Password Reset Request", body);

        //     }

        // @Override
        // public boolean verifyResetCode(String email, String code) {
        //     User user = userRepository.findByUserEmail(email)
        //             .orElseThrow(() -> new RuntimeException("User not found"));

        //     return user.isResetTokenValid(code);
        // }

        // @Override
        // public void resetPassword(String email, String newPassword) {
        //     User user = userRepository.findByUserEmail(email)
        //             .orElseThrow(() -> new RuntimeException("User not found"));

        //     user.setUserPassword(passwordEncoder.encode(newPassword));
        //     user.clearResetToken();
        //     userRepository.save(user);
        // }

        @Override
        public String logoutrUser() {
            return "about to log out";
        }
    }
