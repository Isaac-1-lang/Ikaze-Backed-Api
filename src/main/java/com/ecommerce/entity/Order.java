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
@EqualsAndHashCode(exclude = { "orderTransaction", "orderAddress", "orderInfo", "orderCustomerInfo", "shopOrders",
        "user" })
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrderTransaction orderTransaction;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OrderAddress orderAddress;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OrderInfo orderInfo;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OrderCustomerInfo orderCustomerInfo;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShopOrder> shopOrders = new ArrayList<>();

    @Column(name = "order_status", nullable = false)
    private String orderStatus = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderCode == null || orderCode.isEmpty()) {
            orderCode = generateOrderCode();
        }
        if (orderStatus == null || orderStatus.isEmpty()) {
            orderStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Adds a shop order to the order
     * 
     * @param shopOrder The shop order to add
     */
    public void addShopOrder(ShopOrder shopOrder) {
        shopOrders.add(shopOrder);
        shopOrder.setOrder(this);
    }

    /**
     * Removes a shop order from the order
     * 
     * @param shopOrder The shop order to remove
     */
    public void removeShopOrder(ShopOrder shopOrder) {
        shopOrders.remove(shopOrder);
        shopOrder.setOrder(null);
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
     * Get order ID (convenience method for analytics)
     */
    public Long getId() {
        return orderId;
    }

    /**
     * Get order status (from database column or calculated)
     */
    public String getStatus() {
        // Return persisted status if available
        if (orderStatus != null && !orderStatus.isEmpty()) {
            return orderStatus;
        }
        
        // Fallback: calculate from shop orders
        if (shopOrders == null || shopOrders.isEmpty())
            return "PENDING";

        // Example logic: if any is processing, return processing. If all delivered,
        // return delivered.
        boolean allDelivered = shopOrders.stream()
                .allMatch(so -> so.getStatus() == ShopOrder.ShopOrderStatus.DELIVERED);
        if (allDelivered)
            return "DELIVERED";

        return "PROCESSING"; // simplified
    }
    
    /**
     * Set order status
     */
    public void setStatus(String status) {
        this.orderStatus = status;
    }

    /**
     * Get total amount (convenience method for analytics)
     */
    public BigDecimal getTotalAmount() {
        if (orderInfo != null && orderInfo.getFinalAmount() != null) {
            return orderInfo.getFinalAmount();
        }

        // Calculate from shop orders
        return shopOrders.stream()
                .map(ShopOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if all shop orders are fully returned
     */
    @Transient
    public boolean isFullyReturned() {
        if (shopOrders == null || shopOrders.isEmpty()) {
            return false;
        }

        return shopOrders.stream()
                .allMatch(so -> so.getStatus() == ShopOrder.ShopOrderStatus.RETURNED
                        || so.getStatus() == ShopOrder.ShopOrderStatus.REFUNDED);
    }

    // Helper method to get all items across all shop orders
    @Transient
    public List<OrderItem> getAllItems() {
        return shopOrders.stream()
                .flatMap(so -> so.getItems().stream())
                .collect(java.util.stream.Collectors.toList());
    }
}