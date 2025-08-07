package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_agent_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentLocation {

    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_group_id", nullable = false)
    private ReadyForDeliveryGroup readyForDeliveryGroup;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Updates the location coordinates
     * 
     * @param latitude The new latitude
     * @param longitude The new longitude
     */
    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Updates the location coordinates and area name
     * 
     * @param latitude The new latitude
     * @param longitude The new longitude
     * @param areaName The new area name
     */
    public void updateLocation(Double latitude, Double longitude, String areaName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaName = areaName;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Calculates the distance between this location and another location using the Haversine formula
     * 
     * @param otherLatitude The latitude of the other location
     * @param otherLongitude The longitude of the other location
     * @return The distance in kilometers
     */
    @Transient
    public double calculateDistance(Double otherLatitude, Double otherLongitude) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(otherLatitude - latitude);
        double lonDistance = Math.toRadians(otherLongitude - longitude);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(otherLatitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}