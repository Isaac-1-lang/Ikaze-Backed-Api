package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsPaymentRequest {
    
    private UUID userId;
    private List<CartItemDTO> items;
    private AddressDto shippingAddress;
    private boolean useAllAvailablePoints;
}
