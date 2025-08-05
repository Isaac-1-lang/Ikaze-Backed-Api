package com.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Embeddable
@Data
@Getter
@Setter
public class ResetToken {

    @Column(name = "reset_token", length = 255)
    private String token;

    @Column(name = "reset_token_expires_at")
    private Instant expiresAt;

    public void generateResetToken(String token, int minutesUntilExpiry) {
        this.token = token;
        this.expiresAt = Instant.now().plusSeconds(minutesUntilExpiry * 60);
    }

    public boolean isTokenValid(String inputToken) {
        return inputToken.equals(this.token) && this.expiresAt.isAfter(Instant.now());
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void clearToken() {
        this.token = null;
        this.expiresAt = null;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

}
