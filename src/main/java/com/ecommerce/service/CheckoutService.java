package com.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CheckoutRequest;
import com.ecommerce.dto.CheckoutVerificationResult;
import com.ecommerce.dto.GuestCheckoutRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionRetrieveParams;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    @Transactional
    public String createCheckoutSession(CheckoutRequest req) throws Exception {
        log.info("Creating checkout session for user");

        // Validate user exists
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + req.getUserId()));

        // 1. build order
        Order order = new Order();
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setUser(user);

        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(user.getFirstName());
        customerInfo.setLastName(user.getLastName());
        customerInfo.setEmail(user.getUserEmail());
        customerInfo.setPhoneNumber(user.getPhoneNumber());
        // Map address from req.getShippingAddress() if present
        if (req.getShippingAddress() != null) {
            customerInfo.setStreetAddress(req.getShippingAddress().getStreetAddress());
            customerInfo.setCity(req.getShippingAddress().getCity());
            customerInfo.setState(req.getShippingAddress().getState());
            customerInfo.setPostalCode(req.getShippingAddress().getPostalCode());
            customerInfo.setCountry(req.getShippingAddress().getCountry());
        }
        order.setOrderCustomerInfo(customerInfo);
        customerInfo.setOrder(order);

        // add order items
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO ci : req.getItems()) {
            ProductVariant variant = variantRepository.findById(ci.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

            // Check stock availability
            if (variant.getStockQuantity() < ci.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for variant " + variant.getId());
            }

            OrderItem oi = new OrderItem();
            oi.setProductVariant(variant);
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getPrice());
            oi.setOrder(order);
            order.getOrderItems().add(oi);
            total = total.add(ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // create OrderTransaction
        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(total);
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        // link both ways
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        // persist order (cascade will save items/customer/transaction)
        Order saved = orderRepository.save(order);
        log.info("Order created with ID: {}", saved.getOrderId());

        // 2. create stripe session
        String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, req.getCurrency(), req.getPlatform());
        log.info("Stripe session created successfully");

        return sessionUrl;
    }

    @Transactional
    public String createGuestCheckoutSession(GuestCheckoutRequest req) throws Exception {
        log.info("Creating guest checkout session");

        // 1. build order
        Order order = new Order();
        order.setOrderStatus(Order.OrderStatus.PENDING);
        // order.user remains null for guest
        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(req.getGuestName());
        customerInfo.setLastName(req.getGuestLastName());
        customerInfo.setEmail(req.getGuestEmail());
        customerInfo.setPhoneNumber(req.getGuestPhone());
        // Map address from req.getAddress()
        if (req.getAddress() != null) {
            customerInfo.setStreetAddress(req.getAddress().getStreetAddress());
            customerInfo.setCity(req.getAddress().getCity());
            customerInfo.setState(req.getAddress().getState());
            customerInfo.setPostalCode(req.getAddress().getPostalCode());
            customerInfo.setCountry(req.getAddress().getCountry());
        }
        order.setOrderCustomerInfo(customerInfo);
        customerInfo.setOrder(order);

        // add order items
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO ci : req.getItems()) {
            ProductVariant variant = variantRepository.findById(ci.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

            // Check stock availability
            if (variant.getStockQuantity() < ci.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for variant " + variant.getId());
            }

            OrderItem oi = new OrderItem();
            oi.setProductVariant(variant);
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getPrice());
            oi.setOrder(order);
            order.getOrderItems().add(oi);
            total = total.add(ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // create OrderTransaction
        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(total);
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        // link both ways
        order.setOrderTransaction(tx);
        tx.setOrder(order);

        // persist order (cascade will save items/customer/transaction)
        Order saved = orderRepository.save(order);
        log.info("Guest order created with ID: {}", saved.getOrderId());

        // 2. create stripe session
        String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, "usd", req.getPlatform());
        log.info("Guest Stripe session created successfully");

        return sessionUrl;
    }

    @Transactional
    public CheckoutVerificationResult verifyCheckoutSession(String sessionId) throws Exception {
        log.info("Verifying checkout session: {}", sessionId);

        // Lookup transaction in DB
        OrderTransaction tx = transactionRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("No matching payment record"));

        // Retrieve Stripe session
        Session session = stripeService.retrieveSession(sessionId);

        if (session == null) {
            throw new EntityNotFoundException("Session not found on Stripe");
        }

        // Check payment status
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException("Payment not completed or session expired");
        }

        // Extract receipt URL & intent details
        PaymentIntent intent = (PaymentIntent) session.getPaymentIntentObject();
        String receiptUrl = session.getPaymentStatus().equals("paid") ? session.getUrl() : null;

        // Update transaction & order
        tx.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
        tx.setPaymentDate(LocalDateTime.now());
        if (intent != null) {
            tx.setStripePaymentIntentId(intent.getId());
        }
        tx.setReceiptUrl(receiptUrl);
        transactionRepository.save(tx);
        log.info("Transaction updated to completed status");

        // Reduce inventory
        Order order = tx.getOrder();
        order.setOrderStatus(Order.OrderStatus.PROCESSING);
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = variantRepository.findByIdForUpdate(item.getProductVariant().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
            int stock = variant.getStockQuantity();
            if (stock < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for variant " + variant.getId());
            }
            variant.setStockQuantity(stock - item.getQuantity());
            variantRepository.save(variant);
            log.info("Stock reduced for variant {}: {} -> {}", variant.getId(), stock, variant.getStockQuantity());
        }
        orderRepository.save(order);
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
}
