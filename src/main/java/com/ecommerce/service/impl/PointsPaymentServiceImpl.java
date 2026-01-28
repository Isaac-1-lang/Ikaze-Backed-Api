package com.ecommerce.service.impl;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ShopOrderRepository shopOrderRepository;
    private final CheckoutService checkoutService;

    @Override
    public PointsPaymentPreviewDTO previewPointsPayment(PointsPaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateDeliveryCountry(request.getShippingAddress().getCountry());

        // Use CheckoutService for consistent total calculations
        PaymentSummaryDTO summary = checkoutService.calculatePaymentSummary(
                request.getShippingAddress(), request.getItems(), request.getUserId());

        BigDecimal totalOrderAmount = summary.getTotalAmount();
        BigDecimal totalPointsValueUsed = BigDecimal.ZERO;
        Integer totalAvailablePoints = 0;

        for (PaymentSummaryDTO.ShopSummary shopSummary : summary.getShopSummaries()) {
            UUID shopId = UUID.fromString(shopSummary.getShopId());
            BigDecimal shopTotal = shopSummary.getTotalAmount();

            Integer shopPoints = getAvailablePointsForShop(request.getUserId(), shopId);
            BigDecimal shopPointValue = getPointValueForShop(shopId);
            BigDecimal shopPointsPotentialValue = shopPointValue.multiply(BigDecimal.valueOf(shopPoints));

            // Use points up to shop order total
            BigDecimal usablePointsValue = shopPointsPotentialValue.min(shopTotal);
            totalPointsValueUsed = totalPointsValueUsed.add(usablePointsValue);
            totalAvailablePoints += shopPoints;
        }

        BigDecimal remainingToPay = totalOrderAmount.subtract(totalPointsValueUsed).max(BigDecimal.ZERO);
        boolean canPayWithPointsOnly = remainingToPay.compareTo(BigDecimal.valueOf(0.01)) <= 0;

        // Get average point value for display (simplified)
        BigDecimal avgPointValue = totalAvailablePoints > 0
                ? totalPointsValueUsed.divide(BigDecimal.valueOf(totalAvailablePoints), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("Points payment preview - Total: {}, Points Value: {}, Remaining: {}",
                totalOrderAmount, totalPointsValueUsed, remainingToPay);

        return new PointsPaymentPreviewDTO(
                totalOrderAmount,
                totalAvailablePoints,
                totalPointsValueUsed,
                remainingToPay,
                canPayWithPointsOnly,
                avgPointValue);
    }

    @Override
    // @Transactional
    public PointsPaymentResult processPointsPayment(PointsPaymentRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            validateDeliveryCountry(request.getShippingAddress().getCountry());
            stockValidationService.validateCartItems(request.getItems());

            // Group items by shop
            Map<UUID, List<CartItemDTO>> itemsByShop = groupItemsByShop(request.getItems());

            // Calculate shop-by-shop points usage
            List<ShopPointsCalculation> shopCalculations = calculateShopPointsUsage(
                    user.getId(), itemsByShop, request.getSelectedShopsForPoints(), request.getShippingAddress());

            BigDecimal totalOrderAmount = shopCalculations.stream()
                    .map(c -> c.shopTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPointsValue = shopCalculations.stream()
                    .map(c -> c.pointsValueToUse)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal remainingToPay = totalOrderAmount.subtract(totalPointsValue).max(BigDecimal.ZERO);
            boolean isFullPointsPayment = remainingToPay.compareTo(BigDecimal.valueOf(0.01)) <= 0;

            if (isFullPointsPayment) {
                return processFullPointsPaymentMultiVendor(user, request, shopCalculations, totalOrderAmount);
            } else {
                return processHybridPaymentMultiVendor(user, request, shopCalculations, totalOrderAmount,
                        remainingToPay);
            }
        } catch (Exception e) {
            log.error("Error processing points payment: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    private PointsPaymentResult processFullPointsPaymentMultiVendor(User user, PointsPaymentRequest request,
            List<ShopPointsCalculation> shopCalculations, BigDecimal totalOrderAmount) throws Exception {

        log.info("Starting full points payment for user: {} across {} shops", user.getId(), shopCalculations.size());

        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = warehouseAllocator
                .allocateStockWithFEFO(request.getItems(), request.getShippingAddress());
        log.info("Stock allocation completed successfully");

        Order order = createMultiVendorOrder(request, user, shopCalculations, allocations, totalOrderAmount, true);
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}", savedOrder.getOrderId());

        // Commit FEFO allocations
        for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : allocations.entrySet()) {
            fefoService.commitAllocation(entry.getValue());
            createOrderItemBatches(savedOrder, entry.getKey(), entry.getValue());
        }
        log.info("All batch allocations committed successfully");

        // Deduct points shop by shop and update ShopOrderTransaction
        List<PointsPaymentResult.ShopPointsDeduction> deductions = new ArrayList<>();
        int totalPointsUsed = 0;
        BigDecimal totalPointsValue = BigDecimal.ZERO;

        for (ShopPointsCalculation calc : shopCalculations) {
            if (calc.pointsToUse > 0) {
                createPointsRecord(user, calc.shopId, savedOrder.getOrderId(), -calc.pointsToUse,
                        calc.pointsValueToUse,
                        "Points used for order #" + savedOrder.getOrderCode() + " at " + calc.shopName);

                totalPointsUsed += calc.pointsToUse;
                totalPointsValue = totalPointsValue.add(calc.pointsValueToUse);

                // Update the ShopOrderTransaction with points used for this shop
                updateShopOrderTransactionPoints(savedOrder, calc.shopId, calc.pointsToUse, calc.pointsValueToUse);

                deductions.add(PointsPaymentResult.ShopPointsDeduction.builder()
                        .shopId(calc.shopId)
                        .shopName(calc.shopName)
                        .pointsUsed(calc.pointsToUse)
                        .pointsValue(calc.pointsValueToUse)
                        .shopOrderAmount(calc.shopTotal)
                        .remainingForShop(BigDecimal.ZERO)
                        .build());
            }
        }
        log.info("Deducted {} total points from {} shops", totalPointsUsed, deductions.size());

        // Update transaction
        OrderTransaction transaction = savedOrder.getOrderTransaction();
        transaction.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
        transaction.setPointsUsed(totalPointsUsed);
        transaction.setPointsValue(totalPointsValue);
        transaction.setPaymentDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Update shop orders to PROCESSING
        for (ShopOrder shopOrder : savedOrder.getShopOrders()) {
            shopOrder.setStatus(ShopOrder.ShopOrderStatus.PROCESSING);
        }
        orderRepository.save(savedOrder);

        // Send confirmation email
        try {
            orderEmailService.sendOrderConfirmationEmail(savedOrder);
            log.info("Order confirmation email sent for points payment order: {}", savedOrder.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email: {}", e.getMessage());
        }

        return PointsPaymentResult.builder()
                .success(true)
                .message("Payment completed successfully with points!")
                .orderId(savedOrder.getOrderId())
                .orderNumber(savedOrder.getOrderCode())
                .pointsUsed(totalPointsUsed)
                .pointsValue(totalPointsValue)
                .remainingAmount(BigDecimal.ZERO)
                .stripeSessionId(null)
                .hybridPayment(false)
                .shopPointsDeductions(deductions)
                .build();
    }

    private PointsPaymentResult processHybridPaymentMultiVendor(User user, PointsPaymentRequest request,
            List<ShopPointsCalculation> shopCalculations, BigDecimal totalOrderAmount, BigDecimal remainingToPay)
            throws Exception {

        log.info("Starting hybrid payment for user: {} - Total: {}, Remaining: {}",
                user.getId(), totalOrderAmount, remainingToPay);

        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = warehouseAllocator
                .allocateStockWithFEFO(request.getItems(), request.getShippingAddress());
        log.info("Stock allocation completed successfully");

        Order order = createMultiVendorOrder(request, user, shopCalculations, allocations, totalOrderAmount, false);
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}", savedOrder.getOrderId());

        // Create order item batches
        for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : allocations.entrySet()) {
            createOrderItemBatches(savedOrder, entry.getKey(), entry.getValue());
        }

        // Lock stock
        String tempSessionId = "temp_hybrid_" + savedOrder.getOrderId();
        if (!lockStockWithFEFOAllocation(tempSessionId, request.getItems(), request.getShippingAddress())) {
            log.error("Failed to lock stock for hybrid payment: {}", savedOrder.getOrderId());
            orderRepository.delete(savedOrder);
            throw new IllegalStateException("Unable to secure stock for your order. Please try again.");
        }
        log.info("Stock locked successfully for session: {}", tempSessionId);

        // Deduct points shop by shop and update ShopOrderTransaction
        List<PointsPaymentResult.ShopPointsDeduction> deductions = new ArrayList<>();
        int totalPointsUsed = 0;
        BigDecimal totalPointsValue = BigDecimal.ZERO;

        for (ShopPointsCalculation calc : shopCalculations) {
            if (calc.pointsToUse > 0) {
                createPointsRecord(user, calc.shopId, savedOrder.getOrderId(), -calc.pointsToUse,
                        calc.pointsValueToUse,
                        "Partial points for order #" + savedOrder.getOrderCode() + " at " + calc.shopName);

                totalPointsUsed += calc.pointsToUse;
                totalPointsValue = totalPointsValue.add(calc.pointsValueToUse);

                // Update the ShopOrderTransaction with points used for this shop
                updateShopOrderTransactionPoints(savedOrder, calc.shopId, calc.pointsToUse, calc.pointsValueToUse);

                BigDecimal shopRemaining = calc.shopTotal.subtract(calc.pointsValueToUse).max(BigDecimal.ZERO);
                deductions.add(PointsPaymentResult.ShopPointsDeduction.builder()
                        .shopId(calc.shopId)
                        .shopName(calc.shopName)
                        .pointsUsed(calc.pointsToUse)
                        .pointsValue(calc.pointsValueToUse)
                        .shopOrderAmount(calc.shopTotal)
                        .remainingForShop(shopRemaining)
                        .build());
            }
        }
        log.info("Deducted {} total points for hybrid payment", totalPointsUsed);

        // Create Stripe session for remaining amount
        String stripeSessionId = stripeService.createCheckoutSessionForHybridPayment(
                savedOrder, "usd", "web", remainingToPay);
        log.info("Stripe session created: {}", stripeSessionId);

        // Update transaction
        OrderTransaction transaction = transactionRepository
                .findById(savedOrder.getOrderTransaction().getOrderTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStripeSessionId() != null && !transaction.getStripeSessionId().equals(tempSessionId)) {
            transferBatchLocks(tempSessionId, transaction.getStripeSessionId());
        }

        transaction.setPointsUsed(totalPointsUsed);
        transaction.setPointsValue(totalPointsValue);
        transactionRepository.save(transaction);

        return PointsPaymentResult.builder()
                .success(true)
                .message("Hybrid payment initiated. Complete payment via Stripe.")
                .orderId(savedOrder.getOrderId())
                .orderNumber(savedOrder.getOrderCode())
                .pointsUsed(totalPointsUsed)
                .pointsValue(totalPointsValue)
                .remainingAmount(remainingToPay)
                .stripeSessionId(stripeSessionId)
                .hybridPayment(true)
                .shopPointsDeductions(deductions)
                .build();
    }

    @Override
    @Transactional
    public PointsPaymentResult completeHybridPayment(UUID userId, Long orderId, String stripeSessionId) {
        try {
            OrderTransaction transaction = transactionRepository.findByStripeSessionId(stripeSessionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Check if already completed (idempotency)
            if (transaction.getStatus() == OrderTransaction.TransactionStatus.COMPLETED) {
                log.info("Hybrid payment already completed for session: {}. Returning cached result.", stripeSessionId);
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"));
                return buildCompletedHybridResult(order, transaction);
            }

            // Confirm batch locks
            enhancedStockLockService.confirmBatchLocks(stripeSessionId);
            log.info("Confirmed batch locks for hybrid payment session: {}", stripeSessionId);

            transaction.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
            transaction.setPaymentDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Update shop orders to PROCESSING
            if (order.getShopOrders() != null) {
                for (ShopOrder shopOrder : order.getShopOrders()) {
                    shopOrder.setStatus(ShopOrder.ShopOrderStatus.PROCESSING);
                }
                orderRepository.save(order);
            }

            // Record money flow
            recordHybridPaymentInMoneyFlow(order, transaction);

            // Send confirmation email
            try {
                orderEmailService.sendOrderConfirmationEmail(order);
                log.info("Order confirmation email sent for hybrid payment: {}", order.getOrderId());
            } catch (Exception e) {
                log.error("Failed to send confirmation email: {}", e.getMessage());
            }

            // Award reward points for this purchase
            if (order.getUser() != null) {
                try {
                    rewardService.checkRewardableOnOrderAndReward(order);
                } catch (Exception e) {
                    log.error("Failed to award reward points: {}", e.getMessage());
                }
            }

            return buildCompletedHybridResult(order, transaction);

        } catch (Exception e) {
            log.error("Error completing hybrid payment: {}", e.getMessage(), e);
            return PointsPaymentResult.builder()
                    .success(false)
                    .message("Failed to complete payment: " + e.getMessage())
                    .build();
        }
    }

    private PointsPaymentResult buildCompletedHybridResult(Order order, OrderTransaction transaction) {
        return PointsPaymentResult.builder()
                .success(true)
                .message("Hybrid payment completed successfully!")
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderCode())
                .pointsUsed(transaction.getPointsUsed())
                .pointsValue(transaction.getPointsValue())
                .remainingAmount(BigDecimal.ZERO)
                .stripeSessionId(null)
                .hybridPayment(false)
                .build();
    }

    // Helper class for shop-level calculations
    private static class ShopPointsCalculation {
        UUID shopId;
        String shopName;
        BigDecimal shopTotal;
        Integer availablePoints;
        BigDecimal pointValue;
        Integer pointsToUse;
        BigDecimal pointsValueToUse;
        List<CartItemDTO> items;
    }

    private List<ShopPointsCalculation> calculateShopPointsUsage(UUID userId, Map<UUID, List<CartItemDTO>> itemsByShop,
            List<PointsPaymentRequest.ShopPointsSelection> selectedShops, AddressDto shippingAddress) {

        // Build a map of selected shops for quick lookup
        Map<UUID, Integer> selectedPointsByShop = new HashMap<>();
        if (selectedShops != null) {
            for (PointsPaymentRequest.ShopPointsSelection selection : selectedShops) {
                selectedPointsByShop.put(selection.getShopId(), selection.getPointsToUse());
            }
        }

        // Use CheckoutService for accurate per-shop totals
        PaymentSummaryDTO summary = checkoutService.calculatePaymentSummary(shippingAddress,
                itemsByShop.values().stream().flatMap(List::stream).collect(Collectors.toList()), userId);

        List<ShopPointsCalculation> calculations = new ArrayList<>();

        for (PaymentSummaryDTO.ShopSummary shopSummary : summary.getShopSummaries()) {
            UUID shopId = UUID.fromString(shopSummary.getShopId());

            ShopPointsCalculation calc = new ShopPointsCalculation();
            calc.shopId = shopId;
            // Filter items for this shop
            calc.items = itemsByShop.get(shopId);
            calc.shopName = shopSummary.getShopName();
            calc.shopTotal = shopSummary.getTotalAmount();

            calc.availablePoints = getAvailablePointsForShop(userId, shopId);
            calc.pointValue = getPointValueForShop(shopId);

            // Determine how many points to use
            if (selectedPointsByShop.containsKey(shopId)) {
                // Use selected amount (up to available)
                calc.pointsToUse = Math.min(selectedPointsByShop.get(shopId), calc.availablePoints);
            } else if (selectedShops == null || selectedShops.isEmpty()) {
                // Legacy behavior: use all available points
                calc.pointsToUse = calc.availablePoints;
            } else {
                // Shop not selected - no points
                calc.pointsToUse = 0;
            }

            // Cap points value at shop total
            BigDecimal maxPointsValue = calc.pointValue.multiply(BigDecimal.valueOf(calc.pointsToUse));
            if (maxPointsValue.compareTo(calc.shopTotal) > 0) {
                // Reduce points to only what's needed
                calc.pointsToUse = calculatePointsNeeded(calc.shopTotal, calc.pointValue);
                calc.pointsValueToUse = calc.pointValue.multiply(BigDecimal.valueOf(calc.pointsToUse));
            } else {
                calc.pointsValueToUse = maxPointsValue;
            }

            calculations.add(calc);
        }

        return calculations;
    }

    private Map<UUID, List<CartItemDTO>> groupItemsByShop(List<CartItemDTO> items) {
        Map<UUID, List<CartItemDTO>> itemsByShop = new HashMap<>();

        for (CartItemDTO item : items) {
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
                itemsByShop.computeIfAbsent(shop.getShopId(), k -> new ArrayList<>()).add(item);
            }
        }

        return itemsByShop;
    }

    private BigDecimal calculateShopSubtotal(List<CartItemDTO> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemDTO item : items) {
            BigDecimal itemPrice = BigDecimal.ZERO;
            if (item.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
                if (variant != null)
                    itemPrice = calculateDiscountedPrice(variant);
            } else if (item.getProductId() != null) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null)
                    itemPrice = calculateDiscountedPrice(product);
            }
            subtotal = subtotal.add(itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal;
    }

    private BigDecimal calculateShopShipping(AddressDto address, List<CartItemDTO> items, BigDecimal subtotal) {
        try {
            return shippingCostService.calculateOrderShippingCost(address, items, subtotal);
        } catch (Exception e) {
            log.warn("Error calculating shipping: {}", e.getMessage());
            return BigDecimal.valueOf(5.00); // Default fallback
        }
    }

    private Order createMultiVendorOrder(PointsPaymentRequest request, User user,
            List<ShopPointsCalculation> shopCalculations,
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations,
            BigDecimal totalAmount, boolean isFullPointsPayment) {

        Order order = new Order();
        order.setUser(user);
        order.setOrderCode(generateOrderNumber());

        // Order Info
        BigDecimal subtotal = shopCalculations.stream()
                .flatMap(c -> c.items.stream())
                .map(item -> {
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingCost = totalAmount.subtract(subtotal).max(BigDecimal.ZERO);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setSubtotal(subtotal);
        orderInfo.setShippingCost(shippingCost);
        orderInfo.setTotalAmount(totalAmount);
        orderInfo.setOrder(order);
        order.setOrderInfo(orderInfo);

        // Address
        OrderAddress orderAddress = new OrderAddress();
        orderAddress.setStreet(request.getShippingAddress().getStreetAddress());
        orderAddress.setRegions(request.getShippingAddress().getCity() + "," + request.getShippingAddress().getState());
        orderAddress.setCountry(request.getShippingAddress().getCountry());
        orderAddress.setLatitude(request.getShippingAddress().getLatitude());
        orderAddress.setLongitude(request.getShippingAddress().getLongitude());
        orderAddress.setOrder(order);
        order.setOrderAddress(orderAddress);

        // Customer Info
        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(user.getFirstName());
        customerInfo.setLastName(user.getLastName());
        customerInfo.setEmail(user.getUserEmail());
        customerInfo.setPhoneNumber(user.getPhoneNumber());
        customerInfo.setOrder(order);
        order.setOrderCustomerInfo(customerInfo);

        // Transaction
        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(totalAmount);
        tx.setPaymentMethod(
                isFullPointsPayment ? OrderTransaction.PaymentMethod.POINTS : OrderTransaction.PaymentMethod.HYBRID);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        // Create ShopOrders
        Set<ShopOrder> shopOrders = new HashSet<>();
        for (ShopPointsCalculation calc : shopCalculations) {
            ShopOrder shopOrder = new ShopOrder();
            shopOrder.setOrder(order);
            shopOrder.setStatus(ShopOrder.ShopOrderStatus.PENDING);

            Shop shop = shopRepository.findById(calc.shopId).orElse(null);
            shopOrder.setShop(shop);

            BigDecimal shopSubtotal = calculateShopSubtotal(calc.items);
            BigDecimal shopShipping = calc.shopTotal.subtract(shopSubtotal);
            shopOrder.setSubtotal(shopSubtotal);
            shopOrder.setShippingCost(shopShipping);
            shopOrder.setTotalAmount(calc.shopTotal);

            // Create order items
            Set<OrderItem> orderItems = new HashSet<>();
            for (CartItemDTO cartItem : calc.items) {
                OrderItem orderItem = new OrderItem();
                orderItem.setShopOrder(shopOrder);
                orderItem.setQuantity(cartItem.getQuantity());

                if (cartItem.getVariantId() != null) {
                    ProductVariant variant = productVariantRepository.findById(cartItem.getVariantId()).orElse(null);
                    if (variant != null) {
                        orderItem.setProductVariant(variant);
                        orderItem.setProduct(variant.getProduct());
                        orderItem.setPrice(calculateDiscountedPrice(variant));
                    }
                } else if (cartItem.getProductId() != null) {
                    Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
                    if (product != null) {
                        orderItem.setProduct(product);
                        orderItem.setPrice(calculateDiscountedPrice(product));
                    }
                }

                orderItems.add(orderItem);
            }
            shopOrder.setItems(orderItems);

            // Shop order transaction
            ShopOrderTransaction shopTx = new ShopOrderTransaction();
            shopTx.setShopOrder(shopOrder);
            shopTx.setGlobalTransaction(tx);
            shopTx.setAmount(calc.shopTotal);
            shopTx.setPointsUsed(calc.pointsToUse != null ? calc.pointsToUse : 0);
            shopTx.setPointsValue(calc.pointsValueToUse != null ? calc.pointsValueToUse : BigDecimal.ZERO);
            shopOrder.setShopOrderTransaction(shopTx);
            tx.getShopTransactions().add(shopTx);

            shopOrders.add(shopOrder);
        }
        order.setShopOrders(shopOrders);

        return order;
    }

    private void createOrderItemBatches(Order order, CartItemDTO cartItem,
            List<FEFOStockAllocationService.BatchAllocation> allocations) {
        OrderItem orderItem = findOrderItemByCartItem(order, cartItem);
        if (orderItem == null) {
            log.warn("Order item not found for cart item: {}", cartItem.getProductId());
            return;
        }

        for (FEFOStockAllocationService.BatchAllocation allocation : allocations) {
            OrderItemBatch orderItemBatch = new OrderItemBatch();
            orderItemBatch.setOrderItem(orderItem);
            orderItemBatch.setStockBatch(allocation.getStockBatch());
            orderItemBatch.setWarehouse(allocation.getWarehouse());
            orderItemBatch.setQuantityUsed(allocation.getQuantityAllocated());
            orderItemBatchRepository.save(orderItemBatch);
        }
    }

    private OrderItem findOrderItemByCartItem(Order order, CartItemDTO cartItem) {
        return order.getShopOrders().stream()
                .flatMap(shopOrder -> shopOrder.getItems().stream())
                .filter(item -> {
                    if (cartItem.getVariantId() != null) {
                        return item.getProductVariant() != null &&
                                item.getProductVariant().getId().equals(cartItem.getVariantId());
                    } else {
                        return item.getProduct() != null &&
                                item.getProduct().getProductId().equals(cartItem.getProductId());
                    }
                })
                .findFirst()
                .orElse(null);
    }

    private Integer calculatePointsNeeded(BigDecimal amount, BigDecimal pointValue) {
        if (pointValue.compareTo(BigDecimal.ZERO) == 0)
            return 0;
        return amount.divide(pointValue, 0, RoundingMode.CEILING).intValue();
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Integer getAvailablePointsForShop(UUID userId, UUID shopId) {
        try {
            Integer balance = userPointsRepository.calculateCurrentBalanceByShop(userId, shopId);
            return balance != null ? Math.max(0, balance) : 0;
        } catch (Exception e) {
            log.warn("Failed to get points for user {} shop {}: {}", userId, shopId, e.getMessage());
            return 0;
        }
    }

    private BigDecimal getPointValueForShop(UUID shopId) {
        try {
            RewardSystemDTO system = rewardService.getActiveRewardSystem(shopId);
            return system != null && system.getPointValue() != null ? system.getPointValue() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get reward system for shop {}: {}", shopId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void createPointsRecord(User user, UUID shopId, Long orderId, Integer pointsChange,
            BigDecimal pointsValue, String description) {
        Integer current = getAvailablePointsForShop(user.getId(), shopId);
        Integer balanceAfter = current + pointsChange;

        UserPoints record = new UserPoints();
        record.setUser(user);
        if (shopId != null) {
            shopRepository.findById(shopId).ifPresent(record::setShop);
        }
        record.setPoints(pointsChange);
        record.setPointsType(
                pointsChange < 0 ? UserPoints.PointsType.SPENT_PURCHASE : UserPoints.PointsType.ADJUSTMENT);
        record.setDescription(description);
        record.setOrderId(orderId);
        record.setPointsValue(pointsValue);
        record.setBalanceAfter(balanceAfter);
        record.setCreatedAt(LocalDateTime.now());

        userPointsRepository.save(record);
        log.info("Points record created: {} points for user {} at shop {}", pointsChange, user.getId(), shopId);
    }

    private void updateShopOrderTransactionPoints(Order order, UUID shopId, Integer pointsUsed,
            BigDecimal pointsValue) {
        for (ShopOrder shopOrder : order.getShopOrders()) {
            if (shopOrder.getShop() != null && shopOrder.getShop().getShopId().equals(shopId)) {
                ShopOrderTransaction shopTx = shopOrder.getShopOrderTransaction();
                if (shopTx != null) {
                    shopTx.setPointsUsed(pointsUsed);
                    shopTx.setPointsValue(pointsValue);
                    log.info("Updated ShopOrderTransaction for shop {} with {} points (value: {})",
                            shopId, pointsUsed, pointsValue);
                }
                break;
            }
        }
    }

    private boolean lockStockWithFEFOAllocation(String sessionId, List<CartItemDTO> items, AddressDto shippingAddress) {
        try {
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations = warehouseAllocator
                    .allocateStockWithFEFO(items, shippingAddress);

            if (fefoAllocations.isEmpty()) {
                return false;
            }

            List<EnhancedStockLockService.BatchLockRequest> lockRequests = new ArrayList<>();
            for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : fefoAllocations
                    .entrySet()) {
                for (FEFOStockAllocationService.BatchAllocation allocation : entry.getValue()) {
                    lockRequests.add(new EnhancedStockLockService.BatchLockRequest(
                            allocation.getStockBatch().getId(),
                            allocation.getQuantityAllocated(),
                            allocation.getWarehouse().getId(),
                            null, null));
                }
            }

            return enhancedStockLockService.lockStockBatches(sessionId, lockRequests);
        } catch (Exception e) {
            log.error("Error locking stock: {}", e.getMessage(), e);
            return false;
        }
    }

    private void transferBatchLocks(String fromSessionId, String toSessionId) {
        try {
            enhancedStockLockService.transferBatchLocks(fromSessionId, toSessionId);
        } catch (Exception e) {
            log.error("Failed to transfer locks: {}", e.getMessage());
        }
    }

    private void validateDeliveryCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery country is required");
        }
        if (!warehouseRepository.existsByCountryIgnoreCase(country.trim())) {
            throw new IllegalArgumentException("Sorry, we don't deliver to " + country);
        }
    }

    private void recordHybridPaymentInMoneyFlow(Order order, OrderTransaction transaction) {
        try {
            BigDecimal pointsValue = transaction.getPointsValue() != null ? transaction.getPointsValue()
                    : BigDecimal.ZERO;
            BigDecimal financialAmount = transaction.getOrderAmount().subtract(pointsValue);

            if (financialAmount.compareTo(BigDecimal.ZERO) > 0) {
                CreateMoneyFlowDTO moneyFlowDTO = new CreateMoneyFlowDTO();
                moneyFlowDTO.setDescription(String.format("Hybrid payment for Order #%s", order.getOrderCode()));
                moneyFlowDTO.setType(com.ecommerce.enums.MoneyFlowType.IN);
                moneyFlowDTO.setAmount(financialAmount);
                moneyFlowService.save(moneyFlowDTO);
            }
        } catch (Exception e) {
            log.error("Failed to record money flow: {}", e.getMessage());
        }
    }

    // Discount calculation methods (same as CheckoutService)
    private BigDecimal calculateDiscountedPrice(ProductVariant variant) {
        if (variant.getDiscount() != null && variant.getDiscount().isValid() && variant.getDiscount().isActive()) {
            if (validateDiscountValidity(variant.getDiscount())) {
                return variant.getPrice().multiply(BigDecimal.ONE.subtract(
                        variant.getDiscount().getPercentage().divide(BigDecimal.valueOf(100))));
            }
        }
        if (variant.getProduct() != null && variant.getProduct().getDiscount() != null
                && variant.getProduct().getDiscount().isValid() && variant.getProduct().getDiscount().isActive()) {
            if (validateDiscountValidity(variant.getProduct().getDiscount())) {
                return variant.getPrice().multiply(BigDecimal.ONE.subtract(
                        variant.getProduct().getDiscount().getPercentage().divide(BigDecimal.valueOf(100))));
            }
        }
        if (variant.getProduct() != null && variant.getProduct().isOnSale()
                && variant.getProduct().getSalePercentage() != null && variant.getProduct().getSalePercentage() > 0) {
            return variant.getPrice().multiply(BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(variant.getProduct().getSalePercentage()).divide(BigDecimal.valueOf(100))));
        }
        return variant.getPrice();
    }

    private BigDecimal calculateDiscountedPrice(Product product) {
        if (product.getDiscount() != null && product.getDiscount().isValid() && product.getDiscount().isActive()) {
            if (validateDiscountValidity(product.getDiscount())) {
                return product.getPrice().multiply(BigDecimal.ONE.subtract(
                        product.getDiscount().getPercentage().divide(BigDecimal.valueOf(100))));
            }
        }
        if (product.isOnSale() && product.getSalePercentage() != null && product.getSalePercentage() > 0) {
            return product.getPrice().multiply(BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(product.getSalePercentage()).divide(BigDecimal.valueOf(100))));
        }
        return product.getPrice();
    }

    private boolean validateDiscountValidity(Discount discount) {
        LocalDateTime now = LocalDateTime.now();
        if (!discount.isActive())
            return false;
        if (now.isBefore(discount.getStartDate()))
            return false;
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate()))
            return false;
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit())
            return false;
        return true;
    }

    @Override
    public PointsEligibilityResponse checkPointsEligibility(PointsEligibilityRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return PointsEligibilityResponse.builder()
                    .shopEligibilities(Collections.emptyList())
                    .build();
        }

        // Use CheckoutService to get accurate per-shop totals including shipping and
        // taxes
        PaymentSummaryDTO summary;
        try {
            // If shipping address is missing, we use a dummy one for basic calculation
            // but the frontend should provide it for accurate results
            AddressDto address = request.getShippingAddress();
            if (address == null) {
                log.warn("Shipping address missing in eligibility check, results may be inaccurate for shipping costs");
                // Fallback to a default address if needed or handle appropriately
                address = new AddressDto();
                address.setCountry("USA"); // Default
            }

            summary = checkoutService.calculatePaymentSummary(address, request.getItems(), request.getUserId());
        } catch (Exception e) {
            log.error("Error calculating payment summary for eligibility: {}", e.getMessage());
            throw new RuntimeException("Could not calculate order totals: " + e.getMessage());
        }

        List<ShopPointsEligibilityDTO> summaries = new ArrayList<>();

        for (PaymentSummaryDTO.ShopSummary shopSummary : summary.getShopSummaries()) {
            UUID shopId = UUID.fromString(shopSummary.getShopId());

            // Total amount now includes shipping and taxes from CheckoutService
            BigDecimal totalAmount = shopSummary.getTotalAmount();
            int totalProductCount = shopSummary.getProductCount();

            RewardSystem rewardSystem = rewardSystemRepository.findByShopShopIdAndIsActiveTrue(shopId).orElse(null);

            boolean isRewardingEnabled = false;
            Integer potentialPoints = 0;
            BigDecimal pointValue = BigDecimal.ZERO;

            if (rewardSystem != null && Boolean.TRUE.equals(rewardSystem.getIsSystemEnabled())) {
                isRewardingEnabled = true;
                pointValue = rewardSystem.getPointValue();
                // Potential points are also calculated in PaymentSummaryDTO, we can use those
                // too
                potentialPoints = shopSummary.getRewardPoints();
            }

            Integer currentPoints = getAvailablePointsForShop(request.getUserId(), shopId);
            BigDecimal currentPointsValue = pointValue.multiply(BigDecimal.valueOf(currentPoints));
            boolean canPay = isRewardingEnabled && currentPoints > 0;
            BigDecimal maxPayable = currentPointsValue.min(totalAmount);

            String message;
            if (!isRewardingEnabled) {
                message = "Reward system not active for this shop.";
            } else if (currentPoints <= 0) {
                message = "No points available in this shop.";
            } else {
                message = "Include points from this shop";
            }

            summaries.add(ShopPointsEligibilityDTO.builder()
                    .shopId(shopId)
                    .shopName(shopSummary.getShopName())
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

        return PointsEligibilityResponse.builder()
                .shopEligibilities(summaries)
                .build();
    }
}
