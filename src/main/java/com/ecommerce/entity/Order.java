package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "orderTransaction", "orderAddress", "orderInfo", "orderCustomerInfo", "orderItems",
        "user", "readyForDeliveryGroup" })
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @Column(name = "pickup_token", unique = true, nullable = false)
    private String pickupToken;

    @Column(name = "pickup_token_used", nullable = false)
    private Boolean pickupTokenUsed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ready_group_id")
    private ReadyForDeliveryGroup readyForDeliveryGroup;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrderTransaction orderTransaction;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OrderAddress orderAddress;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OrderInfo orderInfo;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OrderCustomerInfo orderCustomerInfo;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderCode == null || orderCode.isEmpty()) {
            orderCode = generateOrderCode();
        }
        if (pickupToken == null || pickupToken.isEmpty()) {
            pickupToken = generatePickupToken();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Sets the ready for delivery group
     * 
     * @param readyForDeliveryGroup The ready for delivery group to set
     */
    public void setReadyForDeliveryGroup(ReadyForDeliveryGroup readyForDeliveryGroup) {
        this.readyForDeliveryGroup = readyForDeliveryGroup;
    }

    /**
     * Adds an item to the order
     * 
     * @param item The order item to add
     */
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    /**
     * Removes an item from the order
     * 
     * @param item The order item to remove
     */
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    /**
     * Sets the order address
     * 
     * @param address The order address to set
     */
    public void setOrderAddress(OrderAddress address) {
        this.orderAddress = address;
        address.setOrder(this);
    }

    /**
     * Sets the order info
     * 
     * @param info The order info to set
     */
    public void setOrderInfo(OrderInfo info) {
        this.orderInfo = info;
        info.setOrder(this);
    }

    /**
     * Sets the order customer info
     * 
     * @param customerInfo The order customer info to set
     */
    public void setOrderCustomerInfo(OrderCustomerInfo customerInfo) {
        this.orderCustomerInfo = customerInfo;
        customerInfo.setOrder(this);
    }

    /**
     * Sets the order transaction
     * 
     * @param transaction The order transaction to set
     */
    public void setOrderTransaction(OrderTransaction transaction) {
        this.orderTransaction = transaction;
        transaction.setOrder(this);
    }

    /**
     * Generates a unique order code
     * 
     * @return A unique order code
     */
    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates a unique pickup token for order verification
     * 
     * @return A unique pickup token
     */
    private String generatePickupToken() {
        // Create a long-encoded token using Base64 encoding
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().replace("-", "");
        String combined = timestamp + "-" + randomPart + "-" + orderId;

        // Encode the combined string using Base64
        return java.util.Base64.getEncoder().encodeToString(combined.getBytes());
    }

    /**
     * Get order ID (convenience method for analytics)
     */
    public Long getId() {
        return orderId;
    }

    /**
     * Get order status as string (convenience method for analytics)
     */
    public String getStatus() {
        return orderStatus != null ? orderStatus.name() : null;
    }

    /**
     * Get total amount (convenience method for analytics)
     */
    public BigDecimal getTotalAmount() {
        if (orderInfo != null && orderInfo.getFinalAmount() != null) {
            return orderInfo.getFinalAmount();
        }

        // Calculate from order items if order info is not available
        return orderItems.stream()
                .map(item -> {
                    BigDecimal price = item.getProductVariant() != null && item.getProductVariant().getPrice() != null
                            ? item.getProductVariant().getPrice()
                            : BigDecimal.ZERO;
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Order status enum
     */
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED
    }
}