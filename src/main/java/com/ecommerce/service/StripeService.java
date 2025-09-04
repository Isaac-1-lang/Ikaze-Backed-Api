package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.OrderTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        public String createCheckoutSessionForOrder(Order order, String currency, String platform)
                        throws StripeException, JsonProcessingException {
                log.info("Creating Stripe checkout session for order: {}", order.getOrderId());

                List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
                for (OrderItem item : order.getOrderItems()) {
                        long unitAmount = item.getPrice().multiply(BigDecimal.valueOf(100)).longValue();
                        SessionCreateParams.LineItem.PriceData.ProductData productData = SessionCreateParams.LineItem.PriceData.ProductData
                                        .builder()
                                        .setName(item.getProductVariant().getProduct().getProductName())
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

                // pass orderId in metadata for later lookup
                Map<String, String> metadata = Map.of(
                                "orderId", order.getOrderId().toString(),
                                "orderCode", order.getOrderCode());

                SessionCreateParams params = SessionCreateParams.builder()
                                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .addAllLineItem(lineItems)
                                .putAllMetadata(metadata)
                                .setSuccessUrl("http://localhost:5500/stripeCheckoutPayment/payment-success.html?session_id={CHECKOUT_SESSION_ID}")
                                .setCancelUrl("http://localhost:5500/stripeCheckoutPayment/payment-cancel.html?session_id={CHECKOUT_SESSION_ID}")
                                .build();

                Session session = Session.create(params);
                log.info("Stripe session created with ID: {}", session.getId());

                // update transaction with stripe session id
                OrderTransaction tx = order.getOrderTransaction();
                tx.setStripeSessionId(session.getId());
                txRepo.save(tx);
                log.info("Transaction updated with Stripe session ID");

                return session.getUrl();
        }

        public Session retrieveSession(String sessionId) throws StripeException {
                log.info("Retrieving Stripe session: {}", sessionId);

                SessionRetrieveParams params = SessionRetrieveParams.builder()
                                .addExpand("payment_intent")
                                .build();

                Session session = Session.retrieve(sessionId, params, null);
                log.info("Stripe session retrieved successfully: {}", sessionId);

                return session;
        }
}
