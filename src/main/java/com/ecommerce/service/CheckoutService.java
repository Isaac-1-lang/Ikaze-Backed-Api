package com.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CheckoutRequest;
import com.ecommerce.dto.CheckoutVerificationResult;
import com.ecommerce.dto.GuestCheckoutRequest;
import com.ecommerce.entity.Discount;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderInfo;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.DiscountRepository;
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

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DiscountRepository discountRepository;
    private final StripeService stripeService;
    private final StockLockService stockLockService;
    private final MultiWarehouseStockAllocator multiWarehouseStockAllocator;
    private final com.ecommerce.service.ShippingCostService shippingCostService;
    private final com.ecommerce.service.RewardService rewardService;

    public String createCheckoutSession(CheckoutRequest req) throws Exception {
        log.info("Creating checkout session for user");

        User user = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + getCurrentUserId()));

        String sessionId = java.util.UUID.randomUUID().toString();

        if (req.getShippingAddress() != null) {
            boolean stockLocked = lockStockForItems(sessionId, req.getItems(), req.getShippingAddress());
            if (!stockLocked) {
                throw new IllegalStateException("Unable to lock stock for all items");
            }
        }

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
            customerInfo.setPostalCode(req.getShippingAddress().getPostalCode());
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

        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(paymentSummary.getTotalAmount());
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        Order saved = orderRepository.save(order);
        log.info("Order created with ID: {}", saved.getOrderId());

        String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, req.getCurrency(), req.getPlatform());
        log.info("Stripe session created successfully");

        return sessionUrl;
    }

    public String createGuestCheckoutSession(GuestCheckoutRequest req) throws Exception {
        log.info("Creating guest checkout session");

        String sessionId = java.util.UUID.randomUUID().toString();

        if (req.getAddress() != null) {
            boolean stockLocked = lockStockForItems(sessionId, req.getItems(), req.getAddress());
            if (!stockLocked) {
                throw new IllegalStateException("Unable to lock stock for all items");
            }
        }

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
            customerInfo.setPostalCode(req.getAddress().getPostalCode());
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

        // Calculate payment summary including shipping costs for guest checkout
        com.ecommerce.dto.PaymentSummaryDTO paymentSummary = calculatePaymentSummary(req.getAddress(), req.getItems(),
                null);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrder(order);
        orderInfo.setTotalAmount(paymentSummary.getTotalAmount());
        orderInfo.setTaxAmount(paymentSummary.getTaxAmount());
        orderInfo.setShippingCost(paymentSummary.getShippingCost());
        orderInfo.setDiscountAmount(paymentSummary.getDiscountAmount());
        order.setOrderInfo(orderInfo);

        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(paymentSummary.getTotalAmount());
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        Order saved = orderRepository.save(order);
        log.info("Guest order created with ID: {}", saved.getOrderId());

        String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, "usd", req.getPlatform());
        log.info("Guest Stripe session created successfully");

        return sessionUrl;
    }

    @Transactional
    public CheckoutVerificationResult verifyCheckoutSession(String sessionId) throws Exception {
        log.info("Verifying checkout session: {}", sessionId);

        OrderTransaction tx = transactionRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("No matching payment record"));

        Session session = stripeService.retrieveSession(sessionId);

        if (session == null) {
            throw new EntityNotFoundException("Session not found on Stripe");
        }

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            Order order = tx.getOrder();
            orderRepository.delete(order);
            stockLockService.releaseStock(sessionId);
            log.info("Payment failed, order deleted: {}", order.getOrderId());
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

        Order order = tx.getOrder();
        order.setOrderStatus(Order.OrderStatus.PROCESSING);
        for (OrderItem item : order.getOrderItems()) {
            if (item.isVariantBased()) {
                ProductVariant variant = variantRepository.findByIdForUpdate(item.getProductVariant().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
                int totalStock = variant.getTotalStockQuantity();
                if (totalStock < item.getQuantity()) {
                    throw new IllegalStateException("Insufficient stock for variant " + variant.getId());
                }
                log.info("Stock check passed for variant {}: available {}, requested {}",
                        variant.getId(), totalStock, item.getQuantity());
            } else {
                Product product = productRepository.findById(item.getProduct().getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Product not found"));
                int totalStock = product.getTotalStockQuantity();
                if (totalStock < item.getQuantity()) {
                    throw new IllegalStateException("Insufficient stock for product " + product.getProductId());
                }
                log.info("Stock check passed for product {}: available {}, requested {}",
                        product.getProductId(), totalStock, item.getQuantity());
            }
        }
        orderRepository.save(order);
        stockLockService.confirmStock(sessionId);

        updateDiscountUsage(order);

        if (order.getUser() != null) {
            int totalProductCount = order.getOrderItems().stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
            rewardService.checkRewardableOnOrderAndReward(order.getUser().getId(), order.getOrderId(),
                    totalProductCount, order.getOrderInfo().getTotalAmount());
        }

        log.info("Order status updated to processing");

        return new CheckoutVerificationResult(
                session.getPaymentStatus(),
                session.getAmountTotal(),
                session.getCurrency(),
                session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null,
                receiptUrl,
                intent != null ? intent.getId() : null,
                session.getId(),
                true);
    }

    @Transactional
    public void cleanupFailedOrder(String sessionId) {
        try {
            OrderTransaction tx = transactionRepository.findByStripeSessionId(sessionId).orElse(null);
            if (tx != null && tx.getStatus() == OrderTransaction.TransactionStatus.PENDING) {
                Order order = tx.getOrder();
                orderRepository.delete(order);
                log.info("Cleaned up failed order: {}", order.getOrderId());
            }
            stockLockService.releaseStock(sessionId);
        } catch (Exception e) {
            log.error("Error cleaning up failed order for session {}: {}", sessionId, e.getMessage());
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

            boolean locked = stockLockService.lockStockFromMultipleWarehouses(sessionId, allocations);
            if (!locked) {
                log.error("Failed to lock stock from multiple warehouses");
                return false;
            }

            log.info("Successfully allocated stock across {} warehouses for session {}",
                    allocations.size(), sessionId);
            return true;
        } catch (Exception e) {
            log.error("Error locking stock: {}", e.getMessage(), e);
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
            log.info("Processing cart item: productId={}, variantId={}, quantity={}",
                    item.getProductId(), item.getVariantId(), item.getQuantity());

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
                    log.info("Found variant: {}, originalPrice: {}, discountedPrice: {}",
                            variant.getId(), originalPrice, itemPrice);
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

    public Map<String, Object> getLockedStockInfo(String sessionId) {
        return stockLockService.getLockedStockInfo(sessionId);
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }

            Object principal = auth.getPrincipal();

            // If principal is CustomUserDetails, extract email and find user
            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            // If principal is User entity
            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                return user.getId();
            }

            // If principal is UserDetails
            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            // Fallback to auth name
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name).map(com.ecommerce.entity.User::getId).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
        }
        return null;
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
            validateDiscountValidity(variant.getDiscount());
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    variant.getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
            return variant.getPrice().multiply(discountMultiplier);
        }

        if (variant.getProduct() != null && variant.getProduct().getDiscount() != null
                && variant.getProduct().getDiscount().isValid() && variant.getProduct().getDiscount().isActive()) {
            validateDiscountValidity(variant.getProduct().getDiscount());
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    variant.getProduct().getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
            return variant.getPrice().multiply(discountMultiplier);
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
        if (product.getDiscount() != null && product.getDiscount().isValid() && product.getDiscount().isActive()) {
            validateDiscountValidity(product.getDiscount());
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    product.getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
            return product.getPrice().multiply(discountMultiplier);
        }

        if (product.isOnSale() && product.getSalePercentage() != null && product.getSalePercentage() > 0) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(product.getSalePercentage()).divide(BigDecimal.valueOf(100.0)));
            return product.getPrice().multiply(discountMultiplier);
        }

        return product.getPrice();
    }

    private void validateDiscountValidity(com.ecommerce.entity.Discount discount) {
        LocalDateTime now = LocalDateTime.now();

        if (!discount.isActive()) {
            throw new IllegalStateException("Discount is not active: " + discount.getDiscountId());
        }

        if (now.isBefore(discount.getStartDate())) {
            throw new IllegalStateException("Discount has not started yet: " + discount.getDiscountId());
        }

        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new IllegalStateException("Discount has expired: " + discount.getDiscountId());
        }

        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            throw new IllegalStateException("Discount usage limit exceeded: " + discount.getDiscountId());
        }

        log.info("Discount validation passed for: {}", discount.getDiscountId());
    }
}
