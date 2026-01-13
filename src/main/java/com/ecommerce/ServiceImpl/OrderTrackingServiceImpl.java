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

import java.math.BigDecimal;
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

    @Value("${app.frontend.url:https://shopsphere-frontend.vercel.app/}")
    private String frontendUrl;

    private static final int TOKEN_EXPIRY_MINUTES = 60;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public OrderTrackingResponseDTO requestTrackingAccess(OrderTrackingRequestDTO request) {
        String email = request.getEmail().toLowerCase().trim();

        try {
            // Check if there are any orders for this email
            List<Order> orders = orderRepository.findByCustomerInfoEmail(email);

            if (orders.isEmpty()) {
                return new OrderTrackingResponseDTO(
                        false,
                        "No orders found for this email address",
                        null,
                        null);
            }

            // Check if there's an existing token record for this email
            Optional<OrderTrackingToken> existingTokenOpt = tokenRepository.findByEmail(email);

            if (existingTokenOpt.isPresent()) {
                OrderTrackingToken existingToken = existingTokenOpt.get();

                // If the token is still valid, return it
                if (existingToken.isValid()) {
                    log.info("Valid token already exists for email: {}", email);
                    return new OrderTrackingResponseDTO(
                            true,
                            "A valid tracking link was already sent to your email. Please check your inbox.",
                            existingToken.getToken(),
                            existingToken.getExpiresAt());
                }

                // Token exists but is invalid (expired or used), update it
                log.info("Updating expired/used token for email: {}", email);
                String newToken = generateSecureToken();
                LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

                existingToken.setToken(newToken);
                existingToken.setExpiresAt(newExpiresAt);
                existingToken.setUsed(false);
                existingToken.setUsedAt(null);
                existingToken.setCreatedAt(LocalDateTime.now());

                tokenRepository.save(existingToken);

                String trackingUrl = frontendUrl + "/track-order?token=" + newToken;
                sendTrackingEmail(email, trackingUrl, newExpiresAt, orders.size());

                return new OrderTrackingResponseDTO(
                        true,
                        "Tracking link sent to your email successfully!",
                        newToken,
                        newExpiresAt);
            }

            // No existing token record, create a new one
            log.info("Creating new token for email: {}", email);
            String token = generateSecureToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

            OrderTrackingToken trackingToken = new OrderTrackingToken();
            trackingToken.setToken(token);
            trackingToken.setEmail(email);
            trackingToken.setExpiresAt(expiresAt);
            trackingToken.setUsed(false);
            tokenRepository.save(trackingToken);

            String trackingUrl = frontendUrl + "/track-order?token=" + token;
            sendTrackingEmail(email, trackingUrl, expiresAt, orders.size());

            return new OrderTrackingResponseDTO(
                    true,
                    "Tracking link sent to your email successfully!",
                    token,
                    expiresAt);

        } catch (Exception e) {
            log.error("Failed to generate tracking access", e);
            return new OrderTrackingResponseDTO(
                    false,
                    "Failed to generate tracking access. Please try again later.",
                    null,
                    null);
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

    private void sendTrackingEmail(String email, String trackingUrl, LocalDateTime expiresAt, int orderCount) {
        try {
            String token = trackingUrl.substring(trackingUrl.lastIndexOf("token=") + 6);
            String formattedExpiryTime = expiresAt.toString().replace("T", " ");

            emailService.sendOrderTrackingEmail(email, token, trackingUrl, formattedExpiryTime);
            log.info("Tracking email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send tracking email to: {}", email, e);
            throw new Error("Failed to send the tracking email to the customer");
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
        summary.setItemCount(order.getAllItems() != null ? order.getAllItems().size() : 0);

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
        dto.setStatus(order.getStatus() != null ? order.getStatus() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setTotal(order.getTotalAmount());

        // Set order info from OrderInfo entity
        if (order.getOrderInfo() != null) {
            dto.setSubtotal(order.getOrderInfo().getTotalAmount());
            dto.setTax(order.getOrderInfo().getTaxAmount());
            dto.setShipping(order.getOrderInfo().getShippingCost());
            dto.setDiscount(order.getOrderInfo().getDiscountAmount());
            dto.setTotal(order.getOrderInfo().getFinalAmount());
        } else {
            // Fallback to order total if OrderInfo is null
            dto.setSubtotal(order.getTotalAmount());
            dto.setTax(BigDecimal.ZERO);
            dto.setShipping(BigDecimal.ZERO);
            dto.setDiscount(BigDecimal.ZERO);
        }
        // Pickup token is now on ShopOrder, get from first shop order if available
        if (order.getShopOrders() != null && !order.getShopOrders().isEmpty()) {
            com.ecommerce.entity.ShopOrder firstShopOrder = order.getShopOrders().iterator().next();
            dto.setPickupToken(firstShopOrder.getPickupToken());
            dto.setPickupTokenUsed(firstShopOrder.getPickupTokenUsed());
        } else {
            dto.setPickupToken(null);
            dto.setPickupTokenUsed(false);
        }

        // Set customer info
        if (order.getOrderCustomerInfo() != null) {
            OrderResponseDTO.CustomerInfo customerInfo = new OrderResponseDTO.CustomerInfo();
            customerInfo.setName(order.getOrderCustomerInfo().getFullName());
            customerInfo.setEmail(order.getOrderCustomerInfo().getEmail());
            customerInfo.setPhone(order.getOrderCustomerInfo().getPhoneNumber());
            dto.setCustomerInfo(customerInfo);
            log.debug("Set customer info: {}", customerInfo);
        } else {
            log.warn("OrderCustomerInfo is null for order: {}", order.getId());
        }

        // Set shipping address with coordinates
        if (order.getOrderCustomerInfo() != null) {
            OrderResponseDTO.ShippingAddress shippingAddress = new OrderResponseDTO.ShippingAddress();
            shippingAddress.setStreet(order.getOrderCustomerInfo().getStreetAddress());
            shippingAddress.setCity(order.getOrderCustomerInfo().getCity());
            shippingAddress.setState(order.getOrderCustomerInfo().getState());
            shippingAddress.setCountry(order.getOrderCustomerInfo().getCountry());

            // Add coordinates from OrderAddress if available
            if (order.getOrderAddress() != null) {
                shippingAddress.setLatitude(order.getOrderAddress().getLatitude());
                shippingAddress.setLongitude(order.getOrderAddress().getLongitude());
            }

            dto.setShippingAddress(shippingAddress);
        }

        // Set payment info
        if (order.getOrderTransaction() != null) {
            dto.setPaymentStatus(order.getOrderTransaction().getStatus().toString());
            dto.setPaymentMethod(order.getOrderTransaction().getPaymentMethod().toString());
            dto.setTransaction(convertToTransactionDTO(order.getOrderTransaction()));
        }

        // Set order items (simplified - you may want to add more details)
        if (order.getAllItems() != null) {
            List<OrderResponseDTO.OrderItem> items = order.getAllItems().stream()
                    .map(this::convertToOrderItemDTO)
                    .collect(Collectors.toList());
            dto.setItems(items);
        }

        return dto;
    }

    /**
     * Convert OrderTransaction to OrderTransactionDTO
     */
    private OrderTransactionDTO convertToTransactionDTO(OrderTransaction transaction) {
        OrderTransactionDTO dto = new OrderTransactionDTO();
        dto.setOrderTransactionId(transaction.getOrderTransactionId().toString());
        dto.setOrderAmount(transaction.getOrderAmount());
        dto.setPaymentMethod(transaction.getPaymentMethod().toString());
        dto.setTransactionRef(transaction.getTransactionRef());
        dto.setStatus(transaction.getStatus().toString());
        dto.setReceiptUrl(transaction.getReceiptUrl());
        dto.setStripeSessionId(transaction.getStripeSessionId());
        dto.setStripePaymentIntentId(transaction.getStripePaymentIntentId());
        dto.setPaymentDate(transaction.getPaymentDate());
        dto.setPointsUsed(transaction.getPointsUsed());
        dto.setPointsValue(transaction.getPointsValue());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
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
            if (orderItem.getProduct().getImages() != null && !orderItem.getProduct().getImages().isEmpty()) {
                List<String> imageUrls = orderItem.getProduct().getImages().stream()
                        .sorted((img1, img2) -> {
                            if (img1.isPrimary() && !img2.isPrimary())
                                return -1;
                            if (!img1.isPrimary() && img2.isPrimary())
                                return 1;
                            int sortOrder1 = img1.getSortOrder() != null ? img1.getSortOrder() : 0;
                            int sortOrder2 = img2.getSortOrder() != null ? img2.getSortOrder() : 0;
                            return Integer.compare(sortOrder1, sortOrder2);
                        })
                        .map(img -> img.getImageUrl())
                        .filter(url -> url != null && !url.trim().isEmpty())
                        .collect(Collectors.toList());
                product.setImages(imageUrls);
            }

            dto.setProduct(product);
        }

        // Set variant info if available
        if (orderItem.getProductVariant() != null) {
            OrderResponseDTO.Variant variant = new OrderResponseDTO.Variant();
            variant.setId(orderItem.getProductVariant().getId());
            variant.setName(orderItem.getProductVariant().getVariantName());

            // Add variant images if available (variants may have their own images)
            if (orderItem.getProductVariant().getImages() != null
                    && !orderItem.getProductVariant().getImages().isEmpty()) {
                List<String> variantImageUrls = orderItem.getProductVariant().getImages().stream()
                        .sorted((img1, img2) -> {
                            if (img1.isPrimary() && !img2.isPrimary())
                                return -1;
                            if (!img1.isPrimary() && img2.isPrimary())
                                return 1;
                            int sortOrder1 = img1.getSortOrder() != null ? img1.getSortOrder() : 0;
                            int sortOrder2 = img2.getSortOrder() != null ? img2.getSortOrder() : 0;
                            return Integer.compare(sortOrder1, sortOrder2);
                        })
                        .map(img -> img.getImageUrl())
                        .filter(url -> url != null && !url.trim().isEmpty())
                        .collect(Collectors.toList());
                variant.setImages(variantImageUrls);
            }

            dto.setVariant(variant);
        }

        // Set return eligibility - calculate based on order date and product return
        // policy
        int maxReturnDays = 30; // Default return period
        dto.setMaxReturnDays(maxReturnDays);

        // Calculate days remaining for return based on order creation date
        LocalDateTime orderDate = orderItem.getShopOrder().getOrder().getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        long daysSinceOrder = java.time.temporal.ChronoUnit.DAYS.between(orderDate, now);
        int daysRemaining = (int) (maxReturnDays - daysSinceOrder);

        // Item is return eligible if within return window and shop order is
        // delivered/processing
        boolean isReturnEligible = daysRemaining > 0 &&
                (orderItem.getShopOrder().getStatus() == com.ecommerce.entity.ShopOrder.ShopOrderStatus.DELIVERED ||
                        orderItem.getShopOrder()
                                .getStatus() == com.ecommerce.entity.ShopOrder.ShopOrderStatus.PROCESSING);

        dto.setReturnEligible(isReturnEligible);
        dto.setDaysRemainingForReturn(Math.max(0, daysRemaining));

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

    @Override
    public String getEmailFromToken(String token) {
        return getEmailByToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByNumberWithToken(String orderNumber, String token) {
        Optional<OrderTrackingToken> trackingToken = tokenRepository
                .findValidToken(token, LocalDateTime.now());

        if (trackingToken.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired tracking token");
        }

        String email = trackingToken.get().getEmail();

        // Find order by order number and verify it belongs to the email
        Optional<Order> orderOpt = orderRepository.findByOrderCode(orderNumber);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        Order order = orderOpt.get();

        // Verify the order belongs to the email associated with the token
        if (order.getOrderCustomerInfo() == null ||
                !email.equalsIgnoreCase(order.getOrderCustomerInfo().getEmail())) {
            throw new IllegalArgumentException("Order not found for this email");
        }

        return convertToOrderResponseDTO(order);
    }
}
