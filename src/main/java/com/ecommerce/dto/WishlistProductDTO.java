package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistProductDTO {

    private Long id;
    private UUID productId;
    private String productSku;
    private String productName;
    private String productImage;
    private String notes;
    private Integer priority;
    private LocalDateTime addedAt;
    private boolean inStock;
    private int availableStock;
    private BigDecimal price;
    private BigDecimal finalPrice;
}
