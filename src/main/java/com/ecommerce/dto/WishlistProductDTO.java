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
public class WishlistProductDTO {

    private Long id;
    private Long variantId;
    private String variantSku;
    private String productName;
    private String productImage;
    private String notes;
    private Integer priority;
    private LocalDateTime addedAt;
    private boolean inStock;
    private int availableStock;
    private BigDecimal price;
}
