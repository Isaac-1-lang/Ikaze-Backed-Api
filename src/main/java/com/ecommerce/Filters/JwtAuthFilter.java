package com.ecommerce.Filters;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ecommerce.ServiceImpl.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @SuppressWarnings("null")
    @Override
    public void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = req.getRequestURI();
        String authHeader = req.getHeader("Authorization");
        String token = null;
        String username = null;
        
        log.info("JwtAuthFilter: Processing request to {}", requestPath);
        log.debug("JwtAuthFilter: Authorization header present: {}", authHeader != null);
        
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.debug("JwtAuthFilter: Token extracted, length: {}", token.length());
                try {
                    username = jwtService.extractUsername(token);
                    log.info("JwtAuthFilter: Extracted username from token: {}", username);
                } catch (Exception e) {
                    log.warn("JwtAuthFilter: Failed to extract username from token: {}", e.getMessage());
                    username = null;
                }
            } else {
                log.warn("JwtAuthFilter: No valid Authorization header found");
            }

            if (username != null) {
                boolean hasExistingAuth = SecurityContextHolder.getContext().getAuthentication() != null;
                log.debug("JwtAuthFilter: Existing authentication in context: {}", hasExistingAuth);
                
                if (!hasExistingAuth) {
                    try {
                        log.info("JwtAuthFilter: Loading user details for username: {}", username);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        log.info("JwtAuthFilter: User details loaded - username: {}, authorities: {}", 
                                userDetails.getUsername(), userDetails.getAuthorities());
                        
                        boolean isValidToken = jwtService.validateToken(token, userDetails);
                        log.info("JwtAuthFilter: Token validation result: {}", isValidToken);
                        
                        if (isValidToken) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, userDetails.getAuthorities());
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            log.info("JwtAuthFilter: Authentication set successfully for user: {} with authorities: {}", 
                                    username, userDetails.getAuthorities());
                        } else {
                            log.warn("JwtAuthFilter: Token validation failed for user: {}", username);
                        }
                    } catch (Exception e) {
                        log.error("JwtAuthFilter: Error setting authentication for user: {}", username, e);
                    }
                } else {
                    log.debug("JwtAuthFilter: Authentication already exists in context, skipping");
                }
            } else {
                log.warn("JwtAuthFilter: No username extracted, request will proceed without authentication");
            }
        } catch (Exception e) {
            log.error("JwtAuthFilter: Unexpected error in filter", e);
        }

        org.springframework.security.core.Authentication finalAuth = SecurityContextHolder.getContext().getAuthentication();
        if (finalAuth != null) {
            log.info("JwtAuthFilter: Final authentication state - user: {}, authorities: {}", 
                    finalAuth.getName(), finalAuth.getAuthorities());
        } else {
            log.warn("JwtAuthFilter: No authentication set in SecurityContext after filter processing");
        }

        filterChain.doFilter(req, res);
    }
}
