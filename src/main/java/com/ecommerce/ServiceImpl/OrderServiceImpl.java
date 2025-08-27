// ...existing code...
package com.ecommerce.ServiceImpl;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderAddress;
import com.ecommerce.entity.OrderInfo;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.dto.CreateOrderDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElse(null);
    }

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    public List<Order> getOrdersForUser(UUID userId) {
        return orderRepository.findAllForUserWithDetails(userId);
    }

    @Override
    public Order getOrderByIdForUser(UUID userId, Long orderId) {
        return orderRepository.findByIdForUserWithDetails(userId, orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    @Override
    public Order getOrderByNumber(UUID userId, String orderNumber) {
        return orderRepository.findByOrderCodeAndUser_Id(orderNumber, userId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    @Override
    @Transactional
    public Order createOrder(UUID userId, CreateOrderDTO createOrderDTO) {
        log.info("Creating order for user: {}", userId);

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Generate order code
        order.setOrderCode("ORD-" + System.currentTimeMillis());

        // Prevent duplicate items (by variant)
        Map<String, Integer> variantCount = new HashMap<>();
        for (CreateOrderDTO.CreateOrderItemDTO itemDTO : createOrderDTO.getItems()) {
            String variantId = itemDTO.getVariantId();
            variantCount.put(variantId, variantCount.getOrDefault(variantId, 0) + 1);
            if (variantCount.get(variantId) > 1) {
                throw new IllegalArgumentException("Duplicate product variant in order: " + variantId);
            }
        }

        // Create order items
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderDTO.CreateOrderItemDTO itemDTO : createOrderDTO.getItems()) {
            // Find product variant by ID (Long)
            Long variantIdLong;
            try {
                variantIdLong = Long.parseLong(itemDTO.getVariantId());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid variantId: must be a number. Got: " + itemDTO.getVariantId());
            }
            ProductVariant variant = productVariantRepository.findById(variantIdLong)
                    .orElseThrow(
                            () -> new EntityNotFoundException("Product variant not found: " + itemDTO.getVariantId()));
            Product product = variant.getProduct();

            // Check stock
            if (variant.getStockQuantity() < itemDTO.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getProductName());
            }

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPrice(variant.getPrice());
            orderItem.setCreatedAt(LocalDateTime.now());
            orderItem.setUpdatedAt(LocalDateTime.now());

            order.getOrderItems().add(orderItem);
            subtotal = subtotal.add(variant.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            // Update stock
            variant.setStockQuantity(variant.getStockQuantity() - itemDTO.getQuantity());
            productVariantRepository.save(variant);
        }

        // TODO: Calculate and set taxes, shipping, and discounts if needed
        // subtotal = subtotal.add(...)

        // TODO: Integrate payment gateway here (stub)
        // e.g., callPaymentGateway(order, orderInfo)

        // TODO: Send notification (stub)
        // e.g., notificationService.sendOrderCreated(user, order)

        // Create order address
        OrderAddress orderAddress = new OrderAddress();
        orderAddress.setOrder(order);
        orderAddress.setStreet(createOrderDTO.getShippingAddress().getStreet());
        orderAddress.setZipcode(createOrderDTO.getShippingAddress().getZipCode());
        orderAddress.setCountry(createOrderDTO.getShippingAddress().getCountry());

        // Store city and state in regions field (comma-separated)
        String regions = createOrderDTO.getShippingAddress().getCity() + ","
                + createOrderDTO.getShippingAddress().getState();
        orderAddress.setRegions(regions);

        order.setOrderAddress(orderAddress);

        // Create order info
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrder(order);
        orderInfo.setTotalAmount(subtotal);
        orderInfo.setTaxAmount(BigDecimal.ZERO); // Calculate tax if needed
        orderInfo.setShippingCost(BigDecimal.ZERO); // Calculate shipping if needed
        orderInfo.setDiscountAmount(BigDecimal.ZERO);
        orderInfo.setNotes(createOrderDTO.getNotes());
        order.setOrderInfo(orderInfo);

        // Create order transaction
        OrderTransaction orderTransaction = new OrderTransaction();
        orderTransaction.setOrder(order);
        orderTransaction.setOrderAmount(subtotal);

        // Map payment method from frontend to backend enum
        OrderTransaction.PaymentMethod paymentMethod;
        try {
            paymentMethod = OrderTransaction.PaymentMethod.valueOf(createOrderDTO.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to CREDIT_CARD if payment method is not recognized
            paymentMethod = OrderTransaction.PaymentMethod.CREDIT_CARD;
        }
        orderTransaction.setPaymentMethod(paymentMethod);
        orderTransaction.setStatus(OrderTransaction.TransactionStatus.PENDING);
        orderTransaction.setCreatedAt(LocalDateTime.now());
        orderTransaction.setUpdatedAt(LocalDateTime.now());
        order.setOrderTransaction(orderTransaction);

        // Create customer info
        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setOrder(order);
        customerInfo.setFirstName(user.getFirstName());
        customerInfo.setLastName(user.getLastName());
        customerInfo.setEmail(user.getUserEmail());
        customerInfo.setPhoneNumber(createOrderDTO.getShippingAddress().getPhone());
        order.setOrderCustomerInfo(customerInfo);

        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getOrderId());

        return savedOrder;
    }

    @Override
    @Transactional
    public Order cancelOrder(UUID userId, Long orderId) {
        log.info("Cancelling order {} for user {}", orderId, userId);

        Order order = getOrderByIdForUser(userId, orderId);

        // Check if order can be cancelled
        if (order.getOrderStatus() != Order.OrderStatus.PENDING &&
                order.getOrderStatus() != Order.OrderStatus.PROCESSING) {
            throw new IllegalArgumentException(
                    "Order cannot be cancelled in current status: " + order.getOrderStatus());
        }

        // Update order status
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = item.getProductVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        // Update transaction status
        if (order.getOrderTransaction() != null) {
            order.getOrderTransaction().setStatus(OrderTransaction.TransactionStatus.CANCELLED);
            order.getOrderTransaction().setUpdatedAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled successfully", orderId);

        return savedOrder;
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        log.info("Updating order {} status to {}", orderId, status);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        try {
            Order.OrderStatus currentStatus = order.getOrderStatus();
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());

            // Enforce valid status transitions
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                throw new IllegalArgumentException("Invalid status transition: " + currentStatus + " -> " + newStatus);
            }

            order.setOrderStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());

            // Update transaction status based on order status
            if (order.getOrderTransaction() != null) {
                switch (newStatus) {
                    case PROCESSING:
                    case SHIPPED:
                    case DELIVERED:
                        order.getOrderTransaction().setStatus(OrderTransaction.TransactionStatus.COMPLETED);
                        break;
                    case CANCELLED:
                        order.getOrderTransaction().setStatus(OrderTransaction.TransactionStatus.CANCELLED);
                        break;
                    default:
                        // Keep current transaction status
                        break;
                }
                order.getOrderTransaction().setUpdatedAt(LocalDateTime.now());
            }

            Order savedOrder = orderRepository.save(order);
            log.info("Order {} status updated to {}", orderId, status);

            // TODO: Send notification (stub)
            // e.g., notificationService.sendOrderStatusChanged(order)

            return savedOrder;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }

    }

    // Helper to enforce valid status transitions
    private boolean isValidStatusTransition(Order.OrderStatus current, Order.OrderStatus next) {
        switch (current) {
            case PENDING:
                return next == Order.OrderStatus.PROCESSING || next == Order.OrderStatus.CANCELLED;
            case PROCESSING:
                return next == Order.OrderStatus.SHIPPED || next == Order.OrderStatus.CANCELLED;
            case SHIPPED:
                return next == Order.OrderStatus.DELIVERED;
            case DELIVERED:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }

    @Override
    public Map<String, Object> getOrderTracking(UUID userId, Long orderId) {
        log.info("Getting tracking info for order {} for user {}", orderId, userId);

        Order order = getOrderByIdForUser(userId, orderId);

        Map<String, Object> trackingInfo = new HashMap<>();
        trackingInfo.put("status", order.getOrderStatus().name());
        trackingInfo.put("orderNumber", order.getOrderCode());
        trackingInfo.put("estimatedDelivery", null); // Not implemented yet
        trackingInfo.put("trackingNumber", null); // Not implemented yet

        return trackingInfo;
    }
}