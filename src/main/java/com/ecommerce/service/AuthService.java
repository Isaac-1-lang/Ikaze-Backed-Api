package com.ecommerce.service;

import com.ecommerce.dto.UserRegistrationDTO;
import com.ecommerce.dto.LoginResponseDto;
import com.ecommerce.dto.UserDTO;
import com.ecommerce.dto.SignupResponseDTO;

public interface AuthService {
    SignupResponseDTO registerUser(UserRegistrationDTO registrationDTO);

    LoginResponseDto loginUser(String email, String password);

    void requestPasswordReset(String email);

    boolean verifyResetCode(String email, String code);

    void resetPassword(String email, String newPassword);

    String logoutUser(String token);

    UserDTO getCurrentUser(String email);

    String extractEmailFromToken(String token);
}
