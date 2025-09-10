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
public class ShippingDetailsDTO {
    private BigDecimal shippingCost;
    private Double distanceKm;
    private BigDecimal costPerKm;
    private String selectedWarehouseName;
    private String selectedWarehouseCountry;
    private Boolean isInternationalShipping;
    private BigDecimal baseFee;
    private BigDecimal distanceCost;
    private BigDecimal weightCost;
    private BigDecimal internationalFee;
    private BigDecimal totalWeight;
}
