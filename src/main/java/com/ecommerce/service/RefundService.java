package com.ecommerce.service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.ReturnDecisionDTO;
import com.ecommerce.entity.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ReturnItemRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final ReturnItemRepository returnItemRepository;
    private final ShippingCostService shippingCostService;

    /**
     * Process refund for approved return request based on original payment method
     * Handles full and partial refunds with shipping recalculation
     */
    public void processRefund(ReturnRequest returnRequest) {
        log.info("Processing refund for return request {}", returnRequest.getId());
        try {
            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));

            OrderTransaction transaction = order.getOrderTransaction();
            if (transaction == null) {
                throw new RuntimeException("No transaction found for order: " + order.getOrderId());
            }

            RefundCalculation refundCalc = calculateRefundAmounts(returnRequest, order);

            log.info(
                    "Refund calculation for return request {}: isFullReturn={}, itemsRefund={}, shippingRefund={}, totalRefund={}",
                    returnRequest.getId(), refundCalc.isFullReturn, refundCalc.itemsRefundAmount,
                    refundCalc.shippingRefundAmount, refundCalc.totalRefundAmount);

            switch (transaction.getPaymentMethod()) {
                case CREDIT_CARD:
                case DEBIT_CARD:
                    processCardRefund(returnRequest, order, transaction, refundCalc);
                    break;
                case HYBRID:
                    processHybridRefund(returnRequest, order, transaction, refundCalc);
                    break;
                case POINTS:
                    processPointsRefund(returnRequest, order, transaction, refundCalc);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported payment method: " + transaction.getPaymentMethod());
            }

            // Update transaction status only if fully refunded
            if (refundCalc.isFullReturn) {
                transaction.setStatus(OrderTransaction.TransactionStatus.REFUNDED);
                transaction.setUpdatedAt(java.time.LocalDateTime.now());
            }

            // Notify customer
            notificationService.notifyRefundProcessed(returnRequest,
                    transaction.getPaymentMethod().name(), refundCalc.totalRefundAmount.doubleValue());

            log.info("Refund processed successfully for return request {}", returnRequest.getId());

        } catch (Exception e) {
            log.error("Failed to process refund for return request {}: {}",
                    returnRequest.getId(), e.getMessage(), e);

            throw new RuntimeException("Refund processing failed", e);
        }
    }

    /**
     * Process card payment refund (credit/debit card)
     */
    private void processCardRefund(ReturnRequest returnRequest, Order order,
            OrderTransaction transaction, RefundCalculation refundCalc) {
        log.info("Processing card refund for return request {} - Amount: {}",
                returnRequest.getId(), refundCalc.totalRefundAmount);

        if (transaction.getStripeSessionId() != null && transaction.getStripePaymentIntentId() != null) {
            log.info("Processing Stripe refund for payment intent: {}", transaction.getStripePaymentIntentId());

            try {
                stripeService.processRefund(transaction.getStripePaymentIntentId(), refundCalc.totalRefundAmount);
                log.info("Stripe refund of {} processed successfully for return request {}",
                        refundCalc.totalRefundAmount, returnRequest.getId());

                String refundType = refundCalc.isFullReturn ? "Full card refund" : "Partial card refund";
                recordRefundInMoneyFlow(order, refundCalc.totalRefundAmount, refundType);
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
     * Proportionally refunds both card and points based on return amount
     */
    private void processHybridRefund(ReturnRequest returnRequest, Order order,
            OrderTransaction transaction, RefundCalculation refundCalc) {
        log.info("Processing hybrid payment refund for return request {}", returnRequest.getId());

        BigDecimal originalCardAmount = transaction.getOrderAmount().subtract(transaction.getPointsValue());
        BigDecimal originalPointsValue = transaction.getPointsValue();
        Integer originalPointsUsed = transaction.getPointsUsed();
        BigDecimal originalTotal = transaction.getOrderAmount();

        // Calculate proportional refunds
        BigDecimal cardRefundAmount;
        Integer pointsToRefund;

        if (refundCalc.isFullReturn) {
            // Full refund - return everything
            cardRefundAmount = originalCardAmount;
            pointsToRefund = originalPointsUsed;
        } else {
            // Partial refund - calculate proportionally
            BigDecimal refundRatio = refundCalc.totalRefundAmount.divide(originalTotal, 4, RoundingMode.HALF_UP);
            cardRefundAmount = originalCardAmount.multiply(refundRatio).setScale(2, RoundingMode.HALF_UP);
            pointsToRefund = (int) Math.round(originalPointsUsed * refundRatio.doubleValue());
        }

        log.info("Hybrid refund breakdown - Card refund: {}, Points refund: {}",
                cardRefundAmount, pointsToRefund);

        // Refund card payment if there was a card component
        if (cardRefundAmount.compareTo(BigDecimal.ZERO) > 0 && transaction.getStripePaymentIntentId() != null) {
            log.info("Processing card refund portion: {}", cardRefundAmount);

            try {
                stripeService.processRefund(transaction.getStripePaymentIntentId(), cardRefundAmount);
                log.info("Card portion refund of {} processed successfully", cardRefundAmount);

                String refundType = refundCalc.isFullReturn ? "Full hybrid refund (Card portion)"
                        : "Partial hybrid refund (Card portion)";
                recordRefundInMoneyFlow(order, cardRefundAmount, refundType);
            } catch (Exception e) {
                log.error("Failed to process card refund portion: {}", e.getMessage(), e);
                throw new RuntimeException("Card refund failed: " + e.getMessage(), e);
            }
        }

        // Refund points if there were points used
        if (pointsToRefund != null && pointsToRefund > 0) {
            log.info("Processing points refund: {} points", pointsToRefund);

            User user = order.getUser();
            if (user != null) {
                String refundDescription = String.format("Points refunded for %s (Order #%s)",
                        refundCalc.isFullReturn ? "full return" : "partial return",
                        order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());

                rewardService.refundPointsForCancelledOrder(user.getId(), pointsToRefund, refundDescription);

                log.info("Refunded {} points to user {} via RewardService", pointsToRefund, user.getId());
            } else {
                log.error("Cannot refund points - user not found for order {}", order.getOrderId());
            }
        }
    }

    /**
     * Process refund for points-only payments
     * Proportionally refunds points based on return amount
     */
    private void processPointsRefund(ReturnRequest returnRequest, Order order,
            OrderTransaction transaction, RefundCalculation refundCalc) {
        log.info("Processing points-only refund for return request {}", returnRequest.getId());

        Integer originalPointsUsed = transaction.getPointsUsed();
        if (originalPointsUsed == null || originalPointsUsed <= 0) {
            throw new RuntimeException(
                    "No points found to refund for transaction: " + transaction.getOrderTransactionId());
        }

        User user = order.getUser();
        if (user == null) {
            throw new RuntimeException("User not found for order: " + order.getOrderId());
        }

        // Calculate points to refund
        Integer pointsToRefund;
        if (refundCalc.isFullReturn) {
            pointsToRefund = originalPointsUsed;
        } else {
            // Partial refund - calculate proportionally
            BigDecimal originalTotal = transaction.getPointsValue();
            BigDecimal refundRatio = refundCalc.totalRefundAmount.divide(originalTotal, 4, RoundingMode.HALF_UP);
            pointsToRefund = (int) Math.round(originalPointsUsed * refundRatio.doubleValue());
        }

        // Refund points using RewardService
        String refundDescription = String.format("Points refunded for %s (Order #%s)",
                refundCalc.isFullReturn ? "full return" : "partial return",
                order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());

        rewardService.refundPointsForCancelledOrder(user.getId(), pointsToRefund, refundDescription);

        log.info("Refunded {} points to user {} via RewardService", pointsToRefund, user.getId());
    }

    /**
     * Calculate refund amount based on return request and original order
     * Legacy method for backward compatibility
     */
    public Double calculateRefundAmount(ReturnRequest returnRequest) {
        try {
            Order order = orderRepository.findById(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));

            RefundCalculation refundCalc = calculateRefundAmounts(returnRequest, order);
            return refundCalc.totalRefundAmount.doubleValue();
        } catch (Exception e) {
            log.error("Error calculating refund amount for return request {}: {}",
                    returnRequest.getId(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Comprehensive refund calculation handling full vs partial returns with
     * shipping recalculation
     * This is the core logic that determines how much to refund
     */
    private RefundCalculation calculateRefundAmounts(ReturnRequest returnRequest, Order order) {
        log.info("Calculating comprehensive refund for return request {}", returnRequest.getId());

        // Get return items for this request
        List<ReturnItem> returnItems = returnItemRepository.findByReturnRequestId(returnRequest.getId());
        if (returnItems == null || returnItems.isEmpty()) {
            throw new RuntimeException("No return items found for return request: " + returnRequest.getId());
        }

        // Check if this is a full return (all items returned)
        boolean isFullReturn = isFullOrderReturn(order, returnItems);

        RefundCalculation refundCalc = new RefundCalculation();
        refundCalc.isFullReturn = isFullReturn;

        if (isFullReturn) {
            // Full return - refund everything including full shipping
            refundCalc.itemsRefundAmount = calculateFullItemsRefund(order);
            refundCalc.shippingRefundAmount = order.getOrderInfo() != null ? order.getOrderInfo().getShippingCost()
                    : BigDecimal.ZERO;
            refundCalc.totalRefundAmount = refundCalc.itemsRefundAmount.add(refundCalc.shippingRefundAmount);

            log.info("Full return detected - Items: {}, Shipping: {}, Total: {}",
                    refundCalc.itemsRefundAmount, refundCalc.shippingRefundAmount, refundCalc.totalRefundAmount);
        } else {
            // Partial return - calculate proportional item refund and shipping savings
            refundCalc.itemsRefundAmount = calculatePartialItemsRefund(order, returnItems);
            refundCalc.shippingRefundAmount = calculateShippingRefund(order, returnItems);
            refundCalc.totalRefundAmount = refundCalc.itemsRefundAmount.add(refundCalc.shippingRefundAmount);

            log.info("Partial return detected - Items: {}, Shipping savings: {}, Total: {}",
                    refundCalc.itemsRefundAmount, refundCalc.shippingRefundAmount, refundCalc.totalRefundAmount);
        }

        return refundCalc;
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

    private void recordRefundInMoneyFlow(Order order, BigDecimal refundAmount, String refundType) {
        try {
            if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid refund amount {} for order {}, skipping money flow record",
                        refundAmount, order.getOrderId());
                return;
            }

            String description = String.format("%s for Order #%s",
                    refundType,
                    order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());

            com.ecommerce.dto.CreateMoneyFlowDTO moneyFlowDTO = new com.ecommerce.dto.CreateMoneyFlowDTO();
            moneyFlowDTO.setDescription(description);
            moneyFlowDTO.setType(com.ecommerce.enums.MoneyFlowType.OUT);
            moneyFlowDTO.setAmount(refundAmount);

            com.ecommerce.entity.MoneyFlow savedFlow = moneyFlowService.save(moneyFlowDTO);

            log.info("Recorded money flow OUT: {} for refund on order {} (MoneyFlow ID: {}, New Balance: {})",
                    refundAmount, order.getOrderId(), savedFlow.getId(), savedFlow.getRemainingBalance());
        } catch (Exception e) {
            log.error("Failed to record money flow for refund on order {}: {}",
                    order.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Check if all items in the order are being returned
     */
    private boolean isFullOrderReturn(Order order, List<ReturnItem> returnItems) {
        // Get all order items
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return false;
        }

        // For each order item, check if the total returned quantity (across ALL return
        // requests) equals ordered quantity
        for (OrderItem orderItem : orderItems) {
            int orderedQuantity = orderItem.getQuantity();

            // Get total returned quantity across all approved/completed return requests for
            // this order item
            Integer totalReturnedQuantity = returnItemRepository
                    .getTotalApprovedReturnQuantityForOrderItem(orderItem.getOrderItemId());

            // Add the current return request's quantity for this item
            int currentReturnQuantity = returnItems.stream()
                    .filter(ri -> ri.getOrderItem().getOrderItemId().equals(orderItem.getOrderItemId()))
                    .mapToInt(ReturnItem::getReturnQuantity)
                    .sum();

            int totalAfterThisReturn = totalReturnedQuantity + currentReturnQuantity;

            // If any item is not fully returned, it's a partial return
            if (totalAfterThisReturn < orderedQuantity) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculate full items refund (all items, taxes, discounts)
     */
    private BigDecimal calculateFullItemsRefund(Order order) {
        OrderInfo orderInfo = order.getOrderInfo();
        if (orderInfo == null) {
            // Fallback: calculate from order items
            return order.getOrderItems().stream()
                    .map(OrderItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Return subtotal + tax - discount (everything except shipping)
        BigDecimal refund = orderInfo.getSubtotal() != null ? orderInfo.getSubtotal() : BigDecimal.ZERO;

        if (orderInfo.getTaxAmount() != null) {
            refund = refund.add(orderInfo.getTaxAmount());
        }

        if (orderInfo.getDiscountAmount() != null) {
            refund = refund.subtract(orderInfo.getDiscountAmount());
        }

        return refund;
    }

    /**
     * Calculate partial items refund based on price at purchase
     */
    private BigDecimal calculatePartialItemsRefund(Order order, List<ReturnItem> returnItems) {
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (ReturnItem returnItem : returnItems) {
            OrderItem orderItem = returnItem.getOrderItem();

            // Use price at purchase (from OrderItem)
            BigDecimal pricePerUnit = orderItem.getPrice();
            int returnQuantity = returnItem.getReturnQuantity();

            BigDecimal itemRefund = pricePerUnit.multiply(BigDecimal.valueOf(returnQuantity));
            totalRefund = totalRefund.add(itemRefund);

            log.debug("Refund for item {}: {} x {} = {}",
                    orderItem.getOrderItemId(), pricePerUnit, returnQuantity, itemRefund);
        }

        return totalRefund;
    }

    /**
     * Calculate shipping refund for partial returns
     * Recalculates shipping as if non-returned items were the only ones in the
     * order
     * Refund = Original Shipping - Recalculated Shipping
     */
    private BigDecimal calculateShippingRefund(Order order, List<ReturnItem> returnItems) {
        OrderInfo orderInfo = order.getOrderInfo();
        if (orderInfo == null || orderInfo.getShippingCost() == null) {
            log.warn("No shipping cost found for order {}, returning zero shipping refund", order.getOrderId());
            return BigDecimal.ZERO;
        }

        BigDecimal originalShipping = orderInfo.getShippingCost();

        try {
            List<OrderItem> remainingItems = getRemainingItems(order, returnItems);

            if (remainingItems.isEmpty()) {
                return originalShipping;
            }

            List<CartItemDTO> remainingCartItems = convertToCartItems(remainingItems, returnItems);

            // Get delivery address
            OrderAddress orderAddress = order.getOrderAddress();
            if (orderAddress == null) {
                log.warn("No address found for order {}, cannot recalculate shipping", order.getOrderId());
                return BigDecimal.ZERO;
            }

            AddressDto deliveryAddress = convertToAddressDto(orderAddress);

            BigDecimal recalculatedShipping = shippingCostService.calculateOrderShippingCost(
                    deliveryAddress, remainingCartItems, orderInfo.getSubtotal());

            BigDecimal shippingRefund = originalShipping.subtract(recalculatedShipping);

            if (shippingRefund.compareTo(BigDecimal.ZERO) < 0) {
                shippingRefund = BigDecimal.ZERO;
            }

            log.info("Shipping refund calculation: Original={}, Recalculated={}, Refund={}",
                    originalShipping, recalculatedShipping, shippingRefund);

            return shippingRefund;

        } catch (Exception e) {
            log.error("Failed to recalculate shipping for order {}: {}", order.getOrderId(), e.getMessage(), e);
            // Fallback: no shipping refund on error
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get remaining items after removing returned items
     */
    private List<OrderItem> getRemainingItems(Order order, List<ReturnItem> returnItems) {
        List<OrderItem> remainingItems = new ArrayList<>();

        for (OrderItem orderItem : order.getOrderItems()) {
            int orderedQuantity = orderItem.getQuantity();

            // Get total returned quantity for this item (including current return)
            int returnedQuantity = returnItems.stream()
                    .filter(ri -> ri.getOrderItem().getOrderItemId().equals(orderItem.getOrderItemId()))
                    .mapToInt(ReturnItem::getReturnQuantity)
                    .sum();

            // Get previously returned quantity
            Integer previouslyReturned = returnItemRepository
                    .getTotalApprovedReturnQuantityForOrderItem(orderItem.getOrderItemId());

            int totalReturned = previouslyReturned + returnedQuantity;
            int remainingQuantity = orderedQuantity - totalReturned;

            if (remainingQuantity > 0) {
                // Create a copy with adjusted quantity
                OrderItem remainingItem = new OrderItem();
                remainingItem.setOrderItemId(orderItem.getOrderItemId());
                remainingItem.setProduct(orderItem.getProduct());
                remainingItem.setProductVariant(orderItem.getProductVariant());
                remainingItem.setQuantity(remainingQuantity);
                remainingItem.setPrice(orderItem.getPrice());

                remainingItems.add(remainingItem);
            }
        }

        return remainingItems;
    }

    private List<CartItemDTO> convertToCartItems(List<OrderItem> orderItems, List<ReturnItem> returnItems) {
        List<CartItemDTO> cartItems = new ArrayList<>();

        for (OrderItem orderItem : orderItems) {
            CartItemDTO cartItem = new CartItemDTO();

            if (orderItem.isVariantBased()) {
                cartItem.setVariantId(orderItem.getProductVariant().getId());
                cartItem.setProductId(orderItem.getProductVariant().getProduct().getProductId());
            } else {
                cartItem.setProductId(orderItem.getProduct().getProductId());
            }

            cartItem.setQuantity(orderItem.getQuantity());
            cartItems.add(cartItem);
        }

        return cartItems;
    }

    private AddressDto convertToAddressDto(OrderAddress orderAddress) {
        AddressDto addressDto = new AddressDto();
        addressDto.setStreetAddress(orderAddress.getStreet());
        addressDto.setCountry(orderAddress.getCountry());

        String[] regionsArray = orderAddress.getRegionsArray();
        if (regionsArray.length > 0) {
            addressDto.setState(regionsArray[0]);
            if (regionsArray.length > 1) {
                addressDto.setCity(regionsArray[1]);
            }
        }

        return addressDto;
    }


    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public com.ecommerce.dto.ExpectedRefundDTO calculateExpectedRefund(ReturnRequest returnRequest) {
        try {
            // Eagerly fetch order with all associations to prevent LazyInitializationException
            // This is crucial because we're running outside a transaction (NOT_SUPPORTED)
            Order order = orderRepository.findByIdWithAllAssociations(returnRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + returnRequest.getOrderId()));

            OrderTransaction transaction = order.getOrderTransaction();
            if (transaction == null) {
                throw new RuntimeException("No transaction found for order: " + order.getOrderId());
            }

            RefundCalculation refundCalc = calculateRefundAmounts(returnRequest, order);
            
            com.ecommerce.dto.ExpectedRefundDTO.ExpectedRefundDTOBuilder builder = 
                com.ecommerce.dto.ExpectedRefundDTO.builder()
                    .paymentMethod(transaction.getPaymentMethod().name())
                    .isFullReturn(refundCalc.isFullReturn)
                    .itemsRefund(refundCalc.itemsRefundAmount)
                    .shippingRefund(refundCalc.shippingRefundAmount)
                    .totalRefundValue(refundCalc.totalRefundAmount);

            BigDecimal totalRefundAmount = refundCalc.totalRefundAmount;
            
            switch (transaction.getPaymentMethod()) {
                case CREDIT_CARD:
                case DEBIT_CARD:
                    // Full monetary refund
                    builder.monetaryRefund(totalRefundAmount)
                           .pointsRefund(0)
                           .pointsRefundValue(BigDecimal.ZERO)
                           .refundDescription(String.format("$%.2f will be refunded to your card", 
                                   totalRefundAmount.doubleValue()));
                    break;
                    
                case POINTS:
                    // Full points refund
                    Integer pointsUsed = transaction.getPointsUsed() != null ? transaction.getPointsUsed() : 0;
                    BigDecimal originalPointsValue = transaction.getPointsValue() != null ? 
                            transaction.getPointsValue() : BigDecimal.ZERO;
                    Integer pointsToRefund;
                    BigDecimal pointsRefundValue;
                    
                    if (refundCalc.isFullReturn) {
                        // Full return - refund all points
                        pointsToRefund = pointsUsed;
                        pointsRefundValue = originalPointsValue;
                    } else {
                        // Partial return - calculate proportional points refund
                        BigDecimal refundRatio = totalRefundAmount.divide(
                                transaction.getOrderAmount(), 4, RoundingMode.HALF_UP);
                        pointsToRefund = BigDecimal.valueOf(pointsUsed)
                                .multiply(refundRatio)
                                .setScale(0, RoundingMode.HALF_UP)
                                .intValue();
                        // Calculate proportional points value using original point value
                        pointsRefundValue = originalPointsValue.multiply(refundRatio)
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    
                    BigDecimal pointsValue = pointsRefundValue;
                    
                    builder.monetaryRefund(BigDecimal.ZERO)
                           .pointsRefund(pointsToRefund)
                           .pointsRefundValue(pointsValue)
                           .refundDescription(String.format("%d points (worth $%.2f) will be refunded", 
                                   pointsToRefund, pointsValue.doubleValue()));
                    break;
                    
                case HYBRID:
                    // Hybrid refund - points first, then card
                    BigDecimal pointsValueUsed = transaction.getPointsValue() != null ? 
                            transaction.getPointsValue() : BigDecimal.ZERO;
                    Integer hybridPointsUsed = transaction.getPointsUsed() != null ? 
                            transaction.getPointsUsed() : 0;
                    BigDecimal cardAmount = transaction.getOrderAmount().subtract(pointsValueUsed);
                    
                    Integer hybridPointsToRefund = 0;
                    BigDecimal hybridPointsRefundValue = BigDecimal.ZERO;
                    BigDecimal cardRefundAmount = BigDecimal.ZERO;
                    BigDecimal remainingRefund = totalRefundAmount;
                    
                    if (refundCalc.isFullReturn) {
                        // Full return - refund all points and all card amount
                        hybridPointsToRefund = hybridPointsUsed;
                        hybridPointsRefundValue = pointsValueUsed;
                        cardRefundAmount = cardAmount;
                    } else {
                        // Partial return - prioritize points first
                        if (pointsValueUsed.compareTo(BigDecimal.ZERO) > 0 && 
                            remainingRefund.compareTo(BigDecimal.ZERO) > 0) {
                            // Calculate how much of the refund comes from points
                            BigDecimal pointsPortionOfRefund = remainingRefund.min(pointsValueUsed);
                            BigDecimal pointsRefundRatio = pointsPortionOfRefund.divide(
                                    pointsValueUsed, 4, RoundingMode.HALF_UP);
                            
                            // Calculate proportional points to refund
                            hybridPointsToRefund = BigDecimal.valueOf(hybridPointsUsed)
                                    .multiply(pointsRefundRatio)
                                    .setScale(0, RoundingMode.HALF_UP)
                                    .intValue();
                            
                            // Calculate the value of refunded points using original point value
                            hybridPointsRefundValue = pointsValueUsed.multiply(pointsRefundRatio)
                                    .setScale(2, RoundingMode.HALF_UP);
                            
                            remainingRefund = remainingRefund.subtract(hybridPointsRefundValue);
                        }
                        
                        // Refund remaining amount via card
                        if (remainingRefund.compareTo(BigDecimal.ZERO) > 0) {
                            cardRefundAmount = remainingRefund.min(cardAmount);
                        }
                    }
                    
                    BigDecimal hybridPointsValue = hybridPointsRefundValue;
                    
                    String hybridDesc;
                    if (hybridPointsToRefund > 0 && cardRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
                        hybridDesc = String.format("%d points (worth $%.2f) + $%.2f to your card", 
                                hybridPointsToRefund, hybridPointsValue.doubleValue(), 
                                cardRefundAmount.doubleValue());
                    } else if (hybridPointsToRefund > 0) {
                        hybridDesc = String.format("%d points (worth $%.2f) will be refunded", 
                                hybridPointsToRefund, hybridPointsValue.doubleValue());
                    } else {
                        hybridDesc = String.format("$%.2f will be refunded to your card", 
                                cardRefundAmount.doubleValue());
                    }
                    
                    builder.monetaryRefund(cardRefundAmount)
                           .pointsRefund(hybridPointsToRefund)
                           .pointsRefundValue(hybridPointsValue)
                           .refundDescription(hybridDesc);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unsupported payment method: " + 
                            transaction.getPaymentMethod());
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error calculating expected refund for return request {}: {}", 
                    returnRequest.getId(), e.getMessage(), e);
            // Return a default empty refund
            return com.ecommerce.dto.ExpectedRefundDTO.builder()
                    .monetaryRefund(BigDecimal.ZERO)
                    .pointsRefund(0)
                    .pointsRefundValue(BigDecimal.ZERO)
                    .totalRefundValue(BigDecimal.ZERO)
                    .refundDescription("Unable to calculate refund")
                    .build();
        }
    }

    private static class RefundCalculation {
        boolean isFullReturn;
        BigDecimal itemsRefundAmount = BigDecimal.ZERO;
        BigDecimal shippingRefundAmount = BigDecimal.ZERO;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
    }
}
