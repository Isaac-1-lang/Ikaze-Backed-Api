package com.ecommerce.service.impl;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsPaymentServiceImpl implements PointsPaymentService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;
    private final RewardService rewardService;
    private final ShippingCostService shippingCostService;
    private final EnhancedStockValidationService stockValidationService;
    private final EnhancedMultiWarehouseAllocator warehouseAllocator;
    private final FEFOStockAllocationService fefoService;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final StripeService stripeService;
    private final EnhancedStockLockService enhancedStockLockService;
    private final OrderEmailService orderEmailService;
    private final MoneyFlowService moneyFlowService;

    @Override
    public PointsPaymentPreviewDTO previewPointsPayment(PointsPaymentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateDeliveryCountry(request.getShippingAddress().getCountry());

        stockValidationService.validateCartItems(request.getItems());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (CartItemDTO item : request.getItems()) {
            BigDecimal itemPrice = BigDecimal.ZERO;
            BigDecimal originalPrice = BigDecimal.ZERO;

            if (item.getVariantId() != null) {
                try {
                    ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                            .orElseThrow(() -> new RuntimeException("Variant not found with ID: " + item.getVariantId()));
                    originalPrice = variant.getPrice();

                    if (variant.getDiscount() != null) {
                        validateDiscountValidity(variant.getDiscount());
                    }

                    itemPrice = calculateDiscountedPrice(variant);
                } catch (Exception e) {
                    throw new RuntimeException("Variant not found with ID: " + item.getVariantId());
                }
            } else if (item.getProductId() != null) {
                try {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found with ID: " + item.getProductId()));
                    originalPrice = product.getPrice();

                    if (product.getDiscount() != null) {
                        validateDiscountValidity(product.getDiscount());
                    }

                    itemPrice = calculateDiscountedPrice(product);
                } catch (Exception e) {
                    throw new RuntimeException("Product not found with ID: " + item.getProductId());
                }
            } else {
                continue;
            }

            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal itemDiscount = originalPrice.subtract(itemPrice)
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(itemTotal);
            discountAmount = discountAmount.add(itemDiscount);
        }

        // Calculate shipping cost with proper error handling (same as CheckoutService)
        BigDecimal shippingCost;
        try {
            shippingCost = shippingCostService.calculateOrderShippingCost(request.getShippingAddress(), request.getItems(), subtotal);
        } catch (Exception e) {
            log.error("Error calculating shipping cost for points payment preview: {}", e.getMessage(), e);
            shippingCost = BigDecimal.valueOf(10.00); // Default fallback
        }

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingCost).add(taxAmount);

        Integer availablePoints = user.getPoints();
        BigDecimal pointsValue = rewardService.calculatePointsValue(availablePoints);
        BigDecimal remainingToPay = totalAmount.subtract(pointsValue).max(BigDecimal.ZERO);
        boolean canPayWithPointsOnly = pointsValue.compareTo(totalAmount) >= 0;

        RewardSystemDTO activeSystem = rewardService.getActiveRewardSystem();
        BigDecimal pointValue = activeSystem != null ? activeSystem.getPointValue() : BigDecimal.ZERO;

        log.info("Points payment preview - Subtotal: {}, Shipping: {}, Total: {}, Available Points: {}, Points Value: {}, Remaining: {}", 
                subtotal, shippingCost, totalAmount, availablePoints, pointsValue, remainingToPay);

        return new PointsPaymentPreviewDTO(
                totalAmount,
                availablePoints,
                pointsValue,
                remainingToPay,
                canPayWithPointsOnly,
                pointValue
        );
    }

    @Override
    public PointsPaymentResult processPointsPayment(PointsPaymentRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PointsPaymentPreviewDTO preview = previewPointsPayment(request);

            if (preview.isCanPayWithPointsOnly()) {
                return processFullPointsPayment(user, request, preview);
            } else {
                return processHybridPayment(user, request, preview);
            }
        } catch (Exception e) {
            log.error("Error processing points payment: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private PointsPaymentResult processFullPointsPayment(User user, PointsPaymentRequest request, 
                                                        PointsPaymentPreviewDTO preview) throws Exception {
        
        log.info("Starting full points payment for user: {}", user.getId());
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = 
                    warehouseAllocator.allocateStockWithFEFO(request.getItems(), request.getShippingAddress());
            log.info("Stock allocation completed successfully");

            Order order = createOrderFromRequest(request, user, allocations, preview.getTotalAmount(), true);
            log.info("Order created successfully");
            
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved with ID: {}", savedOrder.getOrderId());

            for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : allocations.entrySet()) {
                log.info("Committing allocation for cart item: {}", entry.getKey().getProductId());
                fefoService.commitAllocation(entry.getValue());
                log.info("FEFO allocation committed successfully");
                
                createOrderItemBatches(savedOrder, entry.getKey(), entry.getValue());
                log.info("Order item batches created successfully");
            }
            log.info("All batch allocations committed successfully");

            Integer pointsToUse = calculatePointsNeeded(preview.getTotalAmount(), preview.getPointValue());
            log.info("Points to deduct: {}", pointsToUse);
            
            rewardService.deductPointsForPurchase(user.getId(), pointsToUse, 
                    "Points used for order #" + savedOrder.getOrderId());
            log.info("Points deducted successfully");

            OrderTransaction transaction = savedOrder.getOrderTransaction();
            transaction.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
            transaction.setPointsUsed(pointsToUse);
            transaction.setPointsValue(preview.getTotalAmount());
            transaction.setPaymentDate(LocalDateTime.now());
            transactionRepository.save(transaction);
            log.info("Transaction updated successfully");

            // Send order confirmation email
            try {
                orderEmailService.sendOrderConfirmationEmail(savedOrder);
                log.info("Order confirmation email sent successfully for points payment order: {}", savedOrder.getOrderId());
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for points payment order {}: {}", savedOrder.getOrderId(), e.getMessage());
            }

            return new PointsPaymentResult(true, "Payment completed successfully", 
                    savedOrder.getOrderId(), savedOrder.getOrderCode(), pointsToUse, preview.getPointsValue(), 
                    BigDecimal.ZERO, null, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private PointsPaymentResult processHybridPayment(User user, PointsPaymentRequest request, 
                                                    PointsPaymentPreviewDTO preview) throws Exception {
        
        log.info("Starting hybrid payment for user: {}", user.getId());
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = 
                    warehouseAllocator.allocateStockWithFEFO(request.getItems(), request.getShippingAddress());
            log.info("Stock allocation completed successfully");

            Order order = createOrderFromRequest(request, user, allocations, preview.getTotalAmount(), false);
            log.info("Order created successfully");
            
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved with ID: {}", savedOrder.getOrderId());

            // Step 3: Create OrderItemBatch records for tracking (but don't commit allocation yet)
            for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : allocations.entrySet()) {
                createOrderItemBatches(savedOrder, entry.getKey(), entry.getValue());
            }

            // Step 4: Lock stock at batch level using session ID
            String sessionId = savedOrder.getOrderTransaction().getStripeSessionId();
            if (sessionId == null) {
                // Generate temporary session ID for locking
                sessionId = "temp_hybrid_" + savedOrder.getOrderId().toString();
            }

            if (!lockStockWithFEFOAllocation(sessionId, request.getItems(), request.getShippingAddress())) {
                log.error("Failed to lock stock for hybrid payment: {}", savedOrder.getOrderId());
                orderRepository.delete(savedOrder);
                throw new IllegalStateException("Unable to secure stock for your order. Please try again.");
            }
            log.info("Stock batches locked successfully for session: {}", sessionId);

            // For hybrid payment, use all available points (up to order total)
            Integer availablePoints = user.getPoints(); // Use User.points directly for performance
            BigDecimal availablePointsValue = rewardService.calculatePointsValue(availablePoints);
            BigDecimal orderTotal = preview.getTotalAmount();
            
            // Use all points if their value is less than order total, otherwise use points worth the order total
            Integer pointsToUse;
            BigDecimal pointsValue;
            if (availablePointsValue.compareTo(orderTotal) <= 0) {
                // Use all available points
                pointsToUse = availablePoints;
                pointsValue = availablePointsValue;
            } else {
                pointsToUse = calculatePointsNeeded(orderTotal, preview.getPointValue());
                pointsValue = rewardService.calculatePointsValue(pointsToUse);
            }
            log.info("Points to deduct: {}, Points value: {}", pointsToUse, pointsValue);
            
            rewardService.deductPointsForPurchase(user.getId(), pointsToUse, 
                    "Partial points payment for order #" + savedOrder.getOrderId());
            log.info("Points deducted successfully");

            // Calculate the reduced amount to charge via Stripe (after points deduction)
            BigDecimal remainingAmount = orderTotal.subtract(pointsValue);
            log.info("Order total: {}, Points value: {}, Remaining to charge: {}", 
                    orderTotal, pointsValue, remainingAmount);

            String stripeSessionId = stripeService.createCheckoutSessionForHybridPayment(
                    savedOrder, 
                    "usd", 
                    "web",
                    remainingAmount
            );
            log.info("Stripe session created: {}", stripeSessionId);

            // Transfer batch locks from temporary session to actual Stripe session
            OrderTransaction transaction = savedOrder.getOrderTransaction();
            if (transaction.getStripeSessionId() != null && !transaction.getStripeSessionId().equals(sessionId)) {
                transferBatchLocks(sessionId, transaction.getStripeSessionId());
            }

            // Update the existing OrderTransaction with points info (StripeService already set the session ID)
            transaction.setPointsUsed(pointsToUse);
            transaction.setPointsValue(pointsValue);
            // Don't set stripeSessionId here - StripeService already did it
            transactionRepository.save(transaction);
            log.info("Transaction updated with points information successfully");

            return new PointsPaymentResult(true, "Hybrid payment initiated", 
                    savedOrder.getOrderId(), savedOrder.getOrderCode(), pointsToUse, pointsValue, 
                    preview.getRemainingToPay(), stripeSessionId, true);
    }

    @Override
    public PointsPaymentResult completeHybridPayment(UUID userId, Long orderId, String stripeSessionId) {
        try {
            OrderTransaction transaction = transactionRepository.findByStripeSessionId(stripeSessionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Confirm batch locks (this will reduce actual batch quantities)
            enhancedStockLockService.confirmBatchLocks(stripeSessionId);
            log.info("Confirmed batch locks for hybrid payment session: {}", stripeSessionId);

            transaction.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
            transaction.setPaymentDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Get order to retrieve orderNumber
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Record money flow for hybrid payment (only the financial part)
            recordHybridPaymentInMoneyFlow(order, transaction);
            
            // Send order confirmation email for completed hybrid payment
            try {
                orderEmailService.sendOrderConfirmationEmail(order);
                log.info("Order confirmation email sent successfully for hybrid payment order: {}", order.getOrderId());
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for hybrid payment order {}: {}", order.getOrderId(), e.getMessage());
            }
            
            return new PointsPaymentResult(true, "Hybrid payment completed", 
                    orderId, order.getOrderCode(), transaction.getPointsUsed(), transaction.getPointsValue(), 
                    BigDecimal.ZERO, null, false);
        } catch (Exception e) {
            log.error("Error completing hybrid payment: {}", e.getMessage(), e);
            return new PointsPaymentResult(false, "Failed to complete payment: " + e.getMessage(), 
                    null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, false);
        }
    }

    /**
     * Simple subtotal calculation (legacy method - kept for backward compatibility)
     * Note: This method doesn't handle discounts properly. Use previewPointsPayment for accurate calculations.
     */
    private BigDecimal calculateSimpleSubtotal(List<CartItemDTO> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculatePointsNeeded(BigDecimal amount, BigDecimal pointValue) {
        if (pointValue.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return amount.divide(pointValue, 0, RoundingMode.HALF_UP).intValue();
    }

    private Order createOrderFromRequest(PointsPaymentRequest request, User user, 
                                       Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations,
                                       BigDecimal totalAmount, boolean isFullPointsPayment) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderCode(generateOrderNumber());
        order.setOrderStatus(Order.OrderStatus.PROCESSING);

        BigDecimal subtotal = calculateSimpleSubtotal(request.getItems());
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setSubtotal(subtotal); // Products total before discounts, taxes, shipping
        orderInfo.setShippingCost(shippingCostService.calculateOrderShippingCost(request.getShippingAddress(), request.getItems(), subtotal));
        orderInfo.setTotalAmount(totalAmount);
        orderInfo.setOrder(order);
        order.setOrderInfo(orderInfo);

        OrderAddress orderAddress = new OrderAddress();
        orderAddress.setStreet(request.getShippingAddress().getStreetAddress());
        orderAddress.setRegions(request.getShippingAddress().getCity() + "," + request.getShippingAddress().getState());
        orderAddress.setCountry(request.getShippingAddress().getCountry());
        orderAddress.setLatitude(request.getShippingAddress().getLatitude());
        orderAddress.setLongitude(request.getShippingAddress().getLongitude());
        orderAddress.setOrder(order);
        order.setOrderAddress(orderAddress);

        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(user.getFirstName());
        customerInfo.setLastName(user.getLastName());
        customerInfo.setEmail(user.getUserEmail());
        customerInfo.setPhoneNumber(user.getPhoneNumber());
        customerInfo.setOrder(order);
        order.setOrderCustomerInfo(customerInfo);

        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(totalAmount);
        tx.setPaymentMethod(isFullPointsPayment ? OrderTransaction.PaymentMethod.POINTS : OrderTransaction.PaymentMethod.HYBRID);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemDTO cartItem : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            
            if (cartItem.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(cartItem.getVariantId())
                        .orElseThrow(() -> new RuntimeException("Product variant not found: " + cartItem.getVariantId()));
                orderItem.setProductVariant(variant);
            } else {
                Product product = productRepository.findById(cartItem.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));
                orderItem.setProduct(product);
            }
            
            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);

        return order;
    }

    private void createOrderItemBatches(Order order, CartItemDTO cartItem, 
                                      List<FEFOStockAllocationService.BatchAllocation> allocations) {
        log.info("Creating order item batches for cart item: {}", cartItem.getProductId());
        OrderItem orderItem = findOrderItemByCartItem(order, cartItem);
        log.info("Found order item for product: {}", cartItem.getProductId());
        
        for (FEFOStockAllocationService.BatchAllocation allocation : allocations) {
            log.info("Creating batch allocation: batch={}, quantity={}", 
                    allocation.getStockBatch().getBatchNumber(), allocation.getQuantityAllocated());
                    
            OrderItemBatch orderItemBatch = new OrderItemBatch();
            orderItemBatch.setOrderItem(orderItem);
            orderItemBatch.setStockBatch(allocation.getStockBatch());
            orderItemBatch.setWarehouse(allocation.getWarehouse());
            orderItemBatch.setQuantityUsed(allocation.getQuantityAllocated());
            
            orderItemBatchRepository.save(orderItemBatch);
            log.info("Order item batch saved successfully");
        }
        log.info("All order item batches created successfully");
    }

    private OrderItem findOrderItemByCartItem(Order order, CartItemDTO cartItem) {
        return order.getOrderItems().stream()
                .filter(item -> {
                    // If cart item has variant, match by variant
                    if (cartItem.getVariantId() != null) {
                        return item.getProductVariant() != null && 
                               item.getProductVariant().getId().equals(cartItem.getVariantId());
                    } else {
                        // If no variant, match by product ID
                        return item.getProduct() != null && 
                               item.getProduct().getProductId().equals(cartItem.getProductId());
                    }
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found for product: " + cartItem.getProductId() + 
                                                      (cartItem.getVariantId() != null ? ", variant: " + cartItem.getVariantId() : "")));
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    /**
     * Lock stock at batch level using FEFO allocation for hybrid payments
     */
    private boolean lockStockWithFEFOAllocation(String sessionId, List<CartItemDTO> items, AddressDto shippingAddress) {
        try {
            log.info("Starting FEFO allocation and batch locking for hybrid payment session: {}", sessionId);

            // Step 1: Perform FEFO allocation to determine which batches to use
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations = warehouseAllocator
                    .allocateStockWithFEFO(items, shippingAddress);

            if (fefoAllocations.isEmpty()) {
                log.error("FEFO allocation failed - no stock available for hybrid payment");
                return false;
            }

            // Step 2: Convert FEFO allocations to batch lock requests
            List<EnhancedStockLockService.BatchLockRequest> lockRequests = new ArrayList<>();

            for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : fefoAllocations
                    .entrySet()) {
                CartItemDTO item = entry.getKey();
                List<FEFOStockAllocationService.BatchAllocation> allocations = entry.getValue();

                for (FEFOStockAllocationService.BatchAllocation allocation : allocations) {
                    // Get product/variant names for tracking
                    String productName = getProductName(item);
                    String variantName = getVariantName(item);

                    EnhancedStockLockService.BatchLockRequest lockRequest = new EnhancedStockLockService.BatchLockRequest(
                            allocation.getStockBatch().getId(),
                            allocation.getQuantityAllocated(),
                            allocation.getWarehouse().getId(),
                            productName,
                            variantName);

                    lockRequests.add(lockRequest);
                }
            }

            // Step 3: Lock the batches (this will temporarily reduce quantities)
            boolean lockSuccess = enhancedStockLockService.lockStockBatches(sessionId, lockRequests);

            if (lockSuccess) {
                log.info("Successfully locked {} batches for hybrid payment session: {}", lockRequests.size(), sessionId);
            } else {
                log.error("Failed to lock batches for hybrid payment session: {}", sessionId);
            }

            return lockSuccess;

        } catch (Exception e) {
            log.error("Error during FEFO allocation and batch locking for hybrid payment session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get product name for tracking
     */
    private String getProductName(CartItemDTO item) {
        try {
            if (item.getProductId() != null) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    return product.getProductName();
                }
            }
        } catch (Exception e) {
            log.warn("Error getting product name for cart item: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get variant name for tracking
     */
    private String getVariantName(CartItemDTO item) {
        try {
            if (item.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
                if (variant != null) {
                    return variant.getVariantName();
                }
            }
        } catch (Exception e) {
            log.warn("Error getting variant name for cart item: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Transfer batch locks from temporary session to actual session
     */
    private void transferBatchLocks(String fromSessionId, String toSessionId) {
        try {
            enhancedStockLockService.transferBatchLocks(fromSessionId, toSessionId);
            log.info("Successfully transferred batch locks from {} to {}", fromSessionId, toSessionId);
        } catch (Exception e) {
            log.error("Failed to transfer batch locks from {} to {}: {}", fromSessionId, toSessionId, e.getMessage());
            throw new RuntimeException("Failed to transfer batch locks: " + e.getMessage(), e);
        }
    }

    private RewardSystem getActiveRewardSystemEntity() {
        RewardSystemDTO dto = rewardService.getActiveRewardSystem();
        if (dto == null) {
            return null;
        }
        RewardSystem system = new RewardSystem();
        system.setId(dto.getId());
        system.setPointValue(dto.getPointValue());
        system.setIsSystemEnabled(dto.getIsSystemEnabled());
        return system;
    }

    /**
     * Calculate discounted price for ProductVariant (same logic as CheckoutService)
     */
    private BigDecimal calculateDiscountedPrice(ProductVariant variant) {
        if (variant.getDiscount() != null && variant.getDiscount().isValid() && variant.getDiscount().isActive()) {
            if (validateDiscountValidity(variant.getDiscount())) {
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                        variant.getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
                return variant.getPrice().multiply(discountMultiplier);
            }
        }

        if (variant.getProduct() != null && variant.getProduct().getDiscount() != null
                && variant.getProduct().getDiscount().isValid() && variant.getProduct().getDiscount().isActive()) {
            if (validateDiscountValidity(variant.getProduct().getDiscount())) {
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                        variant.getProduct().getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
                return variant.getPrice().multiply(discountMultiplier);
            }
        }

        if (variant.getProduct() != null && variant.getProduct().isOnSale()
                && variant.getProduct().getSalePercentage() != null
                && variant.getProduct().getSalePercentage() > 0) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(variant.getProduct().getSalePercentage()).divide(BigDecimal.valueOf(100.0)));
            return variant.getPrice().multiply(discountMultiplier);
        }

        return variant.getPrice();
    }

    /**
     * Calculate discounted price for Product (same logic as CheckoutService)
     */
    private BigDecimal calculateDiscountedPrice(Product product) {
        // Check product discount
        if (product.getDiscount() != null && product.getDiscount().isValid() && product.getDiscount().isActive()) {
            if (validateDiscountValidity(product.getDiscount())) {
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                        product.getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
                return product.getPrice().multiply(discountMultiplier);
            }
            // If discount is invalid, continue to check sale price
        }

        if (product.isOnSale() && product.getSalePercentage() != null && product.getSalePercentage() > 0) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(product.getSalePercentage()).divide(BigDecimal.valueOf(100.0)));
            return product.getPrice().multiply(discountMultiplier);
        }

        return product.getPrice();
    }

    /**
     * Validate discount validity (same logic as CheckoutService)
     */
    private boolean validateDiscountValidity(com.ecommerce.entity.Discount discount) {
        LocalDateTime now = LocalDateTime.now();

        if (!discount.isActive()) {
            log.warn("Skipping inactive discount: {}", discount.getDiscountId());
            return false;
        }

        if (now.isBefore(discount.getStartDate())) {
            log.warn("Skipping discount that hasn't started yet: {}", discount.getDiscountId());
            return false;
        }

        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            log.warn("Skipping expired discount: {}", discount.getDiscountId());
            return false;
        }

        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            log.warn("Skipping discount with exceeded usage limit: {}", discount.getDiscountId());
            return false;
        }

        log.info("Discount validation passed for: {}", discount.getDiscountId());
        return true;
    }

    /**
     * Validate if we have at least one warehouse in the delivery country
     */
    private void validateDeliveryCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery country is required");
        }

        boolean hasWarehouseInCountry = warehouseRepository.existsByCountryIgnoreCase(country.trim());
        
        if (!hasWarehouseInCountry) {
            log.warn("No warehouse found in country: {}", country);
            throw new IllegalArgumentException("Sorry, we don't deliver to " + country + " as we don't have any warehouses there.");
        }
        
    }

    /**
     * Record hybrid payment in money flow system (only the financial part, not points)
     */
    private void recordHybridPaymentInMoneyFlow(Order order, OrderTransaction transaction) {
        try {
            BigDecimal pointsValue = transaction.getPointsValue() != null ? 
                    transaction.getPointsValue() : BigDecimal.ZERO;
            BigDecimal financialAmount = transaction.getOrderAmount().subtract(pointsValue);
            
            // Only record if there's actual money involved
            if (financialAmount.compareTo(BigDecimal.ZERO) > 0) {
                String description = String.format("Hybrid payment received for Order #%s (Points: %d, Cash: $%.2f)", 
                        order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString(),
                        transaction.getPointsUsed(),
                        financialAmount);
                
                com.ecommerce.dto.CreateMoneyFlowDTO moneyFlowDTO = new com.ecommerce.dto.CreateMoneyFlowDTO();
                moneyFlowDTO.setDescription(description);
                moneyFlowDTO.setType(com.ecommerce.enums.MoneyFlowType.IN);
                moneyFlowDTO.setAmount(financialAmount);
                
                moneyFlowService.save(moneyFlowDTO);
                log.info("Recorded money flow IN: {} for hybrid payment order {}", financialAmount, order.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to record money flow for hybrid payment order {}: {}", order.getOrderId(), e.getMessage(), e);
            // Don't throw exception - payment already succeeded, this is just tracking
        }
    }
}
