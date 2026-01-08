package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentOrderDTO {
    private String orderId; // can be Order.orderCode or ShopOrder.shopOrderCode
    private String status; // string representation to support Order/ShopOrder statuses
    private BigDecimal amount;
    private String owner; // user full name or "guest"
}
