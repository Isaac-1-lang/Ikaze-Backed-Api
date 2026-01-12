package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDTO {
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Integer rewardPoints;
    private BigDecimal rewardPointsValue;
    private String currency;
    
    // New fields for distance and shipping details (aggregated from farthest warehouse)
    private Double distanceKm;
    private BigDecimal costPerKm;
    private String selectedWarehouseName;
    private String selectedWarehouseCountry;
    private Boolean isInternationalShipping;
    
    // Per-shop summaries
    private List<ShopSummary> shopSummaries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopSummary {
        private String shopId;
        private String shopName;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal shippingCost;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private Integer rewardPoints;
        private BigDecimal rewardPointsValue;
        private Integer productCount;
        
        // Shipping details for this shop
        private Double distanceKm;
        private BigDecimal costPerKm;
        private String selectedWarehouseName;
        private String selectedWarehouseCountry;
        private Boolean isInternationalShipping;
    }
}
