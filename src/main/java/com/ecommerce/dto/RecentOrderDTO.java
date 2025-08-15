package com.ecommerce.dto;

import com.ecommerce.entity.Order;
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
    private Long orderId;
    private Order.OrderStatus status;
    private BigDecimal amount;
    private String owner; // user full name or "guest"
}
