package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String id;
    private String productId;
    private String variantId;
    private SimpleProductDTO product;
    private SimpleProductDTO variant;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    
    // Return eligibility fields
    private Integer maxReturnDays;
    private LocalDateTime deliveredAt;
    private Boolean isReturnEligible;
    private Integer daysRemainingForReturn;
}
