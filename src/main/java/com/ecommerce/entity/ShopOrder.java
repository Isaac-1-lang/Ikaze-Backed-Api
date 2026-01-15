package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "shop_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "order", "shop", "items", "readyForDeliveryGroup", "orderActivityLogs",
        "shopOrderTransaction", "returnRequests" })
@ToString(exclude = { "order", "shop", "items", "readyForDeliveryGroup", "orderActivityLogs", "shopOrderTransaction",
        "returnRequests" })
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_order_id")
    private Long id;

    @Column(name = "shop_order_code", unique = true, nullable = false)
    private String shopOrderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @OneToMany(mappedBy = "shopOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderItem> items = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShopOrderStatus status = ShopOrderStatus.PENDING;

    @Column(name = "pickup_token", unique = true, nullable = false)
    private String pickupToken;

    @Column(name = "pickup_token_used", nullable = false)
    private Boolean pickupTokenUsed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ready_group_id")
    private ReadyForDeliveryGroup readyForDeliveryGroup;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToOne(mappedBy = "shopOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrderDeliveryNote deliveryNote;

    @OneToMany(mappedBy = "shopOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderActivityLog> orderActivityLogs = new HashSet<>();

    @OneToMany(mappedBy = "shopOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderTrackingToken> trackingTokens = new HashSet<>();

    @OneToOne(mappedBy = "shopOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ShopOrderTransaction shopOrderTransaction;

    @OneToMany(mappedBy = "shopOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnRequest> returnRequests = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (shopOrderCode == null) {
            shopOrderCode = generateShopOrderCode();
        }
        if (pickupToken == null) {
            pickupToken = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateShopOrderCode() {
        return "SO-" + System.currentTimeMillis() + "-"
                + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public enum ShopOrderStatus {
        PENDING, PROCESSING, READY_FOR_PICKUP, SHIPPED, DELIVERED, CANCELLED, RETURNED, REFUNDED
    }
}
