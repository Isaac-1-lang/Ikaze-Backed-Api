package com.ecommerce.service;

import com.ecommerce.dto.ReturnDecisionDTO;
import com.ecommerce.entity.ReturnRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for handling refund processing based on original payment method
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefundService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StripeService stripeService;
    private final RewardService rewardService;
    private final MoneyFlowService moneyFlowService;

    /**
     * Process refund for approved return request based on original payment method
     */
    public void processRefund(ReturnRequest returnRequest, ReturnDecisionDTO.RefundDetailsDTO refundDetails) {
        log.info("Processing refund for return request {}", returnRequest.getId());
        
        try {
            // Get the order and transaction details
            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));
            
            OrderTransaction transaction = order.getOrderTransaction();
            if (transaction == null) {
                throw new RuntimeException("No transaction found for order: " + order.getOrderId());
            }
            
            // Process refund based on original payment method
            switch (transaction.getPaymentMethod()) {
                case CREDIT_CARD:
                case DEBIT_CARD:
                    processOriginalPaymentRefund(returnRequest, order, transaction, refundDetails);
                    break;
                case HYBRID:
                    processHybridRefund(returnRequest, order, transaction, refundDetails);
                    break;
                case POINTS:
                    processPointsRefund(returnRequest, order, transaction, refundDetails);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported payment method: " + transaction.getPaymentMethod());
            }
            
            // Update transaction status
            transaction.setStatus(OrderTransaction.TransactionStatus.REFUNDED);
            transaction.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Notify customer
            notificationService.notifyRefundProcessed(returnRequest, 
                    transaction.getPaymentMethod().name(), refundDetails.getRefundAmount());
            
            log.info("Refund processed successfully for return request {}", returnRequest.getId());
            
        } catch (Exception e) {
            log.error("Failed to process refund for return request {}: {}", 
                    returnRequest.getId(), e.getMessage(), e);
            
            throw new RuntimeException("Refund processing failed", e);
        }
    }

    private void processOriginalPaymentRefund(ReturnRequest returnRequest, Order order, 
                                            OrderTransaction transaction, ReturnDecisionDTO.RefundDetailsDTO refundDetails) {
        log.info("Processing original payment method refund for return request {} - Payment Method: {}", 
                returnRequest.getId(), transaction.getPaymentMethod());
        
        BigDecimal refundAmount = refundDetails.getRefundAmount() != null ? 
                BigDecimal.valueOf(refundDetails.getRefundAmount()) : transaction.getOrderAmount();
        
        if (transaction.getStripeSessionId() != null && transaction.getStripePaymentIntentId() != null) {
            log.info("Processing Stripe refund for payment intent: {}", transaction.getStripePaymentIntentId());
            
            try {
                stripeService.processRefund(transaction.getStripePaymentIntentId(), refundAmount);
                log.info("Stripe refund of {} processed successfully for return request {}", 
                        refundAmount, returnRequest.getId());
                
                recordRefundInMoneyFlow(order, refundAmount, "Card refund");
            } catch (Exception e) {
                log.error("Failed to process Stripe refund: {}", e.getMessage(), e);
                throw new RuntimeException("Stripe refund failed: " + e.getMessage(), e);
            }
        } else {
            log.warn("No Stripe session found for transaction {}, cannot process automatic refund", 
                    transaction.getOrderTransactionId());
        }
    }

    /**
     * Process refund for hybrid payments (partial card + partial points)
     */
    private void processHybridRefund(ReturnRequest returnRequest, Order order, 
                                   OrderTransaction transaction, ReturnDecisionDTO.RefundDetailsDTO refundDetails) {
        log.info("Processing hybrid payment refund for return request {}", returnRequest.getId());
        
        BigDecimal totalRefundAmount = refundDetails.getRefundAmount() != null ? 
                BigDecimal.valueOf(refundDetails.getRefundAmount()) : transaction.getOrderAmount();
        
        // Calculate refund proportions
        BigDecimal cardAmount = transaction.getOrderAmount().subtract(transaction.getPointsValue());
        BigDecimal pointsValue = transaction.getPointsValue();
        Integer pointsUsed = transaction.getPointsUsed();
        
        log.info("Hybrid refund breakdown - Card amount: {}, Points value: {}, Points used: {}", 
                cardAmount, pointsValue, pointsUsed);
        
        // Refund card payment if there was a card component
        if (cardAmount.compareTo(BigDecimal.ZERO) > 0 && transaction.getStripePaymentIntentId() != null) {
            log.info("Processing card refund portion: {}", cardAmount);
            
            try {
                stripeService.processRefund(transaction.getStripePaymentIntentId(), cardAmount);
                log.info("Card portion refund of {} processed successfully", cardAmount);
                
                recordRefundInMoneyFlow(order, cardAmount, "Hybrid refund (Card portion)");
            } catch (Exception e) {
                log.error("Failed to process card refund portion: {}", e.getMessage(), e);
                throw new RuntimeException("Card refund failed: " + e.getMessage(), e);
            }
        }
        
        // Refund points if there were points used
        if (pointsUsed != null && pointsUsed > 0) {
            log.info("Processing points refund: {} points", pointsUsed);
            
            User user = order.getUser();
            if (user != null) {
                String refundDescription = String.format("Points refunded for return request (Order #%s)",
                        order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());
                
                rewardService.refundPointsForCancelledOrder(user.getId(), pointsUsed, refundDescription);
                
                log.info("Refunded {} points to user {} via RewardService", pointsUsed, user.getId());
            } else {
                log.error("Cannot refund points - user not found for order {}", order.getOrderId());
            }
        }

    }

    /**
     * Process refund for points-only payments
     */
    private void processPointsRefund(ReturnRequest returnRequest, Order order, 
                                   OrderTransaction transaction, ReturnDecisionDTO.RefundDetailsDTO refundDetails) {
        log.info("Processing points-only refund for return request {}", returnRequest.getId());
        
        Integer pointsUsed = transaction.getPointsUsed();
        if (pointsUsed == null || pointsUsed <= 0) {
            throw new RuntimeException("No points found to refund for transaction: " + transaction.getOrderTransactionId());
        }
        
        User user = order.getUser();
        if (user == null) {
            throw new RuntimeException("User not found for order: " + order.getOrderId());
        }
        
        // Refund points using RewardService
        String refundDescription = String.format("Points refunded for return request (Order #%s)",
                order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());
        
        rewardService.refundPointsForCancelledOrder(user.getId(), pointsUsed, refundDescription);
        
        log.info("Refunded {} points to user {} via RewardService", pointsUsed, user.getId());
    }

    /**
     * Calculate refund amount based on return request and original order
     */
    public Double calculateRefundAmount(ReturnRequest returnRequest) {
        log.info("Calculating refund amount for return request {}", returnRequest.getId());
        
        try {
            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));
            
            OrderTransaction transaction = order.getOrderTransaction();
            if (transaction == null) {
                throw new RuntimeException("No transaction found for order: " + order.getOrderId());
            }
            
            // For full return, use the original order amount
            BigDecimal refundAmount = transaction.getOrderAmount();
            
            // TODO: Implement logic for partial returns, restocking fees, etc.
            // - Check if it's a partial return (specific items)
            // - Apply any restocking fees
            // - Consider shipping costs
            // - Handle taxes
            
            log.info("Calculated refund amount: {} for return request {}", refundAmount, returnRequest.getId());
            return refundAmount.doubleValue();
            
        } catch (Exception e) {
            log.error("Error calculating refund amount for return request {}: {}", 
                    returnRequest.getId(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Check if refund is eligible
     */
    public boolean isRefundEligible(ReturnRequest returnRequest) {
        try {
            // Check basic conditions
            if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.APPROVED) {
                return false;
            }
            
            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElse(null);
            if (order == null) {
                return false;
            }
            
            OrderTransaction transaction = order.getOrderTransaction();
            if (transaction == null) {
                return false;
            }
            
            // Check if transaction was successful and not already refunded
            return transaction.isSuccessful() && 
                   transaction.getStatus() != OrderTransaction.TransactionStatus.REFUNDED;
                   
        } catch (Exception e) {
            log.error("Error checking refund eligibility for return request {}: {}", 
                    returnRequest.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Record refund in money flow system
     */
    private void recordRefundInMoneyFlow(Order order, BigDecimal refundAmount, String refundType) {
        try {
            String description = String.format("%s for Order #%s", 
                    refundType,
                    order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());
            
            com.ecommerce.dto.CreateMoneyFlowDTO moneyFlowDTO = new com.ecommerce.dto.CreateMoneyFlowDTO();
            moneyFlowDTO.setDescription(description);
            moneyFlowDTO.setType(com.ecommerce.enums.MoneyFlowType.OUT);
            moneyFlowDTO.setAmount(refundAmount);
            moneyFlowService.save(moneyFlowDTO);
            log.info("Recorded money flow OUT: {} for refund on order {}", refundAmount, order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to record money flow for refund on order {}: {}", order.getOrderId(), e.getMessage(), e);
        }
    }
}
