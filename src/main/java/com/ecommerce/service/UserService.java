package com.ecommerce.service;

import com.ecommerce.dto.CreateUserDTO;
import com.ecommerce.entity.User;

public interface UserService {
    User createUserWithRole(CreateUserDTO dto);

    java.util.List<User> getAllAgentsAndEmployees();

    User getUserById(String id);

    User updateUser(String id, CreateUserDTO dto);

    boolean deleteUser(String id);

    void initiatePasswordReset(String email, String currentPassword);

    void resetPasswordWithToken(String token, String newPassword);
}
