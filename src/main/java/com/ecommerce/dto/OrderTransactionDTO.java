package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTransactionDTO {
    private String orderTransactionId;
    private BigDecimal orderAmount;
    private String paymentMethod;
    private String transactionRef;
    private String status;
    private String receiptUrl;
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private LocalDateTime paymentDate;
    private Integer pointsUsed;
    private BigDecimal pointsValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
