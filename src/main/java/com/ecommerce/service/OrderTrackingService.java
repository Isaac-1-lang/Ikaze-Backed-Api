package com.ecommerce.service;

import com.ecommerce.dto.OrderSummaryDTO;
import com.ecommerce.dto.OrderTrackingRequestDTO;
import com.ecommerce.dto.OrderTrackingResponseDTO;
import com.ecommerce.dto.OrderResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for secure order tracking
 */
public interface OrderTrackingService {
    
    /**
     * Request tracking access for email
     * Generates token and sends email if valid orders found
     */
    OrderTrackingResponseDTO requestTrackingAccess(OrderTrackingRequestDTO request);
    
    /**
     * Get orders by tracking token
     * Returns paginated list of orders for the email associated with token
     */
    Page<OrderSummaryDTO> getOrdersByToken(String token, Pageable pageable);
    
    /**
     * Get specific order details by token and order ID
     */
    OrderResponseDTO getOrderByTokenAndId(String token, Long orderId);
    
    /**
     * Validate tracking token
     */
    boolean isValidToken(String token);
    
    /**
     * Get email associated with token
     */
    String getEmailByToken(String token);
    
    /**
     * Clean up expired tokens
     */
    void cleanupExpiredTokens();
}
