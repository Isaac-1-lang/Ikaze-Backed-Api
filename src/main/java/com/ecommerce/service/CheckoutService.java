package com.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CheckoutRequest;
import com.ecommerce.dto.CheckoutVerificationResult;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionRetrieveParams;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderTransactionRepository transactionRepository;
    private final StripeService stripeService; // wrapper for Stripe SDK calls

    @Transactional
    public String createGuestCheckoutSession(CheckoutRequest req) throws Exception {
        // 1. build order
        Order order = new Order();
        order.setOrderStatus(Order.OrderStatus.PENDING);
        // order.user remains null for guest
        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(req.getFirstName());
        customerInfo.setLastName(req.getLastName());
        customerInfo.setEmail(req.getEmail());
        customerInfo.setPhoneNumber(req.getPhoneNumber());
        order.setOrderCustomerInfo(customerInfo);
        customerInfo.setOrder(order);

        // add order items
        BigDecimal total = BigDecimal.ZERO;
        for (CartDTO ci : req.getItems()) {
            ProductVariant variant = variantRepository.findById(ci.getProductVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
            OrderItem oi = new OrderItem();
            oi.setProductVariant(variant);
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getUnitPrice());
            oi.setOrder(order);
            order.getOrderItems().add(oi);
            total = total.add(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
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

        // 2. create stripe session
        String sessionUrl = stripeService.createCheckoutSessionForOrder(saved, req.getCurrency());

        // stripeService should update tx.stripeSessionId and save
        return sessionUrl;
    }

    public CheckoutVerificationResult verifyCheckoutSession(String sessionId) throws Exception {
        com.stripe.Stripe.apiKey = "sk_test_..."; // load from config

        // Retrieve Stripe session with expanded payment intent
        Session session = Session.retrieve(sessionId);

        if (session == null) {
            throw new EntityNotFoundException("Session not found on Stripe");
        }

        // Lookup transaction in DB
        OrderTransaction tx = transactionRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("No matching payment record"));

        // Check payment status
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException("Payment not completed or session expired");
        }

        // Extract receipt URL & intent details
        PaymentIntent intent = (PaymentIntent) session.getPaymentIntentObject();
        String receiptUrl = null;
        // Simplified receipt URL extraction - you may need to implement this
        // differently
        // based on your Stripe configuration

        // Update transaction & order
        tx.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
        // Note: You may need to add these methods to OrderTransaction entity
        // tx.setPaymentDate(LocalDateTime.now());
        // tx.setStripePaymentIntentId(intent != null ? intent.getId() : null);
        // tx.setReceiptUrl(receiptUrl);
        transactionRepository.save(tx);

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
        }
        orderRepository.save(order);

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
