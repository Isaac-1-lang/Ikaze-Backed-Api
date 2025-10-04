package com.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CheckoutRequest;
import com.ecommerce.dto.CheckoutVerificationResult;
import com.ecommerce.dto.GuestCheckoutRequest;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.dto.OrderAddressDTO;
import com.ecommerce.dto.OrderCustomerInfoDTO;
import com.ecommerce.dto.OrderItemDTO;
import com.ecommerce.dto.OrderTransactionDTO;
import com.ecommerce.dto.SimpleProductDTO;
import com.ecommerce.entity.Discount;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderAddress;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderInfo;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderItemBatch;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.StockBatch;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderItemBatchRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.StockBatchRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.DiscountRepository;
import com.ecommerce.service.CartService;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DiscountRepository discountRepository;
    private final OrderRepository orderRepository;
    private final StripeService stripeService;
    private final StockLockService stockLockService;
    private final MultiWarehouseStockAllocator multiWarehouseStockAllocator;
    private final ShippingCostService shippingCostService;
    private final RewardService rewardService;
    private final ProductAvailabilityService productAvailabilityService;
    private final EnhancedMultiWarehouseAllocator enhancedWarehouseAllocator;
    private final FEFOStockAllocationService fefoService;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final StockBatchRepository stockBatchRepository;
    private final OrderEmailService orderEmailService;
    private final EnhancedStockLockService enhancedStockLockService;
    private final CartService cartService;

    public String createCheckoutSession(CheckoutRequest req) throws Exception {
        log.info("Creating checkout session for authenticated user");

        UUID userId;
        try {
            userId = getCurrentUserId();
            log.info("Retrieved user ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to get current user ID: {}", e.getMessage());
            throw new IllegalStateException("Authentication required. Please log in to create a checkout session. " +
                    "For guest checkout, use the guest checkout endpoint instead.", e);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        validateCartItems(req.getItems());

        // Step 1: Allocate stock using FEFO across warehouses
        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations = enhancedWarehouseAllocator
                .allocateStockWithFEFO(req.getItems(), req.getShippingAddress());

        // Step 2: Convert FEFO allocations to stock allocations for locking
        Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> stockAllocations = convertFEFOToStockAllocations(
                fefoAllocations);

        Order order = new Order();
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setUser(user);

        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(user.getFirstName());
        customerInfo.setLastName(user.getLastName());
        customerInfo.setEmail(user.getUserEmail());
        customerInfo.setPhoneNumber(user.getPhoneNumber());
        if (req.getShippingAddress() != null) {
            customerInfo.setStreetAddress(req.getShippingAddress().getStreetAddress());
            customerInfo.setCity(req.getShippingAddress().getCity());
            customerInfo.setState(req.getShippingAddress().getState());
            customerInfo.setCountry(req.getShippingAddress().getCountry());
        }
        order.setOrderCustomerInfo(customerInfo);
        customerInfo.setOrder(order);

        com.ecommerce.dto.PaymentSummaryDTO paymentSummary = calculatePaymentSummary(req.getShippingAddress(),
                req.getItems(), user.getId());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO ci : req.getItems()) {
            OrderItem oi = new OrderItem();
            BigDecimal itemPrice;

            if (ci.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(ci.getVariantId())
                        .orElseThrow(
                                () -> new EntityNotFoundException("Variant not found with ID: " + ci.getVariantId()));

                oi.setProductVariant(variant);
                itemPrice = calculateDiscountedPrice(variant);
            } else if (ci.getProductId() != null) {
                Product product = productRepository.findById(ci.getProductId())
                        .orElseThrow(
                                () -> new EntityNotFoundException("Product not found with ID: " + ci.getProductId()));

                oi.setProduct(product);
                itemPrice = calculateDiscountedPrice(product);
            } else {
                throw new IllegalArgumentException("Cart item must have either productId or variantId");
            }

            oi.setQuantity(ci.getQuantity());
            oi.setPrice(itemPrice);
            oi.setOrder(order);
            order.getOrderItems().add(oi);
            total = total.add(itemPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrder(order);
        orderInfo.setTotalAmount(paymentSummary.getTotalAmount());
        orderInfo.setTaxAmount(paymentSummary.getTaxAmount());
        orderInfo.setShippingCost(paymentSummary.getShippingCost());
        orderInfo.setDiscountAmount(paymentSummary.getDiscountAmount());
        order.setOrderInfo(orderInfo);

        if (req.getShippingAddress() != null) {
            OrderAddress orderAddress = new OrderAddress();
            orderAddress.setOrder(order);
            orderAddress.setStreet(req.getShippingAddress().getStreetAddress());
            orderAddress.setCountry(req.getShippingAddress().getCountry());
            orderAddress.setRegions(req.getShippingAddress().getCity() + "," + req.getShippingAddress().getState());
            orderAddress.setLatitude(req.getShippingAddress().getLatitude());
            orderAddress.setLongitude(req.getShippingAddress().getLongitude());
            orderAddress.setRoadName(req.getShippingAddress().getStreetAddress());
            order.setOrderAddress(orderAddress);
        }

        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(paymentSummary.getTotalAmount());
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        Order saved = orderRepository.save(order);
        log.info("Order created with ID: {}", saved.getOrderId());

        // Step 3: Create OrderItemBatch records for tracking (but don't commit
        // allocation yet)
        for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : fefoAllocations
                .entrySet()) {
            createOrderItemBatches(saved, entry.getKey(), entry.getValue());
        }

        // Step 4: Lock stock at batch level using session ID
        String sessionId = saved.getOrderTransaction().getStripeSessionId();
        if (sessionId == null) {
            // Generate temporary session ID for locking
            sessionId = "temp_" + saved.getOrderId().toString();
        }

        if (!lockStockWithFEFOAllocation(sessionId, req.getItems(), req.getShippingAddress())) {
            log.error("Failed to lock stock for checkout: {}", saved.getOrderId());
            orderRepository.delete(saved);
            throw new IllegalStateException("Unable to secure stock for your order. Please try again.");
        }

        String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, req.getCurrency(), req.getPlatform());

        tx = saved.getOrderTransaction();
        if (tx.getStripeSessionId() != null && !tx.getStripeSessionId().equals(sessionId)) {
            transferBatchLocks(sessionId, tx.getStripeSessionId());
        }

        log.info("Stripe session created successfully with stock locked");

        return sessionUrl;
    }

    public String createGuestCheckoutSession(GuestCheckoutRequest req) throws Exception {
        log.info("Creating guest checkout session");

        validateCartItems(req.getItems());

        // Step 1: Allocate stock using FEFO across warehouses
        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations = enhancedWarehouseAllocator
                .allocateStockWithFEFO(req.getItems(), req.getAddress());

        // Step 2: Convert FEFO allocations to stock allocations for locking
        Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> stockAllocations = convertFEFOToStockAllocations(
                fefoAllocations);

        Order order = new Order();
        order.setOrderStatus(Order.OrderStatus.PENDING);
        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(req.getGuestName());
        customerInfo.setLastName(req.getGuestLastName());
        customerInfo.setEmail(req.getGuestEmail());
        customerInfo.setPhoneNumber(req.getGuestPhone());
        if (req.getAddress() != null) {
            customerInfo.setStreetAddress(req.getAddress().getStreetAddress());
            customerInfo.setCity(req.getAddress().getCity());
            customerInfo.setState(req.getAddress().getState());
            customerInfo.setCountry(req.getAddress().getCountry());
        }
        order.setOrderCustomerInfo(customerInfo);
        customerInfo.setOrder(order);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO ci : req.getItems()) {
            log.info("Processing guest cart item: productId={}, variantId={}, quantity={}",
                    ci.getProductId(), ci.getVariantId(), ci.getQuantity());

            OrderItem oi = new OrderItem();
            BigDecimal itemPrice;

            if (ci.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(ci.getVariantId())
                        .orElseThrow(
                                () -> new EntityNotFoundException("Variant not found with ID: " + ci.getVariantId()));

                oi.setProductVariant(variant);
                itemPrice = calculateDiscountedPrice(variant);
                log.info("Set productVariant for guest OrderItem: {}", oi.getDebugInfo());
            } else if (ci.getProductId() != null) {
                Product product = productRepository.findById(ci.getProductId())
                        .orElseThrow(
                                () -> new EntityNotFoundException("Product not found with ID: " + ci.getProductId()));

                oi.setProduct(product);
                itemPrice = calculateDiscountedPrice(product);
                log.info("Set product for guest OrderItem: {}", oi.getDebugInfo());
            } else {
                throw new IllegalArgumentException("Cart item must have either productId or variantId");
            }

            oi.setQuantity(ci.getQuantity());
            oi.setPrice(itemPrice);
            oi.setOrder(order);
            order.getOrderItems().add(oi);
            total = total.add(itemPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        com.ecommerce.dto.PaymentSummaryDTO paymentSummary = calculatePaymentSummary(req.getAddress(), req.getItems(),
                null);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrder(order);
        orderInfo.setTotalAmount(paymentSummary.getTotalAmount());
        orderInfo.setTaxAmount(paymentSummary.getTaxAmount());
        orderInfo.setShippingCost(paymentSummary.getShippingCost());
        orderInfo.setDiscountAmount(paymentSummary.getDiscountAmount());
        order.setOrderInfo(orderInfo);

        if (req.getAddress() != null) {
            OrderAddress orderAddress = new OrderAddress();
            orderAddress.setOrder(order);
            orderAddress.setStreet(req.getAddress().getStreetAddress());
            orderAddress.setCountry(req.getAddress().getCountry());
            orderAddress.setRegions(req.getAddress().getCity() + "," + req.getAddress().getState());
            orderAddress.setLatitude(req.getAddress().getLatitude());
            orderAddress.setLongitude(req.getAddress().getLongitude());
            orderAddress.setRoadName(req.getAddress().getStreetAddress());
            order.setOrderAddress(orderAddress);
        }

        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(paymentSummary.getTotalAmount());
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        Order saved = orderRepository.save(order);
        log.info("Guest order created with ID: {}", saved.getOrderId());

        // Step 3: Create OrderItemBatch records for tracking (but don't commit
        // allocation yet)
        for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : fefoAllocations
                .entrySet()) {
            createOrderItemBatches(saved, entry.getKey(), entry.getValue());
        }

        // Step 4: Lock stock at batch level using session ID
        String sessionId = saved.getOrderTransaction().getStripeSessionId();
        if (sessionId == null) {
            // Generate temporary session ID for locking
            sessionId = "temp_guest_" + saved.getOrderId().toString();
        }

        boolean stockLocked = lockStockWithFEFOAllocation(sessionId, req.getItems(), req.getAddress());
        if (!stockLocked) {
            log.error("Failed to lock stock for guest order: {}", saved.getOrderId());
            orderRepository.delete(saved);
            throw new IllegalStateException("Unable to secure stock for your order. Please try again.");
        }

        log.info("Successfully locked stock for guest order: {} with session: {}", saved.getOrderId(), sessionId);

        String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, "usd", req.getPlatform());

        tx = saved.getOrderTransaction();
        if (tx.getStripeSessionId() != null && !tx.getStripeSessionId().equals(sessionId)) {
            transferBatchLocks(sessionId, tx.getStripeSessionId());
        }

        log.info("Guest Stripe session created successfully with stock locked");

        return sessionUrl;
    }

    @Transactional
    public CheckoutVerificationResult verifyCheckoutSession(String sessionId) throws Exception {
        OrderTransaction tx = transactionRepository.findByStripeSessionIdWithOrder(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("No matching payment record"));

        Session session = stripeService.retrieveSession(sessionId);

        if (session == null) {
            throw new EntityNotFoundException("Session not found on Stripe");
        }

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            Order order = tx.getOrder();

            refundPointsForFailedPayment(order);
            orderRepository.delete(order);
            enhancedStockLockService.unlockAllBatches(sessionId);
            stockLockService.releaseStock(sessionId);
            log.info("Payment failed, order deleted and points refunded: {}", order.getOrderId());
            throw new IllegalStateException("Payment not completed or session expired");
        }

        PaymentIntent intent = (PaymentIntent) session.getPaymentIntentObject();
        String receiptUrl = session.getPaymentStatus().equals("paid") ? session.getUrl() : null;

        tx.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
        tx.setPaymentDate(LocalDateTime.now());
        if (intent != null) {
            tx.setStripePaymentIntentId(intent.getId());
        }
        tx.setReceiptUrl(receiptUrl);
        transactionRepository.save(tx);
        log.info("Transaction updated to completed status");
        enhancedStockLockService.confirmBatchLocks(sessionId);

        Order order = tx.getOrder();
        order.setOrderStatus(Order.OrderStatus.PROCESSING);
        orderRepository.save(order);

        log.info("Payment verification completed successfully for order: {}", order.getOrderId());
        stockLockService.confirmStock(sessionId);

        String tempSessionId = "temp_" + order.getOrderId().toString();
        String tempGuestSessionId = "temp_guest_" + order.getOrderId().toString();

        stockLockService.confirmBatchLocks(tempSessionId);
        stockLockService.confirmStock(tempSessionId);
        stockLockService.confirmBatchLocks(tempGuestSessionId);
        stockLockService.confirmStock(tempGuestSessionId);

        updateDiscountUsage(order);

        if (order.getUser() != null) {
            int totalProductCount = orderRepository.getTotalQuantityByOrderId(order.getOrderId());
            rewardService.checkRewardableOnOrderAndReward(order.getUser().getId(), order.getOrderId(),
                    totalProductCount, order.getOrderInfo().getTotalAmount());
        }

        try {
            orderEmailService.sendOrderConfirmationEmail(order);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order");
        }

        OrderResponseDTO orderResponse = convertOrderToResponseDTO(order);

        if (order.getUser() != null) {
            try {
                cartService.clearCart(order.getUser().getId());
                log.info("Successfully cleared cart for user: {}", order.getUser().getId());
            } catch (Exception e) {
                log.error("Failed to clear cart for user {}: {}", order.getUser().getId(), e.getMessage());
            }
        }

        return new CheckoutVerificationResult(
                session.getPaymentStatus(),
                session.getAmountTotal(),
                session.getCurrency(),
                session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null,
                receiptUrl,
                intent != null ? intent.getId() : null,
                true,
                orderResponse);
    }

    public void cleanupFailedOrder(String sessionId) {
        try {
            cleanupFailedOrderTransactional(sessionId);
        } catch (Exception e) {
            log.error("Error cleaning up failed order for session {}: {}", sessionId, e.getMessage(), e);
            try {
                enhancedStockLockService.unlockAllBatches(sessionId);
                stockLockService.releaseStock(sessionId);
                log.info("Fallback stock unlock completed for session: {}", sessionId);
            } catch (Exception fallbackEx) {
                log.error("Fallback stock unlock also failed for session {}: {}", sessionId, fallbackEx.getMessage());
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    private synchronized void cleanupFailedOrderTransactional(String sessionId) {
        log.info("Starting cleanup for failed order session: {}", sessionId);
        
        // Double-check to prevent concurrent cleanup
        OrderTransaction tx = transactionRepository.findByStripeSessionId(sessionId).orElse(null);
        if (tx == null) {
            log.info("No transaction found for session: {} - may have already been cleaned up", sessionId);
            enhancedStockLockService.unlockAllBatches(sessionId);
            stockLockService.releaseStock(sessionId);
            return;
        }
        
        if (tx.getStatus() != OrderTransaction.TransactionStatus.PENDING) {
            log.info("Transaction for session: {} is not pending (status: {}) - skipping cleanup", 
                    sessionId, tx.getStatus());
            return;
        }
        
        // Mark transaction as processing to prevent concurrent cleanup
        tx.setStatus(OrderTransaction.TransactionStatus.FAILED);
        transactionRepository.save(tx);
        
        log.info("CLEANUP: Unlocking enhanced batch locks for session: {}", sessionId);
        enhancedStockLockService.unlockAllBatches(sessionId);
        
        log.info("CLEANUP: Unlocking general stock locks for session: {}", sessionId);
        stockLockService.releaseStock(sessionId);
        
        Order order = tx.getOrder();
        Long orderId = order.getOrderId();
                    
        refundPointsForFailedPayment(order);
        
        transactionRepository.flush();
        
        // Delete the order (cascades to related entities)
        log.info("Deleting order: {} for session: {}", orderId, sessionId);
        if (orderRepository.existsById(orderId)) {
            orderRepository.deleteById(orderId);
            orderRepository.flush(); // Force immediate deletion
            log.info("Successfully deleted order: {}", orderId);
        } else {
            log.info("Order {} was already deleted", orderId);
        }
        
        // Clean up temporary session locks
        String tempSessionId = "temp_" + orderId.toString();
        String tempGuestSessionId = "temp_guest_" + orderId.toString();
        
        enhancedStockLockService.unlockAllBatches(tempSessionId);
        stockLockService.releaseStock(tempSessionId);
        enhancedStockLockService.unlockAllBatches(tempGuestSessionId);
        stockLockService.releaseStock(tempGuestSessionId);
        
        log.info("Cleaned up temporary session locks: {} and {}", tempSessionId, tempGuestSessionId);
        
        log.info("Successfully completed cleanup for session: {}", sessionId);
    }

    @Transactional
    public void restoreBatchQuantitiesFromOrder(Order order) {
        try {
            log.info("Restoring batch quantities for cancelled order: {}", order.getOrderId());

            int totalRestoredQuantity = 0;
            Map<String, Integer> restorationSummary = new HashMap<>();

            for (OrderItem orderItem : order.getOrderItems()) {
                log.debug("Processing order item: productId={}, variantId={}, quantity={}",
                        orderItem.getProduct() != null ? orderItem.getProduct().getProductId() : null,
                        orderItem.getProductVariant() != null ? orderItem.getProductVariant().getId() : null,
                        orderItem.getQuantity());

                for (OrderItemBatch orderItemBatch : orderItem.getOrderItemBatches()) {
                    StockBatch batch = orderItemBatch.getStockBatch();
                    int quantityToRestore = orderItemBatch.getQuantityUsed();

                    log.info("Restoring {} units to batch {} (current quantity: {})",
                            quantityToRestore, batch.getBatchNumber(), batch.getQuantity());

                    int newQuantity = batch.getQuantity() + quantityToRestore;
                    batch.setQuantity(newQuantity);

                    if (batch.getStatus() == com.ecommerce.enums.BatchStatus.EMPTY && newQuantity > 0) {
                        batch.setStatus(com.ecommerce.enums.BatchStatus.ACTIVE);
                        log.info("Updated batch {} status from EMPTY to ACTIVE", batch.getBatchNumber());
                    }

                    stockBatchRepository.save(batch);

                    String key = String.format("Batch %s (%s)",
                            batch.getBatchNumber(),
                            orderItemBatch.getWarehouse().getName());
                    restorationSummary.merge(key, quantityToRestore, Integer::sum);
                    totalRestoredQuantity += quantityToRestore;

                    log.debug("Restored {} units to batch {} in warehouse {} (new quantity: {})",
                            quantityToRestore, batch.getBatchNumber(),
                            orderItemBatch.getWarehouse().getName(), newQuantity);
                }
            }

            log.info("Successfully restored {} total units across {} batches for order {}: {}",
                    totalRestoredQuantity, restorationSummary.size(), order.getOrderId(), restorationSummary);

        } catch (Exception e) {
            log.error("Error restoring batch quantities for order {}: {}", order.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to restore batch quantities: " + e.getMessage(), e);
        }
    }

    private boolean lockStockForItems(String sessionId, List<CartItemDTO> items, AddressDto address) {
        try {
            log.info("Starting stock locking for session {} with {} items", sessionId, items.size());
            for (CartItemDTO item : items) {
                log.info(
                        "Item to lock: productId={}, variantId={}, isVariantBased={}, isVariantBasedItem={}, quantity={}",
                        item.getProductId(), item.getVariantId(), item.isVariantBased(), item.isVariantBasedItem(),
                        item.getQuantity());
            }

            Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> allocations = multiWarehouseStockAllocator
                    .allocateStockAcrossWarehouses(items, address);

            log.info("Stock allocation result: {} allocations", allocations.size());
            for (Map.Entry<Long, List<MultiWarehouseStockAllocator.StockAllocation>> entry : allocations.entrySet()) {
                log.info("Key {} has {} allocations", entry.getKey(), entry.getValue().size());
                for (MultiWarehouseStockAllocator.StockAllocation allocation : entry.getValue()) {
                    log.info("Allocation: warehouseId={}, stockId={}, quantity={}",
                            allocation.getWarehouseId(), allocation.getStockId(), allocation.getQuantity());
                }
            }

            if (allocations.isEmpty()) {
                log.error("No stock allocation found for items");
                return false;
            }

            boolean batchLocked = stockLockService.lockStockFromBatches(sessionId, allocations);
            boolean generalLocked = stockLockService.lockStockFromMultipleWarehouses(sessionId, allocations);

            if (!batchLocked || !generalLocked) {
                log.error("Failed to lock stock - batch locked: {}, general locked: {}", batchLocked, generalLocked);
                stockLockService.unlockAllBatches(sessionId);
                stockLockService.releaseStock(sessionId);
                return false;
            }

            log.info("Successfully allocated stock across {} warehouses for session {}",
                    allocations.size(), sessionId);
            return true;
        } catch (Exception e) {
            log.error("Error locking stock: {}", e.getMessage(), e);
            stockLockService.unlockAllBatches(sessionId);
            stockLockService.releaseStock(sessionId);
            return false;
        }
    }

    public BigDecimal calculateShippingCost(AddressDto deliveryAddress, List<CartItemDTO> items,
            BigDecimal orderValue) {
        try {
            return shippingCostService.calculateOrderShippingCost(deliveryAddress, items, orderValue);
        } catch (Exception e) {
            log.error("Error calculating shipping cost: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public com.ecommerce.dto.PaymentSummaryDTO calculatePaymentSummary(AddressDto deliveryAddress,
            List<CartItemDTO> items, UUID userId) {
        log.info("Calculating payment summary for {} items, userId: {}", items.size(), userId);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        int totalProductCount = 0;

        for (CartItemDTO item : items) {
            BigDecimal itemPrice = BigDecimal.ZERO;
            BigDecimal originalPrice = BigDecimal.ZERO;

            if (item.getVariantId() != null) {
                try {
                    ProductVariant variant = variantRepository.findById(item.getVariantId())
                            .orElseThrow(
                                    () -> new EntityNotFoundException(
                                            "Variant not found with ID: " + item.getVariantId()));
                    originalPrice = variant.getPrice();

                    if (variant.getDiscount() != null) {
                        validateDiscountValidity(variant.getDiscount());
                    }

                    itemPrice = calculateDiscountedPrice(variant);
                } catch (Exception e) {
                    log.error("Error finding variant {}: {}", item.getVariantId(), e.getMessage());
                    throw new EntityNotFoundException("Variant not found with ID: " + item.getVariantId());
                }
            } else if (item.getProductId() != null) {
                try {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(
                                    () -> new EntityNotFoundException(
                                            "Product not found with ID: " + item.getProductId()));
                    originalPrice = product.getPrice();

                    if (product.getDiscount() != null) {
                        validateDiscountValidity(product.getDiscount());
                    }

                    itemPrice = calculateDiscountedPrice(product);
                    log.info("Found product: {}, originalPrice: {}, discountedPrice: {}",
                            product.getProductId(), originalPrice, itemPrice);
                } catch (Exception e) {
                    log.error("Error finding product {}: {}", item.getProductId(), e.getMessage());
                    throw new EntityNotFoundException("Product not found with ID: " + item.getProductId());
                }
            } else {
                log.warn("Cart item has neither productId nor variantId: {}", item);
                continue;
            }

            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal itemDiscount = originalPrice.subtract(itemPrice)
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(itemTotal);
            discountAmount = discountAmount.add(itemDiscount);
            totalProductCount += item.getQuantity();
        }

        BigDecimal shippingCost = calculateShippingCost(deliveryAddress, items, subtotal);
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingCost).add(taxAmount);

        Integer rewardPoints = 0;
        BigDecimal rewardPointsValue = BigDecimal.ZERO;
        if (userId != null) {
            rewardPoints = rewardService.getPreviewPointsForOrder(totalProductCount, subtotal);
            rewardPointsValue = rewardService.calculatePointsValue(rewardPoints);
        }

        // Get detailed shipping information
        com.ecommerce.dto.ShippingDetailsDTO shippingDetails = shippingCostService
                .calculateShippingDetails(deliveryAddress, items, subtotal);

        return com.ecommerce.dto.PaymentSummaryDTO.builder()
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingCost(shippingCost)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .rewardPoints(rewardPoints)
                .rewardPointsValue(rewardPointsValue)
                .currency("USD")
                .distanceKm(shippingDetails.getDistanceKm())
                .costPerKm(shippingDetails.getCostPerKm())
                .selectedWarehouseName(shippingDetails.getSelectedWarehouseName())
                .selectedWarehouseCountry(shippingDetails.getSelectedWarehouseCountry())
                .isInternationalShipping(shippingDetails.getIsInternationalShipping())
                .build();
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Authentication object: {}", auth);

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                log.warn("User not authenticated. Auth: {}, isAuthenticated: {}, principal: {}",
                        auth, auth != null ? auth.isAuthenticated() : "null",
                        auth != null ? auth.getPrincipal() : "null");
                throw new IllegalStateException("User not authenticated");
            }

            Object principal = auth.getPrincipal();
            log.debug("Authentication principal type: {}, value: {}",
                    principal.getClass().getName(), principal);

            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                log.debug("Found CustomUserDetails with email: {}", email);
                return userRepository.findByUserEmail(email)
                        .map(com.ecommerce.entity.User::getId)
                        .orElseThrow(() -> new IllegalStateException("User not found: " + email));
            }

            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                log.debug("Found User entity with ID: {}", user.getId());
                return user.getId();
            }

            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                log.debug("Found UserDetails with email: {}", email);
                return userRepository.findByUserEmail(email)
                        .map(com.ecommerce.entity.User::getId)
                        .orElseThrow(() -> new IllegalStateException("User not found: " + email));
            }

            String name = auth.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                log.debug("Fallback to auth name: {}", name);
                return userRepository.findByUserEmail(name)
                        .map(com.ecommerce.entity.User::getId)
                        .orElseThrow(() -> new IllegalStateException("User not found: " + name));
            }

            throw new IllegalStateException("Unable to determine current user. Principal type: " +
                    principal.getClass().getName() + ", value: " + principal);
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
            throw new IllegalStateException("Authentication error: " + e.getMessage());
        }
    }

    private void updateDiscountUsage(Order order) {
        log.info("Updating discount usage for order: {}", order.getOrderId());

        for (OrderItem item : order.getOrderItems()) {
            if (item.isVariantBased() && item.getProductVariant() != null) {
                ProductVariant variant = item.getProductVariant();
                if (variant.getDiscount() != null) {
                    Discount discount = variant.getDiscount();
                    discount.incrementUsage();

                    if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
                        discount.setActive(false);
                        log.info("Discount {} reached usage limit and has been disabled", discount.getDiscountId());
                    }

                    discountRepository.save(discount);
                    log.info("Incremented usage for variant discount: {} (used: {}/{})",
                            discount.getDiscountId(), discount.getUsedCount(), discount.getUsageLimit());
                }
            } else if (item.getProduct() != null) {
                Product product = item.getProduct();
                if (product.getDiscount() != null) {
                    Discount discount = product.getDiscount();
                    discount.incrementUsage();

                    if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
                        discount.setActive(false);
                        log.info("Discount {} reached usage limit and has been disabled", discount.getDiscountId());
                    }

                    discountRepository.save(discount);
                    log.info("Incremented usage for product discount: {} (used: {}/{})",
                            discount.getDiscountId(), discount.getUsedCount(), discount.getUsageLimit());
                }
            }
        }
    }

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

    private void createOrderItemBatches(Order order, CartItemDTO cartItem,
            List<FEFOStockAllocationService.BatchAllocation> allocations) {

        OrderItem orderItem = order.getOrderItems().stream()
                .filter(oi -> matchesCartItem(oi, cartItem))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OrderItem not found for cart item"));

        for (FEFOStockAllocationService.BatchAllocation allocation : allocations) {
            OrderItemBatch orderItemBatch = new OrderItemBatch();
            orderItemBatch.setOrderItem(orderItem);
            orderItemBatch.setStockBatch(allocation.getStockBatch());
            orderItemBatch.setWarehouse(allocation.getWarehouse());
            orderItemBatch.setQuantityUsed(allocation.getQuantityAllocated());

            orderItem.addOrderItemBatch(orderItemBatch);
            orderItemBatchRepository.save(orderItemBatch);
        }
    }

    private boolean matchesCartItem(OrderItem orderItem, CartItemDTO cartItem) {
        if (cartItem.getVariantId() != null) {
            return orderItem.getProductVariant() != null &&
                    orderItem.getProductVariant().getId().equals(cartItem.getVariantId());
        } else {
            return orderItem.getProduct() != null &&
                    orderItem.getProduct().getProductId().equals(cartItem.getProductId());
        }
    }

    private OrderResponseDTO convertOrderToResponseDTO(Order order) {
        OrderInfo info = order.getOrderInfo();
        OrderAddress addr = order.getOrderAddress();
        log.info("The order addressses obtained are "  + addr);
        OrderTransaction tx = order.getOrderTransaction();

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getOrderId());
        dto.setUserId(
                order.getUser() != null && order.getUser().getId() != null ? order.getUser().getId().toString() : null);
        dto.setOrderNumber(order.getOrderCode());
        dto.setPickupToken(order.getPickupToken());
        dto.setPickupTokenUsed(order.getPickupTokenUsed());
        dto.setStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Set customer information from OrderCustomerInfo entity
        if (order.getOrderCustomerInfo() != null) {
            OrderResponseDTO.CustomerInfo customerInfo = new OrderResponseDTO.CustomerInfo();
            customerInfo.setName(order.getOrderCustomerInfo().getFirstName() + " " + order.getOrderCustomerInfo().getLastName());
            customerInfo.setEmail(order.getOrderCustomerInfo().getEmail());
            customerInfo.setPhone(order.getOrderCustomerInfo().getPhoneNumber());
            dto.setCustomerInfo(customerInfo);
            log.info("The customer info are: " + customerInfo);
        }

        // Set order info
        if (info != null) {
            dto.setSubtotal(info.getTotalAmount());
            dto.setTax(info.getTaxAmount());
            dto.setShipping(info.getShippingCost());
            dto.setDiscount(info.getDiscountAmount());
            dto.setTotal(info.getFinalAmount());
            dto.setNotes(info.getNotes());
        }

        if (addr != null) {
            OrderResponseDTO.ShippingAddress shippingAddress = new OrderResponseDTO.ShippingAddress();
            shippingAddress.setStreet(addr.getStreet());
            shippingAddress.setCountry(addr.getCountry());

            if (addr.getRegions() != null && !addr.getRegions().isEmpty()) {
                String[] regions = addr.getRegions().split(",");
                if (regions.length >= 2) {
                    shippingAddress.setCity(regions[0].trim());
                    shippingAddress.setState(regions[1].trim());
                } else if (regions.length == 1) {
                    shippingAddress.setCity(regions[0].trim());
                    shippingAddress.setState("");
                }
            }
            dto.setShippingAddress(shippingAddress);
        }

        // Set payment information
        if (tx != null) {
            dto.setPaymentMethod(tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null);
            dto.setPaymentStatus(tx.getStatus() != null ? tx.getStatus().name() : null);
        }

        // Set transaction information
        if (tx != null) {
            OrderTransactionDTO transactionDTO = new OrderTransactionDTO();
            transactionDTO.setOrderTransactionId(tx.getOrderTransactionId() != null ? tx.getOrderTransactionId().toString() : null);
            transactionDTO.setTransactionRef(tx.getTransactionRef());
            transactionDTO.setPaymentMethod(tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null);
            transactionDTO.setStatus(tx.getStatus() != null ? tx.getStatus().name() : null);
            transactionDTO.setOrderAmount(tx.getOrderAmount());
            transactionDTO.setPointsUsed(tx.getPointsUsed());
            transactionDTO.setPointsValue(tx.getPointsValue());
            transactionDTO.setStripePaymentIntentId(tx.getStripePaymentIntentId());
            transactionDTO.setReceiptUrl(tx.getReceiptUrl());
            transactionDTO.setPaymentDate(tx.getPaymentDate());
            transactionDTO.setCreatedAt(tx.getCreatedAt());
            transactionDTO.setUpdatedAt(tx.getUpdatedAt());
            dto.setTransaction(transactionDTO);
        }

        // Set order items with product/variant information
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<OrderResponseDTO.OrderItem> itemDTOs = order.getOrderItems().stream().map(this::mapOrderItemToResponseDTO).toList();
            dto.setItems(itemDTOs);
        }

        return dto;
    }

    private OrderItemDTO mapOrderItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getOrderItemId() != null ? item.getOrderItemId().toString() : null);
        dto.setProductId(item.getProduct() != null ? item.getProduct().getProductId().toString() : null);
        dto.setVariantId(item.getProductVariant() != null ? item.getProductVariant().getId().toString() : null);
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setTotalPrice(item.getSubtotal());

        // Set product information
        if (item.getProduct() != null) {
            SimpleProductDTO productDto = new SimpleProductDTO();
            productDto.setProductId(item.getProduct().getProductId().toString());
            productDto.setName(item.getProduct().getProductName());
            productDto.setDescription(item.getProduct().getDescription());
            productDto.setPrice(item.getProduct().getDiscountedPrice().doubleValue());

            // Get product images
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                String[] imageUrls = item.getProduct().getImages().stream()
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
                        .toArray(String[]::new);
                productDto.setImages(imageUrls);
            }

            dto.setProduct(productDto);
        }

        // Set variant information if this is a variant-based item
        if (item.getProductVariant() != null) {
            SimpleProductDTO variantDto = new SimpleProductDTO();
            variantDto.setProductId(item.getProductVariant().getId().toString());
            variantDto.setName(item.getProductVariant().getVariantName());
            variantDto.setDescription(item.getProductVariant().getVariantSku());
            variantDto.setPrice(item.getProductVariant().getDiscountedPrice() != null
                    ? item.getProductVariant().getDiscountedPrice().doubleValue()
                    : item.getProductVariant().getPrice().doubleValue());

            // Get variant images - if variant has images, use them; otherwise use product
            // images
            if (item.getProductVariant().getImages() != null && !item.getProductVariant().getImages().isEmpty()) {
                String[] imageUrls = item.getProductVariant().getImages().stream()
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
                        .toArray(String[]::new);
                variantDto.setImages(imageUrls);
            } else if (item.getProduct() != null && item.getProduct().getImages() != null
                    && !item.getProduct().getImages().isEmpty()) {
                // Fallback to product images if variant has no images
                String[] imageUrls = item.getProduct().getImages().stream()
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
                        .toArray(String[]::new);
                variantDto.setImages(imageUrls);
            }

            dto.setVariant(variantDto);
        }

        return dto;
    }

    private OrderResponseDTO.OrderItem mapOrderItemToResponseDTO(OrderItem item) {
        OrderResponseDTO.OrderItem dto = new OrderResponseDTO.OrderItem();
        dto.setId(item.getOrderItemId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setTotalPrice(item.getSubtotal());
        
        // Set product info
        if (item.getProduct() != null) {
            OrderResponseDTO.Product product = new OrderResponseDTO.Product();
            product.setId(item.getProduct().getProductId());
            product.setName(item.getProduct().getProductName());
            dto.setProduct(product);
        }
        
        // Set variant info if available
        if (item.getProductVariant() != null) {
            OrderResponseDTO.Variant variant = new OrderResponseDTO.Variant();
            variant.setId(item.getProductVariant().getId());
            variant.setName(item.getProductVariant().getVariantName());
            dto.setVariant(variant);
        }
        
        // Set return eligibility (placeholder)
        dto.setReturnEligible(true);
        dto.setMaxReturnDays(30);
        dto.setDaysRemainingForReturn(25);
        
        return dto;
    }

    private Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> convertFEFOToStockAllocations(
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations) {

        Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> stockAllocations = new HashMap<>();

        for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : fefoAllocations
                .entrySet()) {
            CartItemDTO cartItem = entry.getKey();
            List<FEFOStockAllocationService.BatchAllocation> batchAllocations = entry.getValue();

            // Group by stock ID and sum quantities
            Map<Long, Integer> stockQuantities = new HashMap<>();
            Map<Long, FEFOStockAllocationService.BatchAllocation> stockToAllocation = new HashMap<>();

            for (FEFOStockAllocationService.BatchAllocation batchAllocation : batchAllocations) {
                Long stockId = batchAllocation.getStockBatch().getStock().getId();
                stockQuantities.merge(stockId, batchAllocation.getQuantityAllocated(), Integer::sum);
                stockToAllocation.put(stockId, batchAllocation); // Keep reference for warehouse info
            }

            // Convert to stock allocations
            List<MultiWarehouseStockAllocator.StockAllocation> allocations = new ArrayList<>();
            for (Map.Entry<Long, Integer> stockEntry : stockQuantities.entrySet()) {
                Long stockId = stockEntry.getKey();
                Integer quantity = stockEntry.getValue();
                FEFOStockAllocationService.BatchAllocation refAllocation = stockToAllocation.get(stockId);

                MultiWarehouseStockAllocator.StockAllocation stockAllocation = new MultiWarehouseStockAllocator.StockAllocation(
                        refAllocation.getWarehouse().getId(),
                        refAllocation.getWarehouse().getName(),
                        stockId,
                        quantity,
                        0.0 // Distance not needed for locking
                );
                allocations.add(stockAllocation);
            }

            // Use a unique key for each cart item (could be productId or variantId)
            Long key = cartItem.getVariantId() != null ? cartItem.getVariantId()
                    : (cartItem.getProductId() != null ? cartItem.getProductId().hashCode() : cartItem.hashCode());
            stockAllocations.put(key, allocations);
        }

        log.info("Converted {} FEFO allocation groups to {} stock allocation groups",
                fefoAllocations.size(), stockAllocations.size());
        return stockAllocations;
    }

    private void validateCartItems(List<CartItemDTO> items) {
        for (CartItemDTO item : items) {
            validateCartItem(item);
        }
    }

    private void validateCartItem(CartItemDTO item) {
        if (item.getVariantId() != null) {
            validateVariantItem(item);
        } else if (item.getProductId() != null) {
            validateProductItem(item);
        } else {
            throw new com.ecommerce.Exception.CheckoutValidationException("INVALID_ITEM",
                    "Cart item must have either productId or variantId");
        }
    }

    private void validateVariantItem(CartItemDTO item) {
        ProductVariant variant = variantRepository.findById(item.getVariantId())
                .orElseThrow(() -> new com.ecommerce.Exception.CheckoutValidationException("VARIANT_NOT_FOUND",
                        "Product variant not found with ID: " + item.getVariantId()));

        Product product = variant.getProduct();
        if (product == null) {
            throw new com.ecommerce.Exception.CheckoutValidationException("PRODUCT_NOT_FOUND",
                    "Product not found for variant ID: " + item.getVariantId());
        }

        if (!product.isActive()) {
            throw new com.ecommerce.Exception.CheckoutValidationException("PRODUCT_INACTIVE",
                    "Product is not active: " + product.getProductName());
        }

        if (!Boolean.TRUE.equals(product.getDisplayToCustomers())) {
            throw new com.ecommerce.Exception.CheckoutValidationException("PRODUCT_NOT_AVAILABLE",
                    "Product is not available for customers: " + product.getProductName());
        }

        if (!variant.isActive()) {
            throw new com.ecommerce.Exception.CheckoutValidationException("VARIANT_INACTIVE",
                    "Product variant is not active: " + variant.getVariantSku());
        }

        if (!productAvailabilityService.isVariantAvailableForCustomers(variant)) {
            int totalStock = productAvailabilityService.getVariantTotalStock(variant);
            String reason = totalStock <= 0 ? "out of stock" : "not available for customers";
            throw new com.ecommerce.Exception.CheckoutValidationException("VARIANT_NOT_AVAILABLE",
                    "Product variant is not available: " + variant.getVariantSku() + " (" + reason + ", stock: "
                            + totalStock + ")");
        }

        int availableStock = productAvailabilityService.getVariantTotalStock(variant);
        if (availableStock < item.getQuantity()) {
            throw new com.ecommerce.Exception.CheckoutValidationException("INSUFFICIENT_STOCK",
                    "Insufficient stock for variant " + variant.getVariantSku() +
                            ". Available: " + availableStock + ", Requested: " + item.getQuantity());
        }
    }

    private void validateProductItem(CartItemDTO item) {
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new com.ecommerce.Exception.CheckoutValidationException("PRODUCT_NOT_FOUND",
                        "Product not found with ID: " + item.getProductId()));

        if (!product.isActive()) {
            throw new com.ecommerce.Exception.CheckoutValidationException("PRODUCT_INACTIVE",
                    "Product is not active: " + product.getProductName());
        }

        if (!Boolean.TRUE.equals(product.getDisplayToCustomers())) {
            throw new com.ecommerce.Exception.CheckoutValidationException("PRODUCT_NOT_AVAILABLE",
                    "Product is not available for customers: " + product.getProductName());
        }

        if (!productAvailabilityService.isProductAvailableForCustomers(product)) {
            throw new com.ecommerce.Exception.CheckoutValidationException("PRODUCT_NOT_AVAILABLE",
                    "Product is not available: " + product.getProductName());
        }

        int availableStock = productAvailabilityService.getTotalAvailableStock(product);
        if (availableStock < item.getQuantity()) {
            throw new com.ecommerce.Exception.CheckoutValidationException("INSUFFICIENT_STOCK",
                    "Insufficient stock for product " + product.getProductName() +
                            ". Available: " + availableStock + ", Requested: " + item.getQuantity());
        }
    }

    private boolean lockStockWithFEFOAllocation(String sessionId, List<CartItemDTO> items, AddressDto shippingAddress) {
        try {
            log.info("Starting FEFO allocation and batch locking for session: {}", sessionId);

            // Step 1: Perform FEFO allocation to determine which batches to use
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations = enhancedWarehouseAllocator
                    .allocateStockWithFEFO(items, shippingAddress);

            if (fefoAllocations.isEmpty()) {
                log.error("FEFO allocation failed - no stock available");
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
                log.info("Successfully locked {} batches for session: {}", lockRequests.size(), sessionId);
            } else {
                log.error("Failed to lock batches for session: {}", sessionId);
            }

            return lockSuccess;

        } catch (Exception e) {
            log.error("Error during FEFO allocation and batch locking for session {}: {}", sessionId, e.getMessage(),
                    e);
            return false;
        }
    }

    /**
     * Helper method to get product name from cart item
     */
    private String getProductName(CartItemDTO item) {
        try {
            if (item.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(item.getVariantId()).orElse(null);
                if (variant != null && variant.getProduct() != null) {
                    return variant.getProduct().getProductName();
                }
            } else if (item.getProductId() != null) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    return product.getProductName();
                }
            }
        } catch (Exception e) {
            log.warn("Error getting product name for cart item: {}", e.getMessage());
        }
        return "Unknown Product";
    }

    /**
     * Helper method to get variant name from cart item
     */
    private String getVariantName(CartItemDTO item) {
        try {
            if (item.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(item.getVariantId()).orElse(null);
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

    /**
     * Cleanup expired batch locks
     */
    public void cleanupExpiredBatchLocks() {
        try {
            enhancedStockLockService.cleanupExpiredLocks();
            log.info("Successfully cleaned up expired batch locks");
        } catch (Exception e) {
            log.error("Failed to cleanup expired batch locks: {}", e.getMessage());
            throw new RuntimeException("Failed to cleanup expired batch locks: " + e.getMessage(), e);
        }
    }

    /**
     * Get locked stock information for a session
     */
    public Map<String, Object> getLockedStockInfo(String sessionId) {
        try {
            return enhancedStockLockService.getBatchLockInfo(sessionId);
        } catch (Exception e) {
            log.error("Failed to get locked stock info for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to get locked stock info: " + e.getMessage(), e);
        }
    }

    private void refundPointsForFailedPayment(Order order) {
        try {
            if (order.getOrderTransaction() == null) {
                return; 
            }

            Integer pointsUsed = order.getOrderTransaction().getPointsUsed();
            if (pointsUsed == null || pointsUsed <= 0) {
                return; 
            }

            if (order.getUser() == null) {
                log.warn("Cannot refund points for order {} - no user associated", order.getOrderId());
                return;
            }

            String refundDescription = String.format("Points refunded for failed hybrid payment (Order #%s)",
                    order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString());

            rewardService.refundPointsForCancelledOrder(
                    order.getUser().getId(),
                    pointsUsed,
                    refundDescription);

            log.info("Refunded {} points to user {} for failed hybrid payment order {}",
                    pointsUsed, order.getUser().getId(), order.getOrderId());

        } catch (Exception e) {
            log.error("Error refunding points for failed payment order {}: {}",
                    order.getOrderId(), e.getMessage(), e);
        }
    }
}
