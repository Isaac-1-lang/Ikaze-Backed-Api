package com.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

        private final OrderRepository orderRepository;
        private final OrderTransactionRepository txRepo;
        private final org.springframework.core.env.Environment environment;

        @PostConstruct
        public void init() {
                String stripeSecret = environment.getProperty("stripe.secret.key");
                if (stripeSecret != null) {
                        com.stripe.Stripe.apiKey = stripeSecret;
                        log.info("Stripe API key initialized");
                } else {
                        log.warn("Stripe secret key not found in configuration");
                }
        }

        @Transactional
        public String createCheckoutSessionForOrder(Order order, String currency, String platform)
                        throws StripeException, JsonProcessingException {
                log.info("Creating Stripe checkout session for order: {}", order.getOrderId());

                // Reload the order with eager fetching to avoid lazy loading issues
                Order reloadedOrder = orderRepository.findById(order.getOrderId())
                        .orElseThrow(() -> new RuntimeException("Order not found: " + order.getOrderId()));

                List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
                for (OrderItem item : reloadedOrder.getAllItems()) {
                        long unitAmount = item.getPrice().multiply(BigDecimal.valueOf(100)).longValue();
                        Product product = item.getEffectiveProduct();
                        SessionCreateParams.LineItem.PriceData.ProductData productData = SessionCreateParams.LineItem.PriceData.ProductData
                                        .builder()
                                        .setName(product.getProductName())
                                        .build();

                        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData
                                        .builder()
                                        .setCurrency(currency)
                                        .setUnitAmount(unitAmount)
                                        .setProductData(productData)
                                        .build();

                        SessionCreateParams.LineItem li = SessionCreateParams.LineItem.builder()
                                        .setPriceData(priceData)
                                        .setQuantity(Long.valueOf(item.getQuantity()))
                                        .build();
                        lineItems.add(li);
                }

                // Add shipping cost as a separate line item if it exists
                if (reloadedOrder.getOrderInfo() != null && reloadedOrder.getOrderInfo().getShippingCost() != null
                                && reloadedOrder.getOrderInfo().getShippingCost().compareTo(BigDecimal.ZERO) > 0) {

                        long shippingAmount = reloadedOrder.getOrderInfo().getShippingCost().multiply(BigDecimal.valueOf(100))
                                        .longValue();

                        SessionCreateParams.LineItem.PriceData.ProductData shippingProductData = SessionCreateParams.LineItem.PriceData.ProductData
                                        .builder()
                                        .setName("Shipping Cost")
                                        .build();

                        SessionCreateParams.LineItem.PriceData shippingPriceData = SessionCreateParams.LineItem.PriceData
                                        .builder()
                                        .setCurrency(currency)
                                        .setUnitAmount(shippingAmount)
                                        .setProductData(shippingProductData)
                                        .build();

                        SessionCreateParams.LineItem shippingItem = SessionCreateParams.LineItem.builder()
                                        .setPriceData(shippingPriceData)
                                        .setQuantity(1L)
                                        .build();
                        lineItems.add(shippingItem);

                        log.info("Added shipping cost to Stripe session: {}", reloadedOrder.getOrderInfo().getShippingCost());
                }

                Map<String, String> metadata = Map.of(
                                "orderId", reloadedOrder.getOrderId().toString(),
                                "orderCode", reloadedOrder.getOrderCode());

                String webSuccess = "https://shopsphere-frontend.vercel.app/payment-success";
                String webCancel = "https://shopsphere-frontend.vercel.app/payment-cancel";
                String mobSuccess = "snapshop://checkout-redirect";
                String mobCancel = "snapshop://checkout-redirect";
                String successUrl;
                String cancelUrl;
                if (platform != null && platform.equalsIgnoreCase("mobile")) {
                        successUrl = mobSuccess;
                        cancelUrl = mobCancel;
                } else {
                        successUrl = webSuccess;
                        cancelUrl = webCancel;
                }
                SessionCreateParams params = SessionCreateParams.builder()
                                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .addAllLineItem(lineItems)
                                .putAllMetadata(metadata)
                                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                                .setCancelUrl(cancelUrl + "?session_id={CHECKOUT_SESSION_ID}")
                                .build();

                Session session = Session.create(params);
                log.info("Stripe session created with ID: {}", session.getId());

                // update transaction with stripe session id
                OrderTransaction tx = reloadedOrder.getOrderTransaction();
                tx.setStripeSessionId(session.getId());
                txRepo.save(tx);
                log.info("Transaction updated with Stripe session ID");

                return session.getUrl();
        }

        @Transactional
        public String createCheckoutSessionForHybridPayment(Order order, String currency, String platform, BigDecimal reducedAmount)
                        throws StripeException, JsonProcessingException {
                log.info("Creating Stripe checkout session for HYBRID payment - order: {}, reduced amount: {}", 
                        order.getOrderId(), reducedAmount);

                // Reload the order with eager fetching to avoid lazy loading issues
                Order reloadedOrder = orderRepository.findById(order.getOrderId())
                        .orElseThrow(() -> new RuntimeException("Order not found: " + order.getOrderId()));

                // For hybrid payments, create a single line item with the reduced amount
                long reducedAmountCents = reducedAmount.multiply(BigDecimal.valueOf(100)).longValue();
                
                SessionCreateParams.LineItem.PriceData.ProductData productData = SessionCreateParams.LineItem.PriceData.ProductData
                                .builder()
                                .setName("Order #" + reloadedOrder.getOrderCode() + " (After Points Discount)")
                                .setDescription("Remaining amount after points redemption")
                                .build();

                SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData
                                .builder()
                                .setCurrency(currency)
                                .setUnitAmount(reducedAmountCents)
                                .setProductData(productData)
                                .build();

                SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                                .setPriceData(priceData)
                                .setQuantity(1L)
                                .build();

                List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
                lineItems.add(lineItem);

                Map<String, String> metadata = Map.of(
                                "orderId", reloadedOrder.getOrderId().toString(),
                                "orderCode", reloadedOrder.getOrderCode(),
                                "paymentType", "hybrid",
                                "pointsUsed", reloadedOrder.getOrderTransaction().getPointsUsed().toString(),
                                "pointsValue", reloadedOrder.getOrderTransaction().getPointsValue().toString());

                String webSuccess = "https://shopsphere-frontend.vercel.app/payment-success";
                String webCancel = "https://shopsphere-frontend.vercel.app/payment-cancel";
                String mobSuccess = "snapshop://checkout-redirect";
                String mobCancel = "snapshop://checkout-redirect";
                String successUrl;
                String cancelUrl;
                if (platform != null && platform.equalsIgnoreCase("mobile")) {
                        successUrl = mobSuccess;
                        cancelUrl = mobCancel;
                } else {
                        successUrl = webSuccess;
                        cancelUrl = webCancel;
                }

                SessionCreateParams params = SessionCreateParams.builder()
                                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .addAllLineItem(lineItems)
                                .putAllMetadata(metadata)
                                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                                .setCancelUrl(cancelUrl + "?session_id={CHECKOUT_SESSION_ID}")
                                .build();

                Session session = Session.create(params);
                log.info("Stripe HYBRID session created with ID: {}, amount: {}", session.getId(), reducedAmount);

                OrderTransaction tx = reloadedOrder.getOrderTransaction();
                tx.setStripeSessionId(session.getId());
                if (session.getPaymentIntent() != null) {
                        tx.setStripePaymentIntentId(session.getPaymentIntent());
                        log.info("Payment intent ID set: {}", session.getPaymentIntent());
                }
                txRepo.save(tx);
                log.info("Transaction updated with Stripe session ID and payment intent ID for hybrid payment");

                return session.getUrl();
        }

        @Transactional(readOnly = true)
        public Session retrieveSession(String sessionId) throws StripeException {
                log.info("Retrieving Stripe session: {}", sessionId);

                SessionRetrieveParams params = SessionRetrieveParams.builder()
                                .addExpand("payment_intent")
                                .build();

                Session session = Session.retrieve(sessionId, params, null);
                log.info("Stripe session retrieved successfully: {}", sessionId);

                return session;
        }

        /**
         * Process refund via Stripe
         * @param paymentIntentId The Stripe payment intent ID
         * @param refundAmount The amount to refund
         * @return The Stripe Refund object
         * @throws StripeException if refund fails
         */
        @Transactional
        public Refund processRefund(String paymentIntentId, BigDecimal refundAmount) throws StripeException {
                log.info("Processing Stripe refund for payment intent: {}, amount: {}", paymentIntentId, refundAmount);

                long amountInCents = refundAmount.multiply(BigDecimal.valueOf(100)).longValue();

                RefundCreateParams params = RefundCreateParams.builder()
                                .setPaymentIntent(paymentIntentId)
                                .setAmount(amountInCents)
                                .build();

                Refund refund = Refund.create(params);
                
                log.info("Stripe refund created successfully. Refund ID: {}, Status: {}, Amount: {}", 
                                refund.getId(), refund.getStatus(), refund.getAmount());

                return refund;
        }
}
