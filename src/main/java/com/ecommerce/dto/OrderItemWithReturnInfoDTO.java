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
public class OrderItemWithReturnInfoDTO {
    
    // Order item basic info
    private Long id;
    private String productId;
    private Long variantId;
    private String productName;
    private String variantName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private List<String> images;
    
    // Return eligibility info
    private Boolean isReturnable;
    private Integer daysRemainingForReturn;
    
    // Return information
    private ReturnItemInfo returnInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemInfo {
        private Boolean hasReturnRequest;
        private Integer totalReturnedQuantity;
        private Integer remainingQuantity;
        private List<ReturnRequestSummary> returnRequests;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnRequestSummary {
        private Long returnRequestId;
        private Integer returnedQuantity;
        private String status;
        private String reason;
        private String submittedAt;
    }
}
