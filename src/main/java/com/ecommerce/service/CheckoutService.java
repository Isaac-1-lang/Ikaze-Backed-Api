package com.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CheckoutRequest;
import com.ecommerce.dto.CheckoutVerificationResult;
import com.ecommerce.dto.GuestCheckoutRequest;
import com.ecommerce.dto.OrderResponseDTO;
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
import com.ecommerce.repository.WarehouseRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.RewardSystemRepository;
import com.ecommerce.repository.ShopOrderRepository;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.entity.ShopOrder;
import com.ecommerce.entity.ShopOrderTransaction;
import com.ecommerce.service.FEFOStockAllocationService;
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
    private final WarehouseRepository warehouseRepository;
    private final ShopRepository shopRepository;
    private final RewardSystemRepository rewardSystemRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final StripeService stripeService;
    private final StockLockService stockLockService;
    private final MultiWarehouseStockAllocator multiWarehouseStockAllocator;
    private final ShippingCostService shippingCostService;
    private final RewardService rewardService;
    private final ProductAvailabilityService productAvailabilityService;
    private final EnhancedMultiWarehouseAllocator enhancedWarehouseAllocator;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final StockBatchRepository stockBatchRepository;
    private final OrderEmailService orderEmailService;
    private final EnhancedStockLockService enhancedStockLockService;
    private final CartService cartService;
    private final MoneyFlowService moneyFlowService;
    private final RoadValidationService roadValidationService;
    private final OrderActivityLogService activityLogService;

    // Helper for persistent debug logging
    private void logDebugToFile(String message) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("checkout_debug_logs.txt");
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logLine = timestamp + " - " + message + "\n";
            java.nio.file.Files.write(path, logLine.getBytes(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.error("Failed to write to debug file: " + e.getMessage());
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUserEmail(username)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + username));
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    // @Transactional
    public String createCheckoutSession(CheckoutRequest req) throws Exception {
        logDebugToFile("START: createCheckoutSession");

        try {
            if (req.getShippingAddress() == null || req.getShippingAddress().getCountry() == null) {
                logDebugToFile("ERROR: Missing shipping address or country");
                throw new IllegalArgumentException("Shipping address and country are required");
            }
            logDebugToFile("Validated shipping address present");

            // Validate cart items
            logDebugToFile("Validating cart items...");
            validateCartItems(req.getItems());
            logDebugToFile("Cart validation complete. Items count: " + req.getItems().size());

            // Get authenticated user
            User user = getAuthenticatedUser();
            logDebugToFile("User found: " + user.getId());

            // Allocate stock using FEFO across warehouses
            logDebugToFile("Starting FEFO stock allocation...");
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations = enhancedWarehouseAllocator
                    .allocateStockWithFEFO(req.getItems(), req.getShippingAddress());
            logDebugToFile("FEFO allocation complete");

            // Calculate payment summary (validates shops and warehouses)
            logDebugToFile("Calculating payment summary...");
            com.ecommerce.dto.PaymentSummaryDTO paymentSummary = calculatePaymentSummary(req.getShippingAddress(),
                    req.getItems(), user.getId());
            logDebugToFile("Payment summary calculated - Total: " + paymentSummary.getTotalAmount());

            // Create main Order entity
            Order order = new Order();
            order.setUser(user);
            order.setStatus("PENDING");
            logDebugToFile("Initialized Order entity");

            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setSubtotal(paymentSummary.getSubtotal());
            orderInfo.setShippingCost(paymentSummary.getShippingCost());
            orderInfo.setTaxAmount(paymentSummary.getTaxAmount());
            orderInfo.setTotalAmount(paymentSummary.getTotalAmount());
            orderInfo.setDiscountAmount(paymentSummary.getDiscountAmount());
            order.setOrderInfo(orderInfo);

            OrderAddress address = new OrderAddress();
            address.setStreet(req.getShippingAddress().getStreetAddress());
            address.setRegions(req.getShippingAddress().getCity() + ", " + req.getShippingAddress().getState());
            address.setCountry(req.getShippingAddress().getCountry());
            address.setLatitude(req.getShippingAddress().getLatitude());
            address.setLongitude(req.getShippingAddress().getLongitude());
            address.setRoadName(req.getShippingAddress().getStreetAddress());
            order.setOrderAddress(address);

            OrderCustomerInfo customerInfo = new OrderCustomerInfo();
            customerInfo.setFirstName(user.getFirstName());
            customerInfo.setLastName(user.getLastName());
            customerInfo.setEmail(user.getUserEmail());
            customerInfo.setPhoneNumber(user.getPhoneNumber());
            order.setOrderCustomerInfo(customerInfo);
            logDebugToFile("Order details (Info, Address, Customer) set");

            // Save initial order to get ID
            Order saved = orderRepository.save(order);
            logDebugToFile("Initial order saved with ID: " + saved.getOrderId());

            // Create main Transaction Record FIRST (before shop loop)
            logDebugToFile("Creating OrderTransaction record...");
            OrderTransaction tx = new OrderTransaction();
            tx.setOrder(saved);
            tx.setOrderAmount(orderInfo.getTotalAmount());
            tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
            tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
            logDebugToFile("OrderTransaction saved with ID: " + tx.getOrderTransactionId());

            // Group items by shop and create ShopOrders
            Map<Shop, List<CartItemDTO>> itemsByShop = new HashMap<>();
            for (CartItemDTO item : req.getItems()) {
                Shop shop = getShopForCartItem(item);
                if (shop == null) {
                    throw new EntityNotFoundException("Product or variant not found or has no associated shop");
                }
                itemsByShop.computeIfAbsent(shop, k -> new ArrayList<>()).add(item);
            }
            logDebugToFile("Shop count: " + itemsByShop.size());

            // Create ShopOrder for each shop
            for (Map.Entry<Shop, List<CartItemDTO>> entry : itemsByShop.entrySet()) {
                Shop shop = entry.getKey();
                List<CartItemDTO> shopItems = entry.getValue();
                logDebugToFile("Creating ShopOrder for shop: " + shop.getName());

                BigDecimal shopSubtotal = BigDecimal.ZERO;

                ShopOrder shopOrder = new ShopOrder();
                shopOrder.setOrder(saved);
                shopOrder.setShop(shop);
                shopOrder.setStatus(ShopOrder.ShopOrderStatus.PENDING);

                // Create OrderItems
                for (CartItemDTO ci : shopItems) {
                    OrderItem oi = new OrderItem();
                    BigDecimal itemPrice;

                    if (ci.getVariantId() != null) {
                        ProductVariant variant = variantRepository.findById(ci.getVariantId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Variant not found with ID: " + ci.getVariantId()));
                        oi.setProductVariant(variant);
                        oi.setProduct(variant.getProduct());
                        itemPrice = variant.getPrice();
                    } else {
                        Product product = productRepository.findById(ci.getProductId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Product not found with ID: " + ci.getProductId()));
                        oi.setProduct(product);
                        itemPrice = product.getPrice();
                    }

                    oi.setShopOrder(shopOrder);
                    oi.setQuantity(ci.getQuantity());
                    oi.setPrice(itemPrice);
                    shopOrder.getItems().add(oi);

                    shopSubtotal = shopSubtotal.add(itemPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
                }

                // Calculate shop-specific shipping
                BigDecimal shopShippingCost = BigDecimal.ZERO;
                try {
                    com.ecommerce.dto.ShippingDetailsDTO details = shippingCostService.calculateEnhancedShippingDetails(
                            req.getShippingAddress(), shopItems, shopSubtotal, shop.getShopId());
                    shopShippingCost = details.getShippingCost();
                } catch (Exception e) {
                    logDebugToFile("Error calculating shipping for shop " + shop.getShopId() + ": " + e.getMessage());
                }

                shopOrder.setShippingCost(shopShippingCost);
                shopOrder.setTotalAmount(shopSubtotal.add(shopShippingCost));

                // Create ShopOrderTransaction and link to global transaction
                ShopOrderTransaction shopTx = new ShopOrderTransaction();
                shopTx.setShopOrder(shopOrder);
                shopTx.setGlobalTransaction(tx); // Link to main transaction
                shopTx.setAmount(shopOrder.getTotalAmount());
                shopOrder.setShopOrderTransaction(shopTx);

                shopOrderRepository.save(shopOrder);
                logDebugToFile("ShopOrder saved for shop " + shop.getShopId());
            }

            // Create Stripe Session
            logDebugToFile("Creating Stripe Session...");
            String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, req.getCurrency(),
                    req.getPlatform() != null ? req.getPlatform() : "web");
            logDebugToFile("Stripe Session URL created: " + sessionUrl);

            // Retrieve session ID from updated transaction
            OrderTransaction updatedTx = transactionRepository.findById(tx.getOrderTransactionId()).orElseThrow();
            String sessionId = updatedTx.getStripeSessionId();

            // Lock stock
            logDebugToFile("Locking stock...");
            if (!lockStockWithFEFOAllocation(sessionId, req.getItems(), req.getShippingAddress())) {
                logDebugToFile("ERROR: Failed to lock stock");
                throw new IllegalStateException("Unable to secure stock");
            }
            logDebugToFile("Stock locked successfully");

            logDebugToFile("SUCCESS: Checkout session created: " + sessionUrl);
            return sessionUrl;

        } catch (Exception e) {
            logDebugToFile("FATAL ERROR in createCheckoutSession: " + e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                logDebugToFile("\t" + ste.toString());
            }
            throw e;
        }
    }

    // @Transactional
    public String createGuestCheckoutSession(GuestCheckoutRequest req) throws Exception {
        log.info("Creating guest checkout session");

        if (req.getAddress() == null || req.getAddress().getCountry() == null) {
            throw new IllegalArgumentException("Address and country are required");
        }

        // Road validation disabled - Google Maps API key expired
        // if (req.getAddress().getLatitude() != null && req.getAddress().getLongitude()
        // != null) {
        // roadValidationService.validateRoadLocation(
        // req.getAddress().getLatitude(),
        // req.getAddress().getLongitude()
        // );
        // log.info("Road validated successfully");
        // }

        validateCartItems(req.getItems());

        // Allocate stock using FEFO across warehouses
        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> fefoAllocations = enhancedWarehouseAllocator
                .allocateStockWithFEFO(req.getItems(), req.getAddress());

        // Calculate payment summary (validates shops and warehouses)
        com.ecommerce.dto.PaymentSummaryDTO paymentSummary = calculatePaymentSummary(req.getAddress(), req.getItems(),
                null);

        // Create main Order entity
        Order order = new Order();
        order.setStatus("PENDING"); // Set initial status

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

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setSubtotal(paymentSummary.getSubtotal());
        orderInfo.setTotalAmount(paymentSummary.getTotalAmount());
        orderInfo.setTaxAmount(paymentSummary.getTaxAmount());
        orderInfo.setShippingCost(paymentSummary.getShippingCost());
        orderInfo.setDiscountAmount(paymentSummary.getDiscountAmount());
        order.setOrderInfo(orderInfo);

        if (req.getAddress() != null) {
            OrderAddress orderAddress = new OrderAddress();
            orderAddress.setStreet(req.getAddress().getStreetAddress());
            orderAddress.setCountry(req.getAddress().getCountry());
            orderAddress.setRegions(req.getAddress().getCity() + "," + req.getAddress().getState());
            orderAddress.setLatitude(req.getAddress().getLatitude());
            orderAddress.setLongitude(req.getAddress().getLongitude());
            orderAddress.setRoadName(req.getAddress().getStreetAddress());
            order.setOrderAddress(orderAddress);
        }

        // Create mock transaction (COMPLETED immediately)
        String mockSessionId = "mock_guest_session_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        String mockPaymentIntentId = "pi_mock_guest_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8);

        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(paymentSummary.getTotalAmount());
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.COMPLETED); // Mark as completed immediately
        tx.setStripeSessionId(mockSessionId);
        tx.setStripePaymentIntentId(mockPaymentIntentId);
        tx.setPaymentDate(LocalDateTime.now());
        tx.setReceiptUrl("/receipt/" + mockSessionId);
        order.setOrderTransaction(tx);

        // Save order first to get orderId
        Order saved = orderRepository.save(order);
        log.info("Guest order created with ID: {}", saved.getOrderId());

        // Group items by shop and create ShopOrders
        Map<Shop, List<CartItemDTO>> itemsByShop = new HashMap<>();
        for (CartItemDTO item : req.getItems()) {
            Shop shop = getShopForCartItem(item);
            if (shop == null) {
                throw new EntityNotFoundException("Product or variant not found or has no associated shop");
            }
            itemsByShop.computeIfAbsent(shop, k -> new ArrayList<>()).add(item);
        }

        // Create ShopOrder for each shop
        for (Map.Entry<Shop, List<CartItemDTO>> entry : itemsByShop.entrySet()) {
            Shop shop = entry.getKey();
            List<CartItemDTO> shopItems = entry.getValue();

            // Calculate shop-specific totals first
            BigDecimal shopSubtotal = BigDecimal.ZERO;
            BigDecimal shopDiscountAmount = BigDecimal.ZERO;

            // Create ShopOrder
            ShopOrder shopOrder = new ShopOrder();
            shopOrder.setOrder(saved);
            shopOrder.setShop(shop);
            shopOrder.setStatus(ShopOrder.ShopOrderStatus.PROCESSING); // Mark as processing (payment completed)

            // Calculate shop totals and create OrderItems
            for (CartItemDTO ci : shopItems) {
                OrderItem oi = new OrderItem();
                BigDecimal itemPrice;
                BigDecimal originalPrice;

                if (ci.getVariantId() != null) {
                    ProductVariant variant = variantRepository.findById(ci.getVariantId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Variant not found with ID: " + ci.getVariantId()));
                    oi.setProductVariant(variant);
                    originalPrice = variant.getPrice();
                    itemPrice = calculateDiscountedPrice(variant);
                    shopSubtotal = shopSubtotal.add(itemPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
                    shopDiscountAmount = shopDiscountAmount.add(
                            originalPrice.subtract(itemPrice).multiply(BigDecimal.valueOf(ci.getQuantity())));
                } else if (ci.getProductId() != null) {
                    Product product = productRepository.findById(ci.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Product not found with ID: " + ci.getProductId()));
                    oi.setProduct(product);
                    originalPrice = product.getPrice();
                    itemPrice = calculateDiscountedPrice(product);
                    shopSubtotal = shopSubtotal.add(itemPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
                    shopDiscountAmount = shopDiscountAmount.add(
                            originalPrice.subtract(itemPrice).multiply(BigDecimal.valueOf(ci.getQuantity())));
                } else {
                    throw new IllegalArgumentException("Cart item must have either productId or variantId");
                }

                oi.setQuantity(ci.getQuantity());
                oi.setPrice(itemPrice);
                oi.setShopOrder(shopOrder); // Link to ShopOrder, not Order
                shopOrder.getItems().add(oi);
            }

            // Calculate shipping for this shop (after we have shopSubtotal)
            BigDecimal shopShippingCost = BigDecimal.ZERO;
            try {
                com.ecommerce.dto.ShippingDetailsDTO shopShippingDetails = shippingCostService
                        .calculateEnhancedShippingDetails(
                                req.getAddress(), shopItems, shopSubtotal, shop.getShopId());
                shopShippingCost = shopShippingDetails.getShippingCost();
            } catch (Exception e) {
                log.error("Error calculating shipping for shop {}: {}", shop.getShopId(), e.getMessage());
                shopShippingCost = BigDecimal.ZERO;
            }

            shopOrder.setShippingCost(shopShippingCost);
            shopOrder.setDiscountAmount(shopDiscountAmount);
            shopOrder.setTotalAmount(shopSubtotal.add(shopShippingCost).subtract(shopDiscountAmount));

            // Create ShopOrderTransaction
            ShopOrderTransaction shopTx = new ShopOrderTransaction();
            shopTx.setShopOrder(shopOrder);
            shopTx.setGlobalTransaction(tx);
            shopTx.setAmount(shopOrder.getTotalAmount());
            shopOrder.setShopOrderTransaction(shopTx);

            saved.addShopOrder(shopOrder);
            shopOrderRepository.save(shopOrder);

            log.info("Created ShopOrder {} for shop {}", shopOrder.getShopOrderCode(), shop.getName());
        }

        // Create order item batches (non-critical - wrap in try-catch to prevent
        // transaction rollback)
        try {
            for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : fefoAllocations
                    .entrySet()) {
                createOrderItemBatchesForShopOrder(saved, entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.error("Failed to create order item batches for guest order {}: {}", saved.getOrderId(), e.getMessage(),
                    e);
        }

        // Lock stock
        String sessionId = mockSessionId;
        boolean stockLocked = lockStockWithFEFOAllocation(sessionId, req.getItems(), req.getAddress());
        if (!stockLocked) {
            log.error("Failed to lock stock for guest order: {}", saved.getOrderId());
            orderRepository.delete(saved);
            throw new IllegalStateException("Unable to secure stock for your order. Please try again.");
        }

        // Confirm stock locks
        enhancedStockLockService.confirmBatchLocks(sessionId);
        stockLockService.confirmStock(sessionId);

        // Update discount usage (non-critical - wrap in try-catch to prevent
        // transaction rollback)
        try {
            updateDiscountUsage(saved);
        } catch (Exception e) {
            log.error("Failed to update discount usage for guest order {}: {}", saved.getOrderId(), e.getMessage(), e);
        }

        // Send order confirmation email (non-critical)
        try {
            orderEmailService.sendOrderConfirmationEmail(saved);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for guest order {}: {}", saved.getOrderId(),
                    e.getMessage(), e);
        }

        // LOG ACTIVITY: Order Placed and Payment Completed (non-critical - wrap in
        // try-catch)
        String guestCustomerName = req.getGuestName() + " " + req.getGuestLastName();
        try {
            activityLogService.logOrderPlaced(saved.getOrderId(), guestCustomerName + " (Guest)");
        } catch (Exception e) {
            log.error("Failed to log order placed activity for guest order {}: {}", saved.getOrderId(), e.getMessage(),
                    e);
        }

        try {
            activityLogService.logPaymentCompleted(saved.getOrderId(),
                    tx.getPaymentMethod().toString(), tx.getOrderAmount().doubleValue());
        } catch (Exception e) {
            log.error("Failed to log payment completed activity for guest order {}: {}", saved.getOrderId(),
                    e.getMessage(), e);
        }

        // Record payment in money flow (non-critical - already has try-catch)
        recordPaymentInMoneyFlow(saved, tx);

        log.info("Mock guest checkout session created successfully. Order ID: {}, Session ID: {}", saved.getOrderId(),
                mockSessionId);

        // Return success URL instead of Stripe session URL
        return "/payment-success?sessionId=" + mockSessionId + "&orderId=" + saved.getOrderId();
    }

    @Transactional
    public CheckoutVerificationResult verifyCheckoutSession(String sessionId) throws Exception {
        log.info("Payment record found for session: {}", sessionId);
        OrderTransaction tx = transactionRepository.findByStripeSessionIdWithOrder(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("No matching payment record"));

        Session session = stripeService.retrieveSession(sessionId);

        if (session == null) {
            throw new EntityNotFoundException("Session not found on Stripe");
        }

        if (tx.getStatus() == OrderTransaction.TransactionStatus.COMPLETED) {
            log.info("Transaction already completed for session: {}. Skipping duplicate verification.", sessionId);
            Order order = tx.getOrder();
            return buildVerificationResult(session, order, tx);
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
        // Update ShopOrder statuses to PROCESSING (status is managed at ShopOrder
        // level)
        if (order.getShopOrders() != null) {
            for (ShopOrder shopOrder : order.getShopOrders()) {
                shopOrder.setStatus(ShopOrder.ShopOrderStatus.PROCESSING);
            }
        }
        orderRepository.save(order);

        activityLogService.logPaymentCompleted(
                order.getOrderId(),
                tx.getPaymentMethod().toString(),
                tx.getOrderAmount().doubleValue());

        recordPaymentInMoneyFlow(order, tx);

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
            log.info("User is not null");
             logDebugToFile("Proceeding to check rewardable and reward");
            rewardService.checkRewardableOnOrderAndReward(order);
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

    /**
     * Helper method to build verification result for already completed transactions
     */
    private CheckoutVerificationResult buildVerificationResult(Session session, Order order, OrderTransaction tx) {
        OrderResponseDTO orderResponse = OrderResponseDTO.builder()
                .id(order.getOrderId())
                .orderNumber(order.getOrderCode())
                .status(order.getStatus() != null ? order.getStatus() : "PENDING")
                .total(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();

        return new CheckoutVerificationResult(
                session.getPaymentStatus(),
                session.getAmountTotal(),
                session.getCurrency(),
                session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null,
                tx.getReceiptUrl(),
                tx.getStripePaymentIntentId(),
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

            // Fetch all OrderItemBatch entities for this order with warehouses eagerly
            // loaded
            List<OrderItemBatch> orderItemBatches = orderItemBatchRepository
                    .findByOrderIdWithWarehouse(order.getOrderId());

            for (OrderItemBatch orderItemBatch : orderItemBatches) {
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

        if (deliveryAddress == null || deliveryAddress.getCountry() == null
                || deliveryAddress.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery address and country are required");
        }

        // Road validation disabled - Google Maps API key expired
        // Validate that the pickup point is on or near a road
        // if (deliveryAddress.getLatitude() != null && deliveryAddress.getLongitude()
        // != null) {
        // roadValidationService.validateRoadLocation(deliveryAddress.getLatitude(),
        // deliveryAddress.getLongitude());
        // }

        // Step 1: Group items by shop
        Map<Shop, List<CartItemDTO>> itemsByShop = new HashMap<>();
        Map<Shop, ShopCalculationResult> shopCalculations = new HashMap<>();

        for (CartItemDTO item : items) {
            Shop shop = getShopForCartItem(item);
            if (shop == null) {
                throw new EntityNotFoundException("Product or variant not found or has no associated shop");
            }
            itemsByShop.computeIfAbsent(shop, k -> new ArrayList<>()).add(item);
        }

        // Step 2: Validate delivery country for each shop and calculate per-shop totals
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        BigDecimal totalShippingCost = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        Integer totalRewardPoints = 0;
        BigDecimal totalRewardPointsValue = BigDecimal.ZERO;
        int totalProductCount = 0;

        com.ecommerce.dto.ShippingDetailsDTO farthestShippingDetails = null;
        double maxDistance = 0.0;

        for (Map.Entry<Shop, List<CartItemDTO>> entry : itemsByShop.entrySet()) {
            Shop shop = entry.getKey();
            List<CartItemDTO> shopItems = entry.getValue();

            // Validate shop has warehouses in delivery country
            validateShopWarehouseInCountry(shop.getShopId(), deliveryAddress.getCountry());

            // Calculate product costs for this shop
            BigDecimal shopSubtotal = BigDecimal.ZERO;
            BigDecimal shopDiscountAmount = BigDecimal.ZERO;
            int shopProductCount = 0;

            for (CartItemDTO item : shopItems) {
                BigDecimal itemPrice = BigDecimal.ZERO;
                BigDecimal originalPrice = BigDecimal.ZERO;

                if (item.getVariantId() != null) {
                    ProductVariant variant = variantRepository.findById(item.getVariantId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Variant not found with ID: " + item.getVariantId()));
                    originalPrice = variant.getPrice();

                    if (variant.getDiscount() != null) {
                        validateDiscountValidity(variant.getDiscount());
                    }

                    itemPrice = calculateDiscountedPrice(variant);
                } else if (item.getProductId() != null) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Product not found with ID: " + item.getProductId()));
                    originalPrice = product.getPrice();

                    if (product.getDiscount() != null) {
                        validateDiscountValidity(product.getDiscount());
                    }

                    itemPrice = calculateDiscountedPrice(product);
                } else {
                    continue;
                }

                BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                BigDecimal itemDiscount = originalPrice.subtract(itemPrice)
                        .multiply(BigDecimal.valueOf(item.getQuantity()));

                shopSubtotal = shopSubtotal.add(itemTotal);
                shopDiscountAmount = shopDiscountAmount.add(itemDiscount);
                shopProductCount += item.getQuantity();
            }

            // Calculate shipping for this shop
            com.ecommerce.dto.ShippingDetailsDTO shopShippingDetails;
            try {
                shopShippingDetails = shippingCostService.calculateEnhancedShippingDetails(
                        deliveryAddress, shopItems, shopSubtotal, shop.getShopId());
            } catch (Exception e) {
                log.error("Error calculating shipping details for shop {}: {}", shop.getShopId(), e.getMessage(), e);
                shopShippingDetails = com.ecommerce.dto.ShippingDetailsDTO.builder()
                        .shippingCost(BigDecimal.valueOf(10.00))
                        .distanceKm(0.0)
                        .costPerKm(BigDecimal.ZERO)
                        .selectedWarehouseName("Default")
                        .selectedWarehouseCountry("Unknown")
                        .isInternationalShipping(false)
                        .build();
            }

            BigDecimal shopShippingCost = shopShippingDetails.getShippingCost();

            // Calculate reward points for this shop (if user is logged in)
            Integer shopRewardPoints = 0;
            BigDecimal shopRewardPointsValue = BigDecimal.ZERO;
            if (userId != null) {
                try {
                    com.ecommerce.dto.RewardSystemDTO rewardSystem = rewardService
                            .getActiveRewardSystem(shop.getShopId());
                    if (rewardSystem != null && rewardSystem.getIsSystemEnabled()
                            && rewardSystem.getIsPurchasePointsEnabled()) {
                        // Get the reward system entity to calculate points
                        com.ecommerce.entity.RewardSystem rewardSystemEntity = rewardSystemRepository
                                .findByShopShopIdAndIsActiveTrue(shop.getShopId())
                                .orElse(null);
                        if (rewardSystemEntity != null) {
                            shopRewardPoints = rewardSystemEntity.calculatePurchasePoints(shopProductCount,
                                    shopSubtotal);
                            shopRewardPointsValue = rewardSystemEntity.calculatePointsValue(shopRewardPoints);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error calculating reward points for shop {} and user {}: {}",
                            shop.getShopId(), userId, e.getMessage());
                }
            }

            // Store shop calculation result
            ShopCalculationResult shopResult = new ShopCalculationResult();
            shopResult.shop = shop;
            shopResult.subtotal = shopSubtotal;
            shopResult.discountAmount = shopDiscountAmount;
            shopResult.shippingCost = shopShippingCost;
            shopResult.shippingDetails = shopShippingDetails;
            shopResult.rewardPoints = shopRewardPoints;
            shopResult.rewardPointsValue = shopRewardPointsValue;
            shopResult.productCount = shopProductCount;
            shopCalculations.put(shop, shopResult);

            // Aggregate totals
            totalSubtotal = totalSubtotal.add(shopSubtotal);
            totalDiscountAmount = totalDiscountAmount.add(shopDiscountAmount);
            totalShippingCost = totalShippingCost.add(shopShippingCost);
            totalRewardPoints += shopRewardPoints;
            totalRewardPointsValue = totalRewardPointsValue.add(shopRewardPointsValue);
            totalProductCount += shopProductCount;

            // Track farthest warehouse (for display purposes)
            if (shopShippingDetails.getDistanceKm() != null && shopShippingDetails.getDistanceKm() > maxDistance) {
                maxDistance = shopShippingDetails.getDistanceKm();
                farthestShippingDetails = shopShippingDetails;
            }
        }

        // Use farthest shipping details or default
        if (farthestShippingDetails == null) {
            farthestShippingDetails = com.ecommerce.dto.ShippingDetailsDTO.builder()
                    .shippingCost(totalShippingCost)
                    .distanceKm(0.0)
                    .costPerKm(BigDecimal.ZERO)
                    .selectedWarehouseName("Multiple Warehouses")
                    .selectedWarehouseCountry(deliveryAddress.getCountry())
                    .isInternationalShipping(false)
                    .build();
        }

        BigDecimal totalAmount = totalSubtotal.add(totalShippingCost).add(totalTaxAmount);

        // Build shop summaries
        List<com.ecommerce.dto.PaymentSummaryDTO.ShopSummary> shopSummaries = shopCalculations.values().stream()
                .map(shopResult -> {
                    BigDecimal shopTotal = shopResult.subtotal
                            .add(shopResult.shippingCost)
                            .subtract(shopResult.discountAmount);

                    return com.ecommerce.dto.PaymentSummaryDTO.ShopSummary.builder()
                            .shopId(shopResult.shop.getShopId().toString())
                            .shopName(shopResult.shop.getName())
                            .subtotal(shopResult.subtotal)
                            .discountAmount(shopResult.discountAmount)
                            .shippingCost(shopResult.shippingCost)
                            .taxAmount(BigDecimal.ZERO) // Tax is typically calculated at order level
                            .totalAmount(shopTotal)
                            .rewardPoints(shopResult.rewardPoints)
                            .rewardPointsValue(shopResult.rewardPointsValue)
                            .productCount(shopResult.productCount)
                            .distanceKm(shopResult.shippingDetails.getDistanceKm())
                            .costPerKm(shopResult.shippingDetails.getCostPerKm())
                            .selectedWarehouseName(shopResult.shippingDetails.getSelectedWarehouseName())
                            .selectedWarehouseCountry(shopResult.shippingDetails.getSelectedWarehouseCountry())
                            .isInternationalShipping(shopResult.shippingDetails.getIsInternationalShipping())
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());

        log.info("Payment summary calculated: {} shops, {} items, total: {}",
                itemsByShop.size(), totalProductCount, totalAmount);

        return com.ecommerce.dto.PaymentSummaryDTO.builder()
                .subtotal(totalSubtotal)
                .discountAmount(totalDiscountAmount)
                .shippingCost(totalShippingCost)
                .taxAmount(totalTaxAmount)
                .totalAmount(totalAmount)
                .rewardPoints(totalRewardPoints)
                .rewardPointsValue(totalRewardPointsValue)
                .currency("USD")
                .distanceKm(farthestShippingDetails.getDistanceKm())
                .costPerKm(farthestShippingDetails.getCostPerKm())
                .selectedWarehouseName(farthestShippingDetails.getSelectedWarehouseName())
                .selectedWarehouseCountry(farthestShippingDetails.getSelectedWarehouseCountry())
                .isInternationalShipping(farthestShippingDetails.getIsInternationalShipping())
                .shopSummaries(shopSummaries)
                .build();
    }

    /**
     * Helper class to store per-shop calculation results
     */
    private static class ShopCalculationResult {
        Shop shop;
        BigDecimal subtotal;
        BigDecimal discountAmount;
        BigDecimal shippingCost;
        com.ecommerce.dto.ShippingDetailsDTO shippingDetails;
        Integer rewardPoints;
        BigDecimal rewardPointsValue;
        int productCount;
    }

    /**
     * Get the shop associated with a cart item
     */
    private Shop getShopForCartItem(CartItemDTO item) {
        if (item.getVariantId() != null) {
            ProductVariant variant = variantRepository.findById(item.getVariantId()).orElse(null);
            if (variant != null && variant.getProduct() != null) {
                return variant.getProduct().getShop();
            }
        } else if (item.getProductId() != null) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                return product.getShop();
            }
        }
        return null;
    }

    /**
     * Validate that a shop has warehouses in the delivery country
     */
    private void validateShopWarehouseInCountry(UUID shopId, String country) {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery country is required");
        }

        List<Warehouse> shopWarehouses = warehouseRepository.findByShopShopIdAndIsActiveTrue(shopId);
        if (shopWarehouses == null || shopWarehouses.isEmpty()) {
            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new EntityNotFoundException("Shop not found with ID: " + shopId));
            throw new IllegalArgumentException(
                    String.format("Shop '%s' does not have any active warehouses.", shop.getName()));
        }

        boolean hasWarehouseInCountry = shopWarehouses.stream()
                .anyMatch(w -> w.getCountry() != null &&
                        w.getCountry().equalsIgnoreCase(country.trim()));

        if (!hasWarehouseInCountry) {
            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new EntityNotFoundException("Shop not found with ID: " + shopId));
            throw new IllegalArgumentException(
                    String.format("Shop '%s' does not deliver to %s as it doesn't have any warehouses there.",
                            shop.getName(), country));
        }
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

        for (OrderItem item : order.getAllItems()) {
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

    /**
     * Validate if we have at least one warehouse in the delivery country
     */
    private void validateDeliveryCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery country is required");
        }

        boolean hasWarehouseInCountry = warehouseRepository.existsByCountryIgnoreCase(country.trim());

        if (!hasWarehouseInCountry) {
            throw new IllegalArgumentException(
                    "Sorry, we don't deliver to " + country + " as we don't have any warehouses there.");
        }

    }

    private void createOrderItemBatches(Order order, CartItemDTO cartItem,
            List<FEFOStockAllocationService.BatchAllocation> allocations) {

        OrderItem orderItem = order.getAllItems().stream()
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

    /**
     * Create order item batches for ShopOrder structure
     */
    private void createOrderItemBatchesForShopOrder(Order order, CartItemDTO cartItem,
            List<FEFOStockAllocationService.BatchAllocation> allocations) {

        // Find the OrderItem across all ShopOrders
        OrderItem orderItem = order.getAllItems().stream()
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
        log.info("The order addressses obtained are " + addr);
        OrderTransaction tx = order.getOrderTransaction();

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getOrderId());
        dto.setUserId(
                order.getUser() != null && order.getUser().getId() != null ? order.getUser().getId().toString() : null);
        dto.setOrderNumber(order.getOrderCode());
        // Pickup token is now on ShopOrder, get from first shop order if available
        if (order.getShopOrders() != null && !order.getShopOrders().isEmpty()) {
            com.ecommerce.entity.ShopOrder firstShopOrder = order.getShopOrders().iterator().next();
            dto.setPickupToken(firstShopOrder.getPickupToken());
            dto.setPickupTokenUsed(firstShopOrder.getPickupTokenUsed());
        } else {
            dto.setPickupToken(null);
            dto.setPickupTokenUsed(false);
        }
        dto.setStatus(order.getStatus() != null ? order.getStatus() : null);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Set customer information from OrderCustomerInfo entity
        if (order.getOrderCustomerInfo() != null) {
            OrderResponseDTO.CustomerInfo customerInfo = new OrderResponseDTO.CustomerInfo();
            customerInfo.setName(
                    order.getOrderCustomerInfo().getFirstName() + " " + order.getOrderCustomerInfo().getLastName());
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
            shippingAddress.setLatitude(addr.getLatitude());
            shippingAddress.setLongitude(addr.getLongitude());

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
            transactionDTO.setOrderTransactionId(
                    tx.getOrderTransactionId() != null ? tx.getOrderTransactionId().toString() : null);
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
        if (order.getAllItems() != null && !order.getAllItems().isEmpty()) {
            List<OrderResponseDTO.OrderItem> itemDTOs = order.getAllItems().stream()
                    .map(this::mapOrderItemToResponseDTO).toList();
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

            // Add product images if available
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                List<String> imageUrls = item.getProduct().getImages().stream()
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
        if (item.getProductVariant() != null) {
            OrderResponseDTO.Variant variant = new OrderResponseDTO.Variant();
            variant.setId(item.getProductVariant().getId());
            variant.setName(item.getProductVariant().getVariantName());

            // Add variant images if available
            if (item.getProductVariant().getImages() != null && !item.getProductVariant().getImages().isEmpty()) {
                List<String> variantImageUrls = item.getProductVariant().getImages().stream()
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

    /**
     * Record payment in money flow system
     */
    private void recordPaymentInMoneyFlow(Order order, OrderTransaction transaction) {
        try {
            BigDecimal paymentAmount = transaction.getOrderAmount();

            // For hybrid payments, only record the actual money paid (not points)
            if (transaction.getPaymentMethod() == OrderTransaction.PaymentMethod.HYBRID) {
                BigDecimal pointsValue = transaction.getPointsValue() != null ? transaction.getPointsValue()
                        : BigDecimal.ZERO;
                paymentAmount = paymentAmount.subtract(pointsValue);
            }

            // Only record if there's actual money involved
            if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
                String description = String.format("Payment received for Order #%s (%s)",
                        order.getOrderCode() != null ? order.getOrderCode() : order.getOrderId().toString(),
                        transaction.getPaymentMethod().name());

                com.ecommerce.dto.CreateMoneyFlowDTO moneyFlowDTO = new com.ecommerce.dto.CreateMoneyFlowDTO();
                moneyFlowDTO.setDescription(description);
                moneyFlowDTO.setType(com.ecommerce.enums.MoneyFlowType.IN);
                moneyFlowDTO.setAmount(paymentAmount);

                moneyFlowService.save(moneyFlowDTO);
                log.info("Recorded money flow IN: {} for order {}", paymentAmount, order.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to record money flow for order {}: {}", order.getOrderId(), e.getMessage(), e);
        }
    }
}
