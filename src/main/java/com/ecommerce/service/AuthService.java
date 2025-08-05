package com.ecommerce.service;

import com.ecommerce.dto.LoginDto;
import com.ecommerce.dto.LoginResponseDto;
import com.ecommerce.dto.RegisterDto;

public interface AuthService {
    String registerUser(RegisterDto registerDto);
    LoginResponseDto loginUser(LoginDto loginDto);
    String logoutrUser();
    void requestPasswordReset(String email);
    boolean verifyResetCode(String email, String code);
    void resetPassword(String email, String newPassword);
}
