package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateOrderShippingRequest {
    private AddressDto deliveryAddress;
    private List<CartItemDTO> items;
    private BigDecimal orderValue;
    private String userId;
    private UUID shopId;
}
