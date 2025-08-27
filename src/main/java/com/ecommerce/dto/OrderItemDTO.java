package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String id;
    private String productId;
    private SimpleProductDTO product;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
}


