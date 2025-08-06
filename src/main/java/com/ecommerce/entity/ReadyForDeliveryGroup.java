package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private List<Order> orders = new ArrayList<>();

    @OneToOne(mappedBy = "readyForDeliveryGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DeliveryAgentLocation deliveryAgentLocation;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Adds an order to this delivery group
     * 
     * @param order The order to add
     */
    public void addOrder(Order order) {
        orders.add(order);
        order.setReadyForDeliveryGroup(this);
    }

    /**
     * Removes an order from this delivery group
     * 
     * @param order The order to remove
     */
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setReadyForDeliveryGroup(null);
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