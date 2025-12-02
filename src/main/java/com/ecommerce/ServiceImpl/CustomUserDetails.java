package com.ecommerce.ServiceImpl;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ecommerce.entity.User;
import com.ecommerce.Enum.UserRole;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomUserDetails implements UserDetails {

    private String name;
    private String password;
    private UserRole role;
    private UUID userId;

    public CustomUserDetails(User user) {
        this.name = user.getUserEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.userId = user.getId();
        log.info("CustomUserDetails: Created for user - email: {}, role: {}, userId: {}", 
                name, role, userId);
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = "ROLE_" + role.name();
        log.info("CustomUserDetails: Creating authority for role {} -> {}", role, authority);
        Collection<? extends GrantedAuthority> authorities = java.util.Collections.singletonList(
                new SimpleGrantedAuthority(authority));
        log.info("CustomUserDetails: Returning authorities: {}", authorities);
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
