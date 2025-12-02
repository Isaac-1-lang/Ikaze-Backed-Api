package com.ecommerce.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("CustomUserDetailsService: Loading user by email: {}", email);
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> {
                    log.error("CustomUserDetailsService: User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        log.info("CustomUserDetailsService: User found - email: {}, role: {}, id: {}", 
                user.getUserEmail(), user.getRole(), user.getId());
        CustomUserDetails userDetails = new CustomUserDetails(user);
        log.info("CustomUserDetailsService: Created UserDetails with authorities: {}", 
                userDetails.getAuthorities());
        return userDetails;
    }
}
