package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for order summary in listing (for token-based access)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    
    private Long id;
    private String orderNumber;
    private String status;
    private LocalDateTime createdAt;
    private BigDecimal total;
    private int itemCount;
    private String customerName;
    private String customerEmail;
    private boolean hasReturnRequest;
}
