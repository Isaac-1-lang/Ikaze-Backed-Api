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

    // Legacy field - kept for backward compatibility but deprecated
    @Deprecated
    private UUID shopId;

    private List<CartItemDTO> items;
    private AddressDto shippingAddress;
    private boolean useAllAvailablePoints;

    // New field for multi-vendor points selection
    private List<ShopPointsSelection> selectedShopsForPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopPointsSelection {
        private UUID shopId;
        private Integer pointsToUse;
    }
}
