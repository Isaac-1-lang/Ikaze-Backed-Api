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
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/webhook")
public class StripeWebhookController {

    private final OrderRepository orderRepository;
    private final OrderTransactionRepository txRepository;
    private final ProductVariantRepository variantRepository;
    private final org.springframework.core.env.Environment environment;

    public StripeWebhookController(OrderRepository orderRepository,
            OrderTransactionRepository txRepository,
            ProductVariantRepository variantRepository,
            org.springframework.core.env.Environment environment) {
        this.orderRepository = orderRepository;
        this.txRepository = txRepository;
        this.variantRepository = variantRepository;
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

                // Process payment inside transaction
                processSuccessfulPayment(tx, session);
            });
        }
        // handle other events: payment_failed, charge.refunded, etc.

        return ResponseEntity.ok("received");
    }

    @Transactional
    protected void processSuccessfulPayment(OrderTransaction tx, Session session) {
        // 1. mark transaction completed
        tx.setStatus(OrderTransaction.TransactionStatus.COMPLETED);
        tx.setStripePaymentIntentId(session.getPaymentIntent());
        tx.setReceiptUrl(getReceiptUrlFromSession(session)); // optional
        txRepository.save(tx);

        // 2. update order status
        Order order = tx.getOrder();
        order.setOrderStatus(Order.OrderStatus.PROCESSING);
        orderRepository.save(order);

        // 3. reduce inventory for each OrderItem (pessimistic lock)
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = variantRepository.findByIdForUpdate(item.getProductVariant().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

            int currentStock = variant.getStockQuantity();
            if (currentStock < item.getQuantity()) {
                // optionally raise alert and mark order as CANCELLED or BACKORDER
                throw new IllegalStateException("Insufficient stock for variant " + variant.getId());
            }

            variant.setStockQuantity(currentStock - item.getQuantity());
            // Optionally set low stock flags
            variantRepository.save(variant);
        }

        // 4. send confirmation email to orderCustomerInfo.email (not shown)
    }

    private String getReceiptUrlFromSession(Session session) {
        // try to get charge receipt URL from expanded payment_intent if available
        // Implementation depends on whether you expand payment_intent on session
        // creation
        return null;
    }
}
