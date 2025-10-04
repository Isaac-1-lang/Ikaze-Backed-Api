package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.EmailService;
import com.ecommerce.service.OrderTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of OrderTrackingService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderTrackingServiceImpl implements OrderTrackingService {
    
    private final OrderRepository orderRepository;
    private final OrderTrackingTokenRepository tokenRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final EmailService emailService;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public OrderTrackingResponseDTO requestTrackingAccess(OrderTrackingRequestDTO request) {
        String email = request.getEmail().toLowerCase().trim();
        
        try {
            // Check if there are any orders for this email
            List<Order> orders = orderRepository.findByCustomerInfoEmail(email);
            
            if (orders.isEmpty()) {
                log.warn("No orders found for email: {}", email);
                return new OrderTrackingResponseDTO(
                    false, 
                    "No orders found for this email address", 
                    null, 
                    null, 
                    null
                );
            }
            
            // Check if user already has a valid token
            Optional<OrderTrackingToken> existingToken = tokenRepository
                .findValidTokenByEmail(email, LocalDateTime.now());
            
            if (existingToken.isPresent()) {
                log.info("Valid token already exists for email: {}", email);
                OrderTrackingToken token = existingToken.get();
                String trackingUrl = frontendUrl + "/track-order?token=" + token.getToken();
                
                return new OrderTrackingResponseDTO(
                    true,
                    "A valid tracking link was already sent to your email. Please check your inbox.",
                    token.getToken(),
                    token.getExpiresAt(),
                    trackingUrl
                );
            }
            
            // Mark all existing tokens for this email as used
            tokenRepository.markAllTokensAsUsedForEmail(email, LocalDateTime.now());
            
            // Generate new secure token
            String token = generateSecureToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
            
            // Save token to database
            OrderTrackingToken trackingToken = new OrderTrackingToken();
            trackingToken.setToken(token);
            trackingToken.setEmail(email);
            trackingToken.setExpiresAt(expiresAt);
            tokenRepository.save(trackingToken);
            
            // Generate tracking URL
            String trackingUrl = frontendUrl + "/track-order?token=" + token;
            
            // Send email with tracking link
            sendTrackingEmail(email, trackingUrl, expiresAt, orders.size());
            
            log.info("Generated tracking token for email: {} with {} orders", email, orders.size());
            
            return new OrderTrackingResponseDTO(
                true,
                "Tracking link sent to your email successfully!",
                token,
                expiresAt,
                trackingUrl
            );
            
        } catch (Exception e) {
            log.error("Error generating tracking access for email: {}", email, e);
            return new OrderTrackingResponseDTO(
                false,
                "Failed to generate tracking access. Please try again later.",
                null,
                null,
                null
            );
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getOrdersByToken(String token, Pageable pageable) {
        // Validate token
        Optional<OrderTrackingToken> trackingToken = tokenRepository
            .findValidToken(token, LocalDateTime.now());
        
        if (trackingToken.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired tracking token");
        }
        
        String email = trackingToken.get().getEmail();
        
        // Get orders for this email with pagination
        Page<Order> orders = orderRepository.findByCustomerInfoEmailWithPagination(email, pageable);
        
        // Convert to DTOs
        List<OrderSummaryDTO> orderSummaries = orders.getContent().stream()
            .map(this::convertToOrderSummary)
            .collect(Collectors.toList());
        
        return new PageImpl<>(orderSummaries, pageable, orders.getTotalElements());
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByTokenAndId(String token, Long orderId) {
        // Validate token
        Optional<OrderTrackingToken> trackingToken = tokenRepository
            .findValidToken(token, LocalDateTime.now());
        
        if (trackingToken.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired tracking token");
        }
        
        String email = trackingToken.get().getEmail();
        
        // Get order and verify it belongs to this email
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }
        
        Order order = orderOpt.get();
        
        // Verify order belongs to the email associated with token
        if (order.getOrderCustomerInfo() == null || 
            !email.equalsIgnoreCase(order.getOrderCustomerInfo().getEmail())) {
            throw new IllegalArgumentException("Order does not belong to this email address");
        }
        
        // Convert to detailed DTO
        return convertToOrderResponseDTO(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isValidToken(String token) {
        return tokenRepository.findValidToken(token, LocalDateTime.now()).isPresent();
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getEmailByToken(String token) {
        return tokenRepository.findValidToken(token, LocalDateTime.now())
            .map(OrderTrackingToken::getEmail)
            .orElse(null);
    }
    
    @Override
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired tracking tokens");
    }
    
    /**
     * Generate secure random token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Send tracking email to customer
     */
    private void sendTrackingEmail(String email, String trackingUrl, LocalDateTime expiresAt, int orderCount) {
        try {
            String subject = "Your Secure Order Tracking Link";
            String body = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2563eb;">🔒 Your Secure Order Tracking Link</h2>
                        
                        <p>Hello,</p>
                        
                        <p>You requested access to track your orders. We found <strong>%d order(s)</strong> associated with this email address.</p>
                        
                        <div style="background: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0 0 15px 0;"><strong>Click the link below to access your orders:</strong></p>
                            <a href="%s" style="display: inline-block; background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">
                                View My Orders
                            </a>
                        </div>
                        
                        <div style="background: #fef3c7; padding: 15px; border-radius: 6px; border-left: 4px solid #f59e0b;">
                            <p style="margin: 0;"><strong>⏰ Important:</strong> This link will expire on <strong>%s</strong> for security reasons.</p>
                        </div>
                        
                        <h3 style="color: #374151;">🛡️ Security Information</h3>
                        <ul style="color: #6b7280;">
                            <li>This link is unique to your email address</li>
                            <li>It will expire in 30 minutes for your security</li>
                            <li>Only you can access your order information with this link</li>
                            <li>If you didn't request this, you can safely ignore this email</li>
                        </ul>
                        
                        <p style="margin-top: 30px; color: #6b7280; font-size: 14px;">
                            If you have any questions, please contact our support team.
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">
                        <p style="color: #9ca3af; font-size: 12px; text-align: center;">
                            This is an automated message. Please do not reply to this email.
                        </p>
                    </div>
                </body>
                </html>
                """, orderCount, trackingUrl, expiresAt.toString());
            
            emailService.sendEmail(email, subject, body);
            log.info("Tracking email sent successfully to: {}", email);
            
        } catch (Exception e) {
            log.error("Failed to send tracking email to: {}", email, e);
            // Don't throw exception here - token is already generated
        }
    }
    
    /**
     * Convert Order to OrderSummaryDTO
     */
    private OrderSummaryDTO convertToOrderSummary(Order order) {
        OrderSummaryDTO summary = new OrderSummaryDTO();
        summary.setId(order.getId());
        summary.setOrderNumber(order.getOrderCode());
        summary.setStatus(order.getStatus().toString());
        summary.setCreatedAt(order.getCreatedAt());
        summary.setTotal(order.getTotalAmount());
        summary.setItemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0);
        
        if (order.getOrderCustomerInfo() != null) {
            summary.setCustomerName(order.getOrderCustomerInfo().getFullName());
            summary.setCustomerEmail(order.getOrderCustomerInfo().getEmail());
        }
        
        // Check if order has return request
        summary.setHasReturnRequest(hasReturnRequest(order.getOrderCode()));
        
        return summary;
    }
    
    /**
     * Convert Order to detailed OrderResponseDTO
     */
    private OrderResponseDTO convertToOrderResponseDTO(Order order) {
        // Use existing conversion logic from OrderController
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderCode());
        dto.setStatus(order.getStatus().toString());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setTotal(order.getTotalAmount());
        dto.setPickupToken(order.getPickupToken());
        
        // Set customer info
        if (order.getOrderCustomerInfo() != null) {
            OrderResponseDTO.CustomerInfo customerInfo = new OrderResponseDTO.CustomerInfo();
            customerInfo.setName(order.getOrderCustomerInfo().getFullName());
            customerInfo.setEmail(order.getOrderCustomerInfo().getEmail());
            customerInfo.setPhone(order.getOrderCustomerInfo().getPhoneNumber());
            dto.setCustomerInfo(customerInfo);
            
            // Set shipping address
            OrderResponseDTO.ShippingAddress shippingAddress = new OrderResponseDTO.ShippingAddress();
            shippingAddress.setStreet(order.getOrderCustomerInfo().getStreetAddress());
            shippingAddress.setCity(order.getOrderCustomerInfo().getCity());
            shippingAddress.setState(order.getOrderCustomerInfo().getState());
            shippingAddress.setCountry(order.getOrderCustomerInfo().getCountry());
            dto.setShippingAddress(shippingAddress);
        }
        
        // Set payment info
        if (order.getOrderTransaction() != null) {
            dto.setPaymentMethod(order.getOrderTransaction().getPaymentMethod().toString());
            dto.setPaymentStatus(order.getOrderTransaction().getStatus().toString());
        }
        
        // Set order items (simplified - you may want to add more details)
        if (order.getOrderItems() != null) {
            List<OrderResponseDTO.OrderItem> items = order.getOrderItems().stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
            dto.setItems(items);
        }
        
        return dto;
    }
    
    /**
     * Convert OrderItem to OrderResponseDTO.OrderItem
     */
    private OrderResponseDTO.OrderItem convertToOrderItemDTO(OrderItem orderItem) {
        OrderResponseDTO.OrderItem dto = new OrderResponseDTO.OrderItem();
        dto.setId(orderItem.getOrderItemId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setTotalPrice(orderItem.getSubtotal()); // Use getSubtotal() method
        
        // Set product info
        if (orderItem.getProduct() != null) {
            OrderResponseDTO.Product product = new OrderResponseDTO.Product();
            product.setId(orderItem.getProduct().getProductId());
            product.setName(orderItem.getProduct().getProductName());
            // Add product images if available
            dto.setProduct(product);
        }
        
        // Set variant info if available
        if (orderItem.getProductVariant() != null) {
            OrderResponseDTO.Variant variant = new OrderResponseDTO.Variant();
            variant.setId(orderItem.getProductVariant().getId());
            variant.setName(orderItem.getProductVariant().getVariantName());
            dto.setVariant(variant);
        }
        
        // Set return eligibility (you may want to implement this logic)
        dto.setReturnEligible(true); // Placeholder
        dto.setMaxReturnDays(30); // Placeholder
        dto.setDaysRemainingForReturn(25); // Placeholder - calculate based on order date
        
        return dto;
    }
    
    /**
     * Check if order has return request
     */
    private boolean hasReturnRequest(String orderNumber) {
        try {
            return returnRequestRepository.findByOrderNumber(orderNumber).isPresent();
        } catch (Exception e) {
            log.warn("Error checking return request for order: {}", orderNumber, e);
            return false;
        }
    }
}
