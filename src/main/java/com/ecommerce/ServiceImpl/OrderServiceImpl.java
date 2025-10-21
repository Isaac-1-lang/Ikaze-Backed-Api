package com.ecommerce.ServiceImpl;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderAddress;
import com.ecommerce.entity.OrderInfo;
import com.ecommerce.entity.OrderCustomerInfo;
import com.ecommerce.entity.OrderTransaction;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.ProductVariantImage;
import com.ecommerce.entity.User;
import com.ecommerce.entity.OrderItemBatch;
import com.ecommerce.entity.Warehouse;
import com.ecommerce.entity.StockBatch;
import com.ecommerce.entity.ReadyForDeliveryGroup;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.ReadyForDeliveryGroupRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.RewardService;
import com.ecommerce.dto.CreateOrderDTO;
import com.ecommerce.dto.CustomerOrderDTO;
import com.ecommerce.dto.AdminOrderDTO;
import com.ecommerce.dto.DeliveryOrderDTO;
import com.ecommerce.dto.DeliveryGroupInfoDTO;
import com.ecommerce.dto.SimpleProductDTO;
import com.ecommerce.dto.OrderSearchDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final ReadyForDeliveryGroupRepository readyForDeliveryGroupRepository;
    private final RewardService rewardService;

    public OrderServiceImpl(OrderRepository orderRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository,
            ReadyForDeliveryGroupRepository readyForDeliveryGroupRepository,
            @Autowired(required = false) RewardService rewardService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
        this.readyForDeliveryGroupRepository = readyForDeliveryGroupRepository;
        this.rewardService = rewardService;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElse(null);
    }

    /**
     * Get order by ID with user ownership validation
     * This method ensures that only the owner of the order can access it
     */
    public Order getOrderByIdWithUserValidation(Long orderId, UUID userId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return null;
        }

        // Check if the requesting user is the owner of the order
        if (order.getUser() != null && !order.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to access order {} which belongs to user {}",
                    userId, orderId, order.getUser().getId());
            throw new SecurityException("Access denied: You can only view your own orders");
        }

        return order;
    }

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
        int totalProductCount = 0;
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
            if (variant.getTotalStockQuantity() < itemDTO.getQuantity()) {
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
            totalProductCount += itemDTO.getQuantity();

            // Update stock
            // TODO: Implement proper stock reduction through Stock entities
            // variant.setStockQuantity(variant.getTotalStockQuantity() -
            // itemDTO.getQuantity());
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

        // Store Stripe payment information (no sensitive data)
        if (createOrderDTO.getStripePaymentIntentId() != null) {
            orderTransaction.setStripePaymentIntentId(createOrderDTO.getStripePaymentIntentId());
        }
        if (createOrderDTO.getStripeSessionId() != null) {
            orderTransaction.setStripeSessionId(createOrderDTO.getStripeSessionId());
        }

        // Generate transaction reference
        orderTransaction.setTransactionRef("TXN-" + System.currentTimeMillis());

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
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully with ID: {}", savedOrder.getOrderId());
        if (rewardService != null) {
            try {
                rewardService.checkRewardableOnOrderAndReward(userId, savedOrder.getOrderId(), totalProductCount,
                        subtotal);
            } catch (Exception e) {
                log.error("Error checking reward eligibility for order {}: {}", savedOrder.getOrderId(), e.getMessage(),
                        e);
            }
        } else {
            log.warn("RewardService not available, skipping reward processing for order {}", savedOrder.getOrderId());
        }

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
            // TODO: Implement proper stock increase through Stock entities
            // variant.setStockQuantity(variant.getTotalStockQuantity() +
            // item.getQuantity());
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

            // Set delivery timestamp when order is marked as delivered
            if (newStatus == Order.OrderStatus.DELIVERED) {
                order.setDeliveredAt(LocalDateTime.now());
            }

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

    // Customer methods
    public List<CustomerOrderDTO> getCustomerOrders(UUID userId) {
        List<Order> orders = getOrdersForUser(userId);
        return orders.stream()
                .map(this::toCustomerOrderDTO)
                .collect(Collectors.toList());
    }

    public CustomerOrderDTO getCustomerOrderById(UUID userId, Long orderId) {
        Order order = getOrderByIdForUser(userId, orderId);
        return toCustomerOrderDTO(order);
    }

    public CustomerOrderDTO getCustomerOrderByNumber(UUID userId, String orderNumber) {
        Order order = getOrderByNumber(userId, orderNumber);
        return toCustomerOrderDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderDTO> getAllAdminOrders() {
        List<Order> orders = orderRepository.findAllWithDetailsForAdmin();
        return orders.stream()
                .map(this::toAdminOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderDTO> getAllAdminOrdersPaginated(Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findAllWithDetailsForAdmin(pageable);
        return ordersPage.map(this::toAdminOrderDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderDTO> getOrdersByStatus(String status) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        List<Order> orders = orderRepository.findByOrderStatusWithDetailsForAdmin(orderStatus);
        return orders.stream()
                .map(this::toAdminOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDTO getAdminOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithDetailsForAdmin(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return toAdminOrderDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDTO getAdminOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderCodeWithDetailsForAdmin(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return toAdminOrderDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderDTO> searchOrders(OrderSearchDTO searchRequest, Pageable pageable) {
        log.info("Searching orders with criteria: {}", searchRequest);
        Page<Order> ordersPage = orderRepository.searchOrders(searchRequest, pageable);
        return ordersPage.map(this::toAdminOrderDTO);
    }

    // Delivery agency methods
    @Override
    public List<DeliveryOrderDTO> getDeliveryOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::toDeliveryOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryOrderDTO> getDeliveryOrdersByStatus(String status) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        List<Order> orders = orderRepository.findByOrderStatus(orderStatus);
        return orders.stream()
                .map(this::toDeliveryOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DeliveryOrderDTO getDeliveryOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderCode(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return toDeliveryOrderDTO(order);
    }

    @Override
    @Transactional
    public Order updateOrderTracking(Long orderId, String trackingNumber, String estimatedDelivery) {
        log.info("Updating tracking info for order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        // Update tracking information (you might want to add these fields to Order
        // entity)
        // For now, we'll store in notes or create a separate tracking entity
        String currentNotes = order.getOrderInfo() != null ? order.getOrderInfo().getNotes() : "";
        String updatedNotes = currentNotes + "\nTracking: " + trackingNumber +
                (estimatedDelivery != null ? "\nEstimated Delivery: " + estimatedDelivery : "");

        if (order.getOrderInfo() != null) {
            order.getOrderInfo().setNotes(updatedNotes);
        }

        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Tracking info updated for order {}", orderId);

        return savedOrder;
    }

    // DTO conversion methods
    private CustomerOrderDTO toCustomerOrderDTO(Order order) {
        return CustomerOrderDTO.builder()
                .id(order.getOrderId().toString())
                .orderNumber(order.getOrderCode())
                .status(order.getOrderStatus().name())
                .items(order.getOrderItems().stream()
                        .map(this::toCustomerOrderItemDTO)
                        .collect(Collectors.toList()))
                .subtotal(order.getOrderInfo() != null ? order.getOrderInfo().getSubtotal() : BigDecimal.ZERO)
                .tax(order.getOrderInfo() != null ? order.getOrderInfo().getTaxAmount() : BigDecimal.ZERO)
                .shipping(order.getOrderInfo() != null ? order.getOrderInfo().getShippingCost() : BigDecimal.ZERO)
                .discount(order.getOrderInfo() != null ? order.getOrderInfo().getDiscountAmount() : BigDecimal.ZERO)
                .total(order.getOrderInfo() != null ? order.getOrderInfo().getTotalAmount() : BigDecimal.ZERO)
                .shippingAddress(toCustomerOrderAddressDTO(order.getOrderAddress()))
                .billingAddress(toCustomerOrderAddressDTO(order.getOrderAddress())) // Same as shipping for now
                .paymentMethod(
                        order.getOrderTransaction() != null ? order.getOrderTransaction().getPaymentMethod().name()
                                : null)
                .paymentStatus(
                        order.getOrderTransaction() != null ? order.getOrderTransaction().getStatus().name() : null)
                .notes(order.getOrderInfo() != null ? order.getOrderInfo().getNotes() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private CustomerOrderDTO.CustomerOrderItemDTO toCustomerOrderItemDTO(OrderItem item) {
        Product product = item.getEffectiveProduct();

        // Calculate discount information by comparing with current product/variant
        // prices
        BigDecimal currentPrice = item.getPrice();
        BigDecimal originalPrice = currentPrice;
        boolean hasDiscount = false;
        BigDecimal discountPercentage = BigDecimal.ZERO;

        // Check if item was bought at a discount by comparing with current product
        // price
        if (item.isVariantBased() && item.getProductVariant() != null) {
            // For variant-based items, compare with variant price
            BigDecimal currentVariantPrice = item.getProductVariant().getPrice();
            if (currentVariantPrice.compareTo(currentPrice) > 0) {
                originalPrice = currentVariantPrice;
                hasDiscount = true;
                discountPercentage = originalPrice.subtract(currentPrice)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(originalPrice, 2, RoundingMode.HALF_UP);
            }
        } else {
            // For regular products, compare with product price
            BigDecimal currentProductPrice = product.getPrice();
            if (currentProductPrice.compareTo(currentPrice) > 0) {
                originalPrice = currentProductPrice;
                hasDiscount = true;
                discountPercentage = originalPrice.subtract(currentPrice)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(originalPrice, 2, RoundingMode.HALF_UP);
            }
        }

        return CustomerOrderDTO.CustomerOrderItemDTO.builder()
                .id(item.getOrderItemId().toString())
                .productId(product.getProductId().toString())
                .product(item.isVariantBased()
                        ? toSimpleProductDTOWithVariant(product, item.getProductVariant())
                        : toSimpleProductDTO(product))
                .quantity(item.getQuantity())
                .price(currentPrice)
                .originalPrice(originalPrice)
                .totalPrice(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .discountPercentage(discountPercentage)
                .discountName(hasDiscount ? "Discount Applied" : null)
                .hasDiscount(hasDiscount)
                .build();
    }

    private CustomerOrderDTO.CustomerOrderAddressDTO toCustomerOrderAddressDTO(OrderAddress addr) {
        if (addr == null)
            return null;

        String city = "", state = "";
        if (addr.getRegions() != null && !addr.getRegions().isEmpty()) {
            String[] regions = addr.getRegions().split(",");
            if (regions.length >= 2) {
                city = regions[0].trim();
                state = regions[1].trim();
            } else if (regions.length == 1) {
                city = regions[0].trim();
            }
        }

        return CustomerOrderDTO.CustomerOrderAddressDTO.builder()
                .id(addr.getOrderAddressId().toString())
                .street(addr.getStreet())
                .city(city)
                .state(state)
                .country(addr.getCountry())
                .phone("") // Will be set from customer info
                .latitude(addr.getLatitude())
                .longitude(addr.getLongitude())
                .build();
    }

    private AdminOrderDTO toAdminOrderDTO(Order order) {
        return AdminOrderDTO.builder()
                .id(order.getOrderId().toString())
                .userId(order.getUser() != null ? order.getUser().getId().toString() : null)
                .customerName(order.getOrderCustomerInfo() != null
                        ? order.getOrderCustomerInfo().getFirstName() + " " + order.getOrderCustomerInfo().getLastName()
                        : null)
                .customerEmail(order.getOrderCustomerInfo() != null ? order.getOrderCustomerInfo().getEmail() : null)
                .customerPhone(
                        order.getOrderCustomerInfo() != null ? order.getOrderCustomerInfo().getPhoneNumber() : null)
                .orderNumber(order.getOrderCode())
                .status(order.getOrderStatus().name())
                .items(order.getOrderItems().stream()
                        .map(this::toAdminOrderItemDTO)
                        .collect(Collectors.toList()))
                .subtotal(order.getOrderInfo() != null ? order.getOrderInfo().getSubtotal() : BigDecimal.ZERO)
                .tax(order.getOrderInfo() != null ? order.getOrderInfo().getTaxAmount() : BigDecimal.ZERO)
                .shipping(order.getOrderInfo() != null ? order.getOrderInfo().getShippingCost() : BigDecimal.ZERO)
                .discount(order.getOrderInfo() != null ? order.getOrderInfo().getDiscountAmount() : BigDecimal.ZERO)
                .total(order.getOrderInfo() != null ? order.getOrderInfo().getTotalAmount() : BigDecimal.ZERO)
                .shippingAddress(toAdminOrderAddressDTO(order.getOrderAddress(), order.getOrderCustomerInfo()))
                .billingAddress(toAdminOrderAddressDTO(order.getOrderAddress(), order.getOrderCustomerInfo()))
                .paymentInfo(toAdminPaymentInfoDTO(order.getOrderTransaction()))
                .notes(order.getOrderInfo() != null ? order.getOrderInfo().getNotes() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveryGroup(toDeliveryGroupInfoDTO(order.getReadyForDeliveryGroup()))
                .build();
    }

    private AdminOrderDTO.AdminOrderItemDTO toAdminOrderItemDTO(OrderItem item) {
        Product product = item.getEffectiveProduct();

        BigDecimal currentPrice = item.getPrice();
        BigDecimal originalPrice = currentPrice;
        boolean hasDiscount = false;
        BigDecimal discountPercentage = BigDecimal.ZERO;
        if (item.isVariantBased() && item.getProductVariant() != null) {
            BigDecimal currentVariantPrice = item.getProductVariant().getPrice();
            if (currentVariantPrice.compareTo(currentPrice) > 0) {
                originalPrice = currentVariantPrice;
                hasDiscount = true;
                discountPercentage = originalPrice.subtract(currentPrice)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(originalPrice, 2, RoundingMode.HALF_UP);
            }
        } else {
            // For regular products, compare with product price
            BigDecimal currentProductPrice = product.getPrice();
            if (currentProductPrice.compareTo(currentPrice) > 0) {
                originalPrice = currentProductPrice;
                hasDiscount = true;
                discountPercentage = originalPrice.subtract(currentPrice)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(originalPrice, 2, RoundingMode.HALF_UP);
            }
        }

        // Get warehouse and batch information
        List<AdminOrderDTO.AdminOrderWarehouseDTO> warehouses = getWarehousesForOrderItem(item);

        return AdminOrderDTO.AdminOrderItemDTO.builder()
                .id(item.getOrderItemId().toString())
                .productId(product.getProductId().toString())
                .variantId(item.isVariantBased() ? item.getProductVariant().getId().toString() : null)
                .product(item.isVariantBased()
                        ? toSimpleProductDTOWithVariant(product, item.getProductVariant())
                        : toSimpleProductDTO(product))
                .quantity(item.getQuantity())
                .price(currentPrice)
                .originalPrice(originalPrice)
                .totalPrice(currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .discountPercentage(discountPercentage)
                .discountName(hasDiscount ? "Discount Applied" : null)
                .hasDiscount(hasDiscount)
                .availableStock(item.isVariantBased()
                        ? item.getProductVariant().getTotalStockQuantity()
                        : product.getTotalStockQuantity())
                .warehouses(warehouses)
                .build();
    }

    private AdminOrderDTO.AdminOrderAddressDTO toAdminOrderAddressDTO(OrderAddress addr,
            OrderCustomerInfo customerInfo) {
        if (addr == null) {
            // If no OrderAddress, try to create from OrderCustomerInfo address fields
            if (customerInfo != null &&
                    (customerInfo.getStreetAddress() != null ||
                            customerInfo.getCity() != null ||
                            customerInfo.getCountry() != null)) {
                return AdminOrderDTO.AdminOrderAddressDTO.builder()
                        .id("customer-info")
                        .street(customerInfo.getStreetAddress())
                        .city(customerInfo.getCity())
                        .state(customerInfo.getState())
                        .country(customerInfo.getCountry())
                        .phone(customerInfo.getPhoneNumber())
                        .build();
            }
            return null;
        }

        String city = "", state = "";
        if (addr.getRegions() != null && !addr.getRegions().isEmpty()) {
            String[] regions = addr.getRegions().split(",");
            if (regions.length >= 2) {
                city = regions[0].trim();
                state = regions[1].trim();
            } else if (regions.length == 1) {
                city = regions[0].trim();
            }
        }

        // Get phone number from customer info
        String phone = customerInfo != null ? customerInfo.getPhoneNumber() : "";

        return AdminOrderDTO.AdminOrderAddressDTO.builder()
                .id(addr.getOrderAddressId().toString())
                .street(addr.getStreet())
                .city(city)
                .state(state)
                .country(addr.getCountry())
                .phone(phone)
                .latitude(addr.getLatitude())
                .longitude(addr.getLongitude())
                .build();
    }

    private List<AdminOrderDTO.AdminOrderWarehouseDTO> getWarehousesForOrderItem(OrderItem item) {
        if (item.getOrderItemBatches() == null || item.getOrderItemBatches().isEmpty()) {
            return new ArrayList<>();
        }

        Map<Warehouse, List<OrderItemBatch>> warehouseBatches = item.getOrderItemBatches().stream()
                .collect(Collectors.groupingBy(OrderItemBatch::getWarehouse));

        return warehouseBatches.entrySet().stream()
                .map(entry -> {
                    Warehouse warehouse = entry.getKey();
                    List<OrderItemBatch> batches = entry.getValue();

                    // Calculate total quantity from this warehouse
                    int totalQuantityFromWarehouse = batches.stream()
                            .mapToInt(OrderItemBatch::getQuantityUsed)
                            .sum();

                    // Convert batches to DTOs
                    List<AdminOrderDTO.AdminOrderBatchDTO> batchDTOs = batches.stream()
                            .map(this::toAdminOrderBatchDTO)
                            .collect(Collectors.toList());

                    return AdminOrderDTO.AdminOrderWarehouseDTO.builder()
                            .warehouseId(warehouse.getId().toString())
                            .warehouseName(warehouse.getName())
                            .warehouseLocation(warehouse.getCity() + ", " + warehouse.getState())
                            .warehouseAddress(warehouse.getAddress())
                            .warehousePhone(warehouse.getContactNumber())
                            .warehouseManager("N/A") // Not available in current entity
                            .quantityFromWarehouse(totalQuantityFromWarehouse)
                            .batches(batchDTOs)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Helper method to convert OrderItemBatch to AdminOrderBatchDTO
    private AdminOrderDTO.AdminOrderBatchDTO toAdminOrderBatchDTO(OrderItemBatch orderItemBatch) {
        StockBatch stockBatch = orderItemBatch.getStockBatch();

        return AdminOrderDTO.AdminOrderBatchDTO.builder()
                .batchId(stockBatch.getId().toString())
                .batchNumber(stockBatch.getBatchNumber())
                .quantityFromBatch(orderItemBatch.getQuantityUsed())
                .manufactureDate(stockBatch.getManufactureDate())
                .expiryDate(stockBatch.getExpiryDate())
                .batchStatus(stockBatch.getStatus().name())
                .supplierName(stockBatch.getSupplierName())
                .costPrice(null) // Not available in current entity
                .build();
    }

    private AdminOrderDTO.AdminPaymentInfoDTO toAdminPaymentInfoDTO(OrderTransaction tx) {
        if (tx == null)
            return null;

        return AdminOrderDTO.AdminPaymentInfoDTO.builder()
                .paymentMethod(tx.getPaymentMethod().name())
                .paymentStatus(tx.getStatus().name())
                .stripePaymentIntentId(tx.getStripePaymentIntentId())
                .stripeSessionId(tx.getStripeSessionId())
                .transactionRef(tx.getTransactionRef())
                .paymentDate(tx.getPaymentDate())
                .receiptUrl(tx.getReceiptUrl())
                .pointsUsed(tx.getPointsUsed())
                .pointsValue(tx.getPointsValue())
                .build();
    }

    private DeliveryOrderDTO toDeliveryOrderDTO(Order order) {
        return DeliveryOrderDTO.builder()
                .orderNumber(order.getOrderCode())
                .status(order.getOrderStatus().name())
                .items(order.getOrderItems().stream()
                        .map(this::toDeliveryItemDTO)
                        .collect(Collectors.toList()))
                .deliveryAddress(toDeliveryAddressDTO(order.getOrderAddress()))
                .customer(toDeliveryCustomerDTO(order.getOrderCustomerInfo()))
                .notes(order.getOrderInfo() != null ? order.getOrderInfo().getNotes() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private DeliveryOrderDTO.DeliveryItemDTO toDeliveryItemDTO(OrderItem item) {
        Product product = item.getEffectiveProduct();
        String primaryImage;
        String sku;

        if (item.isVariantBased()) {
            primaryImage = getPrimaryImageUrl(item.getProductVariant(), product);
            sku = "SKU-" + item.getProductVariant().getId();
        } else {
            primaryImage = getPrimaryImageUrl(null, product);
            sku = product.getSku();
        }

        return DeliveryOrderDTO.DeliveryItemDTO.builder()
                .productName(product.getProductName())
                .variantSku(sku)
                .quantity(item.getQuantity())
                .productImage(primaryImage != null ? primaryImage : "")
                .productDescription(product.getDescription())
                .build();
    }

    private String getPrimaryImageUrl(ProductVariant variant, Product product) {
        if (variant != null && variant.getImages() != null && !variant.getImages().isEmpty()) {
            return variant.getImages().stream()
                    .filter(ProductVariantImage::isPrimary)
                    .findFirst()
                    .map(ProductVariantImage::getImageUrl)
                    .orElse(variant.getImages().get(0).getImageUrl());
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(product.getImages().get(0).getImageUrl());
        }

        return null;
    }

    private DeliveryOrderDTO.DeliveryAddressDTO toDeliveryAddressDTO(OrderAddress addr) {
        if (addr == null)
            return null;

        String city = "", state = "";
        if (addr.getRegions() != null && !addr.getRegions().isEmpty()) {
            String[] regions = addr.getRegions().split(",");
            if (regions.length >= 2) {
                city = regions[0].trim();
                state = regions[1].trim();
            } else if (regions.length == 1) {
                city = regions[0].trim();
            }
        }

        return DeliveryOrderDTO.DeliveryAddressDTO.builder()
                .street(addr.getStreet())
                .city(city)
                .state(state)
                .country(addr.getCountry())
                .build();
    }

    private DeliveryOrderDTO.DeliveryCustomerDTO toDeliveryCustomerDTO(OrderCustomerInfo customerInfo) {
        if (customerInfo == null)
            return null;

        return DeliveryOrderDTO.DeliveryCustomerDTO.builder()
                .fullName(customerInfo.getFirstName() + " " + customerInfo.getLastName())
                .phone(customerInfo.getPhoneNumber())
                .build();
    }

    private SimpleProductDTO toSimpleProductDTO(Product product) {
        String[] imageUrls = getProductImages(product);

        return new SimpleProductDTO(
                product.getProductId().toString(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice().doubleValue(),
                imageUrls);
    }

    private SimpleProductDTO toSimpleProductDTOWithVariant(Product product, ProductVariant variant) {
        // Get variant images first, fallback to product images
        String[] imageUrls = getVariantOrProductImages(variant, product);

        return new SimpleProductDTO(
                product.getProductId().toString(),
                product.getProductName(),
                product.getDescription(),
                product.getPrice().doubleValue(),
                imageUrls);
    }

    private String[] getProductImages(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return new String[0];
        }

        try {
            return product.getImages().stream()
                    .sorted((img1, img2) -> {
                        if (img1.isPrimary() && !img2.isPrimary())
                            return -1;
                        if (!img1.isPrimary() && img2.isPrimary())
                            return 1;
                        int sortOrder1 = img1.getSortOrder() != null ? img1.getSortOrder() : 0;
                        int sortOrder2 = img2.getSortOrder() != null ? img2.getSortOrder() : 0;
                        return Integer.compare(sortOrder1, sortOrder2);
                    })
                    .map(ProductImage::getImageUrl)
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .toArray(String[]::new);
        } catch (Exception e) {
            log.warn("Error loading product images for product {}: {}", product.getProductId(), e.getMessage());
            return new String[0];
        }
    }

    private String[] getVariantOrProductImages(ProductVariant variant, Product product) {
        if (variant != null) {
            try {
                if (variant.getImages() != null && !variant.getImages().isEmpty()) {
                    String[] variantImages = variant.getImages().stream()
                            .sorted((img1, img2) -> {
                                if (img1.isPrimary() && !img2.isPrimary())
                                    return -1;
                                if (!img1.isPrimary() && img2.isPrimary())
                                    return 1;
                                int sortOrder1 = img1.getSortOrder() != null ? img1.getSortOrder() : 0;
                                int sortOrder2 = img2.getSortOrder() != null ? img2.getSortOrder() : 0;
                                return Integer.compare(sortOrder1, sortOrder2);
                            })
                            .map(ProductVariantImage::getImageUrl)
                            .filter(url -> url != null && !url.trim().isEmpty())
                            .toArray(String[]::new);

                    if (variantImages.length > 0) {
                        return variantImages;
                    }
                }
            } catch (Exception e) {
                log.warn("Error loading variant images for variant {}: {}", variant.getId(), e.getMessage());
            }
        }

        return getProductImages(product);
    }

    @Override
    public Order getOrderByOrderCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode).orElse(null);
    }

    @Override
    public Order getOrderByPickupToken(String pickupToken) {
        return orderRepository.findByPickupToken(pickupToken).orElse(null);
    }

    @Override
    public Order saveOrder(Order order) {
        log.info("Saving order with ID: {}, Status: {}, PickupTokenUsed: {}",
                order.getOrderId(), order.getOrderStatus(), order.getPickupTokenUsed());

        Order savedOrder = orderRepository.save(order);
        orderRepository.flush();

        log.info("Order saved successfully with ID: {}, Status: {}, PickupTokenUsed: {}",
                savedOrder.getOrderId(), savedOrder.getOrderStatus(), savedOrder.getPickupTokenUsed());

        return savedOrder;
    }

    @Override
    public boolean checkAllOrdersDeliveredInGroup(Long groupId) {
        log.info("Checking if all orders in group {} are delivered", groupId);

        return readyForDeliveryGroupRepository.findByIdWithOrders(groupId)
                .map(group -> {
                    List<Order> orders = group.getOrders();
                    boolean allDelivered = orders.stream()
                            .allMatch(order -> order.getOrderStatus() == Order.OrderStatus.DELIVERED
                                    && Boolean.TRUE.equals(order.getPickupTokenUsed()));

                    log.info("Group {} has {} orders, all delivered: {}",
                            groupId, orders.size(), allDelivered);

                    return allDelivered;
                })
                .orElse(false);
    }

    @Override
    public void autoFinishDeliveryGroup(Long groupId) {
        log.info("Auto-finishing delivery group {}", groupId);

        readyForDeliveryGroupRepository.findByIdWithDeliverer(groupId)
                .ifPresent(group -> {
                    if (!group.getHasDeliveryFinished()) {
                        group.setHasDeliveryFinished(true);
                        group.setDeliveryFinishedAt(LocalDateTime.now());
                        readyForDeliveryGroupRepository.save(group);
                        log.info("Auto-finished delivery group {}", groupId);
                    } else {
                        log.info("Delivery group {} is already finished", groupId);
                    }
                });
    }

    // Helper method to convert ReadyForDeliveryGroup to DeliveryGroupInfoDTO
    private DeliveryGroupInfoDTO toDeliveryGroupInfoDTO(ReadyForDeliveryGroup group) {
        if (group == null) {
            return null;
        }

        User deliverer = group.getDeliverer();
        String delivererName = deliverer != null
                ? deliverer.getFirstName() + " " + deliverer.getLastName()
                : null;

        // Determine status
        String status;
        if (group.getHasDeliveryFinished()) {
            status = "COMPLETED";
        } else if (group.getHasDeliveryStarted()) {
            status = "IN_PROGRESS";
        } else {
            status = "READY";
        }

        return DeliveryGroupInfoDTO.builder()
                .deliveryGroupId(group.getDeliveryGroupId())
                .deliveryGroupName(group.getDeliveryGroupName())
                .deliveryGroupDescription(group.getDeliveryGroupDescription())
                .delivererId(deliverer != null ? deliverer.getId().toString() : null)
                .delivererName(delivererName)
                .delivererEmail(deliverer != null ? deliverer.getUserEmail() : null)
                .delivererPhone(deliverer != null ? deliverer.getPhoneNumber() : null)
                .memberCount(group.getOrders() != null ? group.getOrders().size() : 0)
                .hasDeliveryStarted(group.getHasDeliveryStarted())
                .deliveryStartedAt(group.getDeliveryStartedAt())
                .hasDeliveryFinished(group.getHasDeliveryFinished())
                .deliveryFinishedAt(group.getDeliveryFinishedAt())
                .scheduledAt(group.getScheduledAt())
                .createdAt(group.getCreatedAt())
                .status(status)
                .build();
    }
    
    @Override
    public long countOrdersByStatus(String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.countByOrderStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {}", status);
            return 0;
        }
    }
    
    @Override
    public long countProcessingOrdersWithoutDeliveryGroup() {
        return orderRepository.countByOrderStatusAndReadyForDeliveryGroupIsNull(Order.OrderStatus.PROCESSING);
    }
}