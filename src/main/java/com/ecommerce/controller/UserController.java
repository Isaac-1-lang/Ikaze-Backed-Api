package com.ecommerce.controller;

import com.ecommerce.dto.UserRegistrationDTO;
import com.ecommerce.entity.User;
import com.ecommerce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        User registeredUser = userService.registerUser(registrationDTO);
        return ResponseEntity.ok(registeredUser);
    }
}
