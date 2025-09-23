package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutVerificationResult {
    private String status;
    private Long amount;
    private String currency;
    private String customerEmail;
    private String receiptUrl;
    private String paymentIntentId;
    private boolean updated;
    private OrderResponseDTO order;
}
