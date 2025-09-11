package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    
    // New fields for distance and shipping details
    private Double distanceKm;
    private BigDecimal costPerKm;
    private String selectedWarehouseName;
    private String selectedWarehouseCountry;
    private Boolean isInternationalShipping;
}
