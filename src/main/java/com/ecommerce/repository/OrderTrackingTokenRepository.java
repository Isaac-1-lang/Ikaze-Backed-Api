package com.ecommerce.repository;

import com.ecommerce.entity.OrderTrackingToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for OrderTrackingToken entity
 */
@Repository
public interface OrderTrackingTokenRepository extends JpaRepository<OrderTrackingToken, Long> {
    
    /**
     * Find valid token by token string
     */
    @Query("SELECT t FROM OrderTrackingToken t WHERE t.token = :token AND t.used = false AND t.expiresAt > :now")
    Optional<OrderTrackingToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    /**
     * Find any valid (unused and not expired) token for email
     */
    @Query("SELECT t FROM OrderTrackingToken t WHERE t.email = :email AND t.used = false AND t.expiresAt > :now")
    Optional<OrderTrackingToken> findValidTokenByEmail(@Param("email") String email, @Param("now") LocalDateTime now);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM OrderTrackingToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Mark all tokens for email as used (when generating new token)
     */
    @Modifying
    @Query("UPDATE OrderTrackingToken t SET t.used = true, t.usedAt = :now WHERE t.email = :email AND t.used = false")
    void markAllTokensAsUsedForEmail(@Param("email") String email, @Param("now") LocalDateTime now);
}
