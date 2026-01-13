package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ready_for_delivery_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadyForDeliveryGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_group_id")
    private Long deliveryGroupId;

    @Column(name = "delivery_group_name", nullable = false)
    private String deliveryGroupName;

    @Column(name = "delivery_group_description", length = 1000)
    private String deliveryGroupDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deliverer_id")
    private User deliverer;

    @OneToMany(mappedBy = "readyForDeliveryGroup", fetch = FetchType.LAZY)
    private Set<ShopOrder> shopOrders = new HashSet<>();

    @OneToOne(mappedBy = "readyForDeliveryGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DeliveryAgentLocation deliveryAgentLocation;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "has_delivery_started", nullable = false)
    private Boolean hasDeliveryStarted = false;

    @Column(name = "delivery_started_at")
    private LocalDateTime deliveryStartedAt;

    @Column(name = "has_delivery_finished", nullable = false)
    private Boolean hasDeliveryFinished = false;

    @Column(name = "delivery_finished_at")
    private LocalDateTime deliveryFinishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Sets the ready for delivery group for an order
     * 
     * @param shopOrder The shop order to set
     */
    public void setReadyForDeliveryGroup(ShopOrder shopOrder) {
        // This method is needed for the relationship
    }

    /**
     * Adds an order to this delivery group
     * 
     * @param shopOrder The shop order to add
     */
    public void addShopOrder(ShopOrder shopOrder) {
        shopOrders.add(shopOrder);
        shopOrder.setReadyForDeliveryGroup(this);
    }

    /**
     * Removes an order from this delivery group
     * 
     * @param shopOrder The shop order to remove
     */
    public void removeShopOrder(ShopOrder shopOrder) {
        shopOrders.remove(shopOrder);
        shopOrder.setReadyForDeliveryGroup(null);
    }

    /**
     * Sets the delivery agent location
     * 
     * @param location The location to set
     */
    public void setDeliveryAgentLocation(DeliveryAgentLocation location) {
        this.deliveryAgentLocation = location;
        location.setReadyForDeliveryGroup(this);
    }
}