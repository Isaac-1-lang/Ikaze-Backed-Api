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
    private final UserPointsRepository userPointsRepository;
    private final ShopRepository shopRepository;
    private final RewardSystemRepository rewardSystemRepository;

    @Override
    public PointsPaymentPreviewDTO previewPointsPayment(PointsPaymentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getShopId() == null) {
            throw new IllegalArgumentException("shopId is required for points payments");
        }

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
                            .orElseThrow(
                                    () -> new RuntimeException("Variant not found with ID: " + item.getVariantId()));
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
                            .orElseThrow(
                                    () -> new RuntimeException("Product not found with ID: " + item.getProductId()));
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
            shippingCost = shippingCostService.calculateOrderShippingCost(request.getShippingAddress(),
                    request.getItems(), subtotal);
        } catch (Exception e) {
            log.error("Error calculating shipping cost for points payment preview: {}", e.getMessage(), e);
            shippingCost = BigDecimal.valueOf(10.00); // Default fallback
        }

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingCost).add(taxAmount);

        Integer availablePoints = getAvailablePointsForShop(user.getId(), request.getShopId());
        BigDecimal pointValue = getPointValueForShop(request.getShopId());
        BigDecimal pointsValue = pointValue.multiply(BigDecimal.valueOf(availablePoints));
        BigDecimal remainingToPay = totalAmount.subtract(pointsValue).max(BigDecimal.ZERO);
        boolean canPayWithPointsOnly = pointsValue.compareTo(totalAmount) >= 0;

        log.info(
                "Points payment preview - Subtotal: {}, Shipping: {}, Total: {}, Available Points: {}, Points Value: {}, Remaining: {}",
                subtotal, shippingCost, totalAmount, availablePoints, pointsValue, remainingToPay);

        return new PointsPaymentPreviewDTO(
                totalAmount,
                availablePoints,
                pointsValue,
                remainingToPay,
                canPayWithPointsOnly,
                pointValue);
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
        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = warehouseAllocator
                .allocateStockWithFEFO(request.getItems(), request.getShippingAddress());
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

        // Record shop-scoped points deduction
        createPointsRecord(user, request.getShopId(), savedOrder.getOrderId(), -pointsToUse,
                preview.getTotalAmount(), "Points used for order #" + savedOrder.getOrderCode());
        log.info("Points deduction recorded successfully");

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
            log.info("Order confirmation email sent successfully for points payment order: {}",
                    savedOrder.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for points payment order {}: {}",
                    savedOrder.getOrderId(), e.getMessage());
        }

        return new PointsPaymentResult(true, "Payment completed successfully",
                savedOrder.getOrderId(), savedOrder.getOrderCode(), pointsToUse, preview.getPointsValue(),
                BigDecimal.ZERO, null, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private PointsPaymentResult processHybridPayment(User user, PointsPaymentRequest request,
            PointsPaymentPreviewDTO preview) throws Exception {

        log.info("Starting hybrid payment for user: {}", user.getId());
        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = warehouseAllocator
                .allocateStockWithFEFO(request.getItems(), request.getShippingAddress());
        log.info("Stock allocation completed successfully");

        Order order = createOrderFromRequest(request, user, allocations, preview.getTotalAmount(), false);
        log.info("Order created successfully");

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}", savedOrder.getOrderId());

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
        Integer availablePoints = getAvailablePointsForShop(user.getId(), request.getShopId());
        BigDecimal pointValue = getPointValueForShop(request.getShopId());
        BigDecimal availablePointsValue = pointValue.multiply(BigDecimal.valueOf(availablePoints));
        BigDecimal orderTotal = preview.getTotalAmount();

        // Use all points if their value is less than order total, otherwise use points
        // worth the order total
        Integer pointsToUse;
        BigDecimal pointsValue;
        if (availablePointsValue.compareTo(orderTotal) <= 0) {
            // Use all available points
            pointsToUse = availablePoints;
            pointsValue = availablePointsValue;
        } else {
            pointsToUse = calculatePointsNeeded(orderTotal, pointValue);
            pointsValue = pointValue.multiply(BigDecimal.valueOf(pointsToUse));
        }
        log.info("Points to deduct: {}, Points value: {}", pointsToUse, pointsValue);

        // Record shop-scoped points deduction
        createPointsRecord(user, request.getShopId(), savedOrder.getOrderId(), -pointsToUse,
                pointsValue, "Partial points payment for order #" + savedOrder.getOrderCode());
        log.info("Points deduction recorded successfully");

        // Calculate the reduced amount to charge via Stripe (after points deduction)
        BigDecimal remainingAmount = orderTotal.subtract(pointsValue);
        log.info("Order total: {}, Points value: {}, Remaining to charge: {}",
                orderTotal, pointsValue, remainingAmount);

        String stripeSessionId = stripeService.createCheckoutSessionForHybridPayment(
                savedOrder,
                "usd",
                "web",
                remainingAmount);
        log.info("Stripe session created: {}", stripeSessionId);

        OrderTransaction transaction = transactionRepository
                .findById(savedOrder.getOrderTransaction().getOrderTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        log.info("Transaction reloaded - Session ID: {}, Payment Intent ID: {}",
                transaction.getStripeSessionId(), transaction.getStripePaymentIntentId());

        if (transaction.getStripeSessionId() != null && !transaction.getStripeSessionId().equals(sessionId)) {
            transferBatchLocks(sessionId, transaction.getStripeSessionId());
        }

        transaction.setPointsUsed(pointsToUse);
        transaction.setPointsValue(pointsValue);
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
                log.error("Failed to send order confirmation email for hybrid payment order {}: {}", order.getOrderId(),
                        e.getMessage());
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
     * Note: This method doesn't handle discounts properly. Use previewPointsPayment
     * for accurate calculations.
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
        // Note: Order status is calculated from shopOrders, not set directly

        // Create OrderInfo to store order totals
        BigDecimal subtotal = calculateSimpleSubtotal(request.getItems());
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setSubtotal(subtotal);
        orderInfo.setShippingCost(shippingCostService.calculateOrderShippingCost(request.getShippingAddress(),
                request.getItems(), subtotal));
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
        tx.setPaymentMethod(
                isFullPointsPayment ? OrderTransaction.PaymentMethod.POINTS : OrderTransaction.PaymentMethod.HYBRID);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        // For multivendor system, we need to create ShopOrders and OrderItems
        // This is a simplified implementation - in a real multivendor system,
        // items would be grouped by shop and separate ShopOrders created
        List<ShopOrder> shopOrders = new ArrayList<>();
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setOrder(order);
        shopOrder.setTotalAmount(totalAmount);
        shopOrder.setStatus(ShopOrder.ShopOrderStatus.PENDING);

        // Set shop from first item (simplified - should group by shop)
        if (!request.getItems().isEmpty()) {
            CartItemDTO firstItem = request.getItems().get(0);
            if (firstItem.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(firstItem.getVariantId())
                        .orElseThrow(
                                () -> new RuntimeException("Product variant not found: " + firstItem.getVariantId()));
                shopOrder.setShop(variant.getProduct().getShop());
            } else {
                Product product = productRepository.findById(firstItem.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + firstItem.getProductId()));
                shopOrder.setShop(product.getShop());
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemDTO cartItem : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setShopOrder(shopOrder);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());

            if (cartItem.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(cartItem.getVariantId())
                        .orElseThrow(
                                () -> new RuntimeException("Product variant not found: " + cartItem.getVariantId()));
                orderItem.setProductVariant(variant);
            } else {
                Product product = productRepository.findById(cartItem.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));
                orderItem.setProduct(product);
            }

            orderItems.add(orderItem);
        }
        shopOrder.setItems(new java.util.HashSet<>(orderItems));
        shopOrders.add(shopOrder);
        order.setShopOrders(new java.util.HashSet<>(shopOrders));

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
        return order.getShopOrders().stream()
                .flatMap(shopOrder -> shopOrder.getItems().stream())
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
                log.info("Successfully locked {} batches for hybrid payment session: {}", lockRequests.size(),
                        sessionId);
            } else {
                log.error("Failed to lock batches for hybrid payment session: {}", sessionId);
            }

            return lockSuccess;

        } catch (Exception e) {
            log.error("Error during FEFO allocation and batch locking for hybrid payment session {}: {}", sessionId,
                    e.getMessage(), e);
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

    private Integer getAvailablePointsForShop(UUID userId, UUID shopId) {
        try {
            Integer balance = userPointsRepository.calculateCurrentBalanceByShop(userId, shopId);
            return balance != null ? balance : 0;
        } catch (Exception e) {
            log.warn("Failed to get available points for user {} and shop {}: {}", userId, shopId, e.getMessage());
            return 0;
        }
    }

    private BigDecimal getPointValueForShop(UUID shopId) {
        try {
            RewardSystemDTO system = rewardService.getActiveRewardSystem(shopId);
            return system != null && system.getPointValue() != null ? system.getPointValue() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get active reward system for shop {}: {}", shopId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void createPointsRecord(User user, UUID shopId, Long orderId, Integer pointsChange,
            BigDecimal pointsValue, String description) {
        try {
            // Compute balance after change
            Integer current = getAvailablePointsForShop(user.getId(), shopId);
            Integer balanceAfter = (current != null ? current : 0) + pointsChange;

            UserPoints record = new UserPoints();
            record.setUser(user);
            if (shopId != null) {
                shopRepository.findById(shopId).ifPresent(record::setShop);
            }
            record.setPoints(pointsChange);
            record.setPointsType(pointsChange != null && pointsChange < 0
                    ? UserPoints.PointsType.SPENT_PURCHASE
                    : UserPoints.PointsType.ADJUSTMENT);
            record.setDescription(description);
            record.setOrderId(orderId);
            record.setPointsValue(pointsValue);
            record.setBalanceAfter(balanceAfter);
            record.setCreatedAt(LocalDateTime.now());

            userPointsRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to create points record for user {} shop {}: {}", user.getId(), shopId, e.getMessage());
            throw new RuntimeException("Failed to record points transaction: " + e.getMessage(), e);
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
            throw new IllegalArgumentException(
                    "Sorry, we don't deliver to " + country + " as we don't have any warehouses there.");
        }

    }

    /**
     * Record hybrid payment in money flow system (only the financial part, not
     * points)
     */
    private void recordHybridPaymentInMoneyFlow(Order order, OrderTransaction transaction) {
        try {
            BigDecimal pointsValue = transaction.getPointsValue() != null ? transaction.getPointsValue()
                    : BigDecimal.ZERO;
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
            log.error("Failed to record money flow for hybrid payment order {}: {}", order.getOrderId(), e.getMessage(),
                    e);
            // Don't throw exception - payment already succeeded, this is just tracking
        }
    }

    @Override
    public com.ecommerce.dto.PointsEligibilityResponse checkPointsEligibility(
            com.ecommerce.dto.PointsEligibilityRequest request) {

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return com.ecommerce.dto.PointsEligibilityResponse.builder()
                    .shopEligibilities(java.util.Collections.emptyList())
                    .build();
        }

        // Group items by Shop
        Map<UUID, List<CartItemDTO>> shopItemsMap = new java.util.HashMap<>();
        Map<UUID, String> shopNamesMap = new java.util.HashMap<>();

        for (CartItemDTO item : request.getItems()) {
            Shop shop = null;
            if (item.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
                if (variant != null && variant.getProduct() != null) {
                    shop = variant.getProduct().getShop();
                }
            } else if (item.getProductId() != null) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    shop = product.getShop();
                }
            }

            if (shop != null) {
                shopItemsMap.computeIfAbsent(shop.getShopId(), k -> new ArrayList<>()).add(item);
                shopNamesMap.putIfAbsent(shop.getShopId(), shop.getName());
            }
        }

        List<com.ecommerce.dto.ShopPointsEligibilityDTO> summaries = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItemDTO>> entry : shopItemsMap.entrySet()) {
            UUID shopId = entry.getKey();
            List<CartItemDTO> shopItems = entry.getValue();
            String shopName = shopNamesMap.get(shopId);

            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalProductCount = 0;

            for (CartItemDTO item : shopItems) {
                BigDecimal itemPrice = BigDecimal.ZERO;
                if (item.getVariantId() != null) {
                    ProductVariant v = productVariantRepository.findById(item.getVariantId()).orElse(null);
                    if (v != null)
                        itemPrice = calculateDiscountedPrice(v);
                } else {
                    Product p = productRepository.findById(item.getProductId()).orElse(null);
                    if (p != null)
                        itemPrice = calculateDiscountedPrice(p);
                }

                totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                totalProductCount += item.getQuantity();
            }

            // Get Reward System
            RewardSystem rewardSystem = rewardSystemRepository.findByShopShopIdAndIsActiveTrue(shopId).orElse(null);

            boolean isRewardingEnabled = false;
            Integer potentialPoints = 0;
            BigDecimal pointValue = BigDecimal.ZERO;

            if (rewardSystem != null && Boolean.TRUE.equals(rewardSystem.getIsSystemEnabled())) {
                isRewardingEnabled = true;
                pointValue = rewardSystem.getPointValue();
                potentialPoints = rewardSystem.calculatePurchasePoints(totalProductCount, totalAmount);
            }

            // Get User Balance
            Integer currentPoints = 0;
            try {
                Integer balance = userPointsRepository.calculateCurrentBalanceByShop(request.getUserId(), shopId);
                if (balance != null)
                    currentPoints = balance;
            } catch (Exception e) {
                log.warn("Error getting points balance for checking eligibility: {}", e.getMessage());
            }

            BigDecimal currentPointsValue = pointValue.multiply(BigDecimal.valueOf(currentPoints));

            boolean canPay = isRewardingEnabled && currentPoints > 0;
            BigDecimal maxPayable = currentPointsValue.min(totalAmount);

            String message = "";
            if (!isRewardingEnabled) {
                message = "Rewarding system not active for this shop.";
            } else if (currentPoints <= 0) {
                message = "No points available.";
            } else {
                message = "Available points applied.";
            }

            summaries.add(com.ecommerce.dto.ShopPointsEligibilityDTO.builder()
                    .shopId(shopId)
                    .shopName(shopName)
                    .isRewardingEnabled(isRewardingEnabled)
                    .currentPointsBalance(currentPoints)
                    .currentPointsValue(currentPointsValue)
                    .potentialEarnedPoints(potentialPoints)
                    .totalAmount(totalAmount)
                    .canPayWithPoints(canPay)
                    .maxPointsPayableAmount(maxPayable)
                    .message(message)
                    .build());
        }

        return com.ecommerce.dto.PointsEligibilityResponse.builder()
                .shopEligibilities(summaries)
                .build();
    }
}
