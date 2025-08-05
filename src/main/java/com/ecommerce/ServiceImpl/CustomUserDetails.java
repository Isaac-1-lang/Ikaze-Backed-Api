package com.ecommerce.ServiceImpl;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ecommerce.entity.User;
import com.ecommerce.entity.User.UserRole;


public class CustomUserDetails implements UserDetails {

    private String name;
    private String password;
    private UserRole role;

    public CustomUserDetails(User user) {

        this.name = user.getFirstName();
        this.password = user.getPassword();
        this.role = user.getRole();

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.name;
    }


    
}
