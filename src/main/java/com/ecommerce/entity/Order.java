package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Can be null for non-logged in users

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ready_group_id")
    private ReadyForDeliveryGroup readyForDeliveryGroup; // The delivery group this order belongs to

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrderTransaction orderTransaction;

    @Column(name = "is_barcode_scanned")
    private boolean isBarCodeScanned = false;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderCode == null || orderCode.isEmpty()) {
            orderCode = generateOrderCode();
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
     * Order status enum
     */
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED, RETURNED
    }
}