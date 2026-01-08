package com.ecommerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.service.AbandonedOrderCleanupService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhook")
@Slf4j
public class StripeWebhookController {

    private final OrderRepository orderRepository;
    private final OrderTransactionRepository txRepository;
    private final ProductVariantRepository variantRepository;
    private final AbandonedOrderCleanupService abandonedOrderCleanupService;
    private final org.springframework.core.env.Environment environment;

    public StripeWebhookController(OrderRepository orderRepository,
            OrderTransactionRepository txRepository,
            ProductVariantRepository variantRepository,
            AbandonedOrderCleanupService abandonedOrderCleanupService,
            org.springframework.core.env.Environment environment) {
        this.orderRepository = orderRepository;
        this.txRepository = txRepository;
        this.variantRepository = variantRepository;
        this.abandonedOrderCleanupService = abandonedOrderCleanupService;
        this.environment = environment;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeEvent(@RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload) {
        Event event = null;
        try {
            String endpointSecret = environment.getProperty("stripe.webhook.secret");
            if (endpointSecret == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret not configured");
            }
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (session == null)
                return ResponseEntity.ok("no session");

            String sessionId = session.getId();

            // idempotency: check existing transaction status
            txRepository.findByStripeSessionId(sessionId).ifPresent(tx -> {
                if (tx.getStatus() == OrderTransaction.TransactionStatus.COMPLETED) {
                    return; // already processed
                }

                processSuccessfulPayment(tx, session);
            });
        } else if ("checkout.session.expired".equals(event.getType())) {
            // Handle payment cancellation/expiration immediately
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (session == null)
                return ResponseEntity.ok("no session");

            String sessionId = session.getId();

            // Find and cleanup the abandoned order immediately
            txRepository.findByStripeSessionId(sessionId).ifPresent(tx -> {
                if (tx.getStatus() == OrderTransaction.TransactionStatus.COMPLETED) {
                    return;
                }

                processCancelledPayment(tx, session);
            });
        }

        return ResponseEntity.ok("received");
    }

    @Transactional
    protected void processSuccessfulPayment(OrderTransaction tx, Session session) {
        // 1. mark transaction completed
        tx.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
        tx.setStripePaymentIntentId(session.getPaymentIntent());
        tx.setReceiptUrl(getReceiptUrlFromSession(session)); // optional
        txRepository.save(tx);

        // 2. update order status - update all shop orders to PROCESSING
        Order order = tx.getOrder();
        if (order.getShopOrders() != null) {
            for (com.ecommerce.entity.ShopOrder shopOrder : order.getShopOrders()) {
                shopOrder.setStatus(com.ecommerce.entity.ShopOrder.ShopOrderStatus.PROCESSING);
            }
        }
        orderRepository.save(order);

        // 3. reduce inventory for each OrderItem (pessimistic lock)
        for (OrderItem item : order.getAllItems()) {
            ProductVariant variant = variantRepository.findByIdForUpdate(item.getProductVariant().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

            int currentStock = variant.getTotalStockQuantity();
            if (currentStock < item.getQuantity()) {
                // optionally raise alert and mark order as CANCELLED or BACKORDER
                throw new IllegalStateException("Insufficient stock for variant " + variant.getId());
            }

            // TODO: Implement proper stock reduction through Stock entities
            // variant.setStockQuantity(currentStock - item.getQuantity());
            // Optionally set low stock flags
            variantRepository.save(variant);
        }

        // 4. send confirmation email to orderCustomerInfo.email (not shown)
    }

    @Transactional
    protected void processCancelledPayment(OrderTransaction tx, Session session) {
        try {
            Order order = tx.getOrder();
            
            // Log the cancellation
            log.info("Processing cancelled payment for order: {}, session: {}", 
                    order.getOrderId(), session.getId());
            abandonedOrderCleanupService.cleanupSingleAbandonedOrder(order);
            
            log.info("Successfully processed cancellation for order: {}", order.getOrderId());
            
        } catch (Exception e) {
            log.error("Error processing cancelled payment for session {}: {}", 
                     session.getId(), e.getMessage(), e);
        }
    }

    private String getReceiptUrlFromSession(Session session) {
        // try to get charge receipt URL from expanded payment_intent if available
        // Implementation depends on whether you expand payment_intent on session
        // creation
        return null;
    }
}
