package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_address_id")
    private Long orderAddressId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "regions", nullable = false)
    private String regions; // Comma-separated list of regions (regionA-regionB)

    @Column(name = "zipcode", nullable = false)
    private String zipcode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Gets the list of regions as an array
     * 
     * @return Array of regions
     */
    @Transient
    public String[] getRegionsArray() {
        if (regions == null || regions.isEmpty()) {
            return new String[0];
        }
        return regions.split(",");
    }

    /**
     * Sets the regions from an array
     * 
     * @param regionsArray Array of regions
     */
    public void setRegionsFromArray(String[] regionsArray) {
        if (regionsArray == null || regionsArray.length == 0) {
            this.regions = "";
            return;
        }
        this.regions = String.join(",", regionsArray);
    }

    /**
     * Sets the order for this address
     * 
     * @param order The order to set
     */
    public void setOrder(Order order) {
        this.order = order;
    }
}