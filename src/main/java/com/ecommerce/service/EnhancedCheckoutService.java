package com.ecommerce.service;

import com.ecommerce.dto.AddressDto;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.CheckoutRequest;
import com.ecommerce.dto.GuestCheckoutRequest;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedCheckoutService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final StripeService stripeService;
    private final EnhancedStockValidationService stockValidationService;
    private final EnhancedMultiWarehouseAllocator warehouseAllocator;
    private final FEFOStockAllocationService fefoService;
    private final com.ecommerce.service.ShippingCostService shippingCostService;
    private final com.ecommerce.service.RewardService rewardService;

    @Transactional
    public String createCheckoutSession(CheckoutRequest req) throws Exception {
        log.info("Creating enhanced checkout session for user");

        User user = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + getCurrentUserId()));

        stockValidationService.validateCartItems(req.getItems());

        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = 
            warehouseAllocator.allocateStockWithFEFO(req.getItems(), req.getShippingAddress());

        Order order = createOrderFromRequest(req, user, allocations);
        Order savedOrder = orderRepository.save(order);

        for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : allocations.entrySet()) {
            fefoService.commitAllocation(entry.getValue());
            createOrderItemBatches(savedOrder, entry.getKey(), entry.getValue());
        }

        String sessionUrl = stripeService.createCheckoutSessionForOrder(savedOrder, req.getCurrency(), req.getPlatform());
        log.info("Enhanced checkout session created successfully");

        return sessionUrl;
    }

    @Transactional
    public String createGuestCheckoutSession(GuestCheckoutRequest req) throws Exception {
        log.info("Creating enhanced guest checkout session");

        stockValidationService.validateCartItems(req.getItems());

        Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations = 
            warehouseAllocator.allocateStockWithFEFO(req.getItems(), req.getAddress());

        Order order = createGuestOrderFromRequest(req, allocations);
        Order savedOrder = orderRepository.save(order);

        for (Map.Entry<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> entry : allocations.entrySet()) {
            fefoService.commitAllocation(entry.getValue());
            createOrderItemBatches(savedOrder, entry.getKey(), entry.getValue());
        }

        String sessionUrl = stripeService.createCheckoutSessionForOrder(savedOrder, "usd", req.getPlatform());
        log.info("Enhanced guest checkout session created successfully");

        return sessionUrl;
    }

    private Order createOrderFromRequest(CheckoutRequest req, User user, 
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations) {
        
        Order order = new Order();
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setUser(user);

        OrderCustomerInfo customerInfo = new OrderCustomerInfo();
        customerInfo.setFirstName(user.getFirstName());
        customerInfo.setLastName(user.getLastName());
        customerInfo.setEmail(user.getUserEmail());
        customerInfo.setPhoneNumber(user.getPhoneNumber());

        if (req.getShippingAddress() != null) {
            OrderAddress orderAddress = new OrderAddress();
            orderAddress.setOrder(order);
            orderAddress.setStreet(req.getShippingAddress().getStreetAddress());
            orderAddress.setCountry(req.getShippingAddress().getCountry());
            orderAddress.setRegions(req.getShippingAddress().getCity() + "," + req.getShippingAddress().getState());
            orderAddress.setLatitude(req.getShippingAddress().getLatitude());
            orderAddress.setLongitude(req.getShippingAddress().getLongitude());
            orderAddress.setRoadName(req.getShippingAddress().getStreetAddress());
            order.setOrderAddress(orderAddress);
        }
        order.setOrderCustomerInfo(customerInfo);
        customerInfo.setOrder(order);

        createOrderItems(order, req.getItems());
        createOrderInfo(order, req.getShippingAddress(), req.getItems(), user.getId());
        createOrderTransaction(order);

        return order;
    }

    private Order createGuestOrderFromRequest(GuestCheckoutRequest req, 
            Map<CartItemDTO, List<FEFOStockAllocationService.BatchAllocation>> allocations) {
        
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
            customerInfo.setCountry(req.getAddress().getCountry());
        }
        
        order.setOrderCustomerInfo(customerInfo);
        customerInfo.setOrder(order);

        createOrderItems(order, req.getItems());
        createOrderInfo(order, req.getAddress(), req.getItems(), null);
        createOrderTransaction(order);

        return order;
    }

    private void createOrderItems(Order order, List<CartItemDTO> items) {
        for (CartItemDTO item : items) {
            OrderItem orderItem = new OrderItem();
            BigDecimal itemPrice;

            if (item.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + item.getVariantId()));
                orderItem.setProductVariant(variant);
                itemPrice = calculateDiscountedPrice(variant);
            } else {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));
                orderItem.setProduct(product);
                itemPrice = calculateDiscountedPrice(product);
            }

            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(itemPrice);
            orderItem.setOrder(order);
            order.getOrderItems().add(orderItem);
        }
    }

    private void createOrderItemBatches(Order order, CartItemDTO cartItem, 
            List<FEFOStockAllocationService.BatchAllocation> allocations) {
        
        OrderItem orderItem = order.getOrderItems().stream()
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

    private void createOrderInfo(Order order, AddressDto address, List<CartItemDTO> items, UUID userId) {
        com.ecommerce.dto.PaymentSummaryDTO paymentSummary = calculatePaymentSummary(address, items, userId);
        
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrder(order);
        orderInfo.setTotalAmount(paymentSummary.getTotalAmount());
        orderInfo.setTaxAmount(paymentSummary.getTaxAmount());
        orderInfo.setShippingCost(paymentSummary.getShippingCost());
        orderInfo.setDiscountAmount(paymentSummary.getDiscountAmount());
        order.setOrderInfo(orderInfo);
    }

    private void createOrderTransaction(Order order) {
        OrderTransaction tx = new OrderTransaction();
        tx.setOrderAmount(order.getOrderInfo().getTotalAmount());
        tx.setPaymentMethod(OrderTransaction.PaymentMethod.CREDIT_CARD);
        tx.setStatus(OrderTransaction.TransactionStatus.PENDING);
        order.setOrderTransaction(tx);
        tx.setOrder(order);
    }

    private BigDecimal calculateDiscountedPrice(ProductVariant variant) {
        if (variant.getDiscount() != null && variant.getDiscount().isValid() && variant.getDiscount().isActive()) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    variant.getDiscount().getPercentage().divide(BigDecimal.valueOf(100.0)));
            return variant.getPrice().multiply(discountMultiplier);
        }

        if (variant.getProduct() != null && variant.getProduct().getDiscount() != null
                && variant.getProduct().getDiscount().isValid() && variant.getProduct().getDiscount().isActive()) {
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

    private com.ecommerce.dto.PaymentSummaryDTO calculatePaymentSummary(AddressDto deliveryAddress,
            List<CartItemDTO> items, UUID userId) {
        
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        int totalProductCount = 0;

        for (CartItemDTO item : items) {
            BigDecimal itemPrice = BigDecimal.ZERO;
            BigDecimal originalPrice = BigDecimal.ZERO;

            if (item.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + item.getVariantId()));
                originalPrice = variant.getPrice();
                itemPrice = calculateDiscountedPrice(variant);
            } else if (item.getProductId() != null) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));
                originalPrice = product.getPrice();
                itemPrice = calculateDiscountedPrice(product);
            }

            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal itemDiscount = originalPrice.subtract(itemPrice)
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(itemTotal);
            discountAmount = discountAmount.add(itemDiscount);
            totalProductCount += item.getQuantity();
        }

        BigDecimal shippingCost = shippingCostService.calculateOrderShippingCost(deliveryAddress, items, subtotal);
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingCost).add(taxAmount);

        Integer rewardPoints = 0;
        BigDecimal rewardPointsValue = BigDecimal.ZERO;
        if (userId != null) {
            rewardPoints = rewardService.getPreviewPointsForOrder(totalProductCount, subtotal);
            rewardPointsValue = rewardService.calculatePointsValue(rewardPoints);
        }

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

    private UUID getCurrentUserId() {
        return UUID.randomUUID();
    }
}
