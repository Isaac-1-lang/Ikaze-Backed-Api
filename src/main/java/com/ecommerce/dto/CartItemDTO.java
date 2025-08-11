package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    
    private Long id;
    private Long variantId;
    private String variantSku;
    private String productName;
    private String productImage;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private LocalDateTime addedAt;
    private boolean inStock;
    private int availableStock;
}

