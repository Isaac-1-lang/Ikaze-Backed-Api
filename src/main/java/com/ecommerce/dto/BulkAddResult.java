package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAddResult {
    private int totalRequested;
    private int successfullyAdded;
    private int skipped;
    private List<SkippedOrder> skippedOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedOrder {
        private Long orderId;
        private String reason;
        private String details;
    }
}
