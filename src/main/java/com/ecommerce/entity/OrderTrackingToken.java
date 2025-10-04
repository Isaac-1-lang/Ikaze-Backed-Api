package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing order tracking tokens
 */
@Entity
@Table(name = "order_tracking_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "token", unique = true, nullable = false)
    private String token;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "used", nullable = false)
    private boolean used = false;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if token is valid (not used and not expired)
     */
    public boolean isValid() {
        return !used && !isExpired();
    }
    
    /**
     * Mark token as used
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}
