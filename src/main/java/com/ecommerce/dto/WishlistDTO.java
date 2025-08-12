package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDTO {

    private Long wishlistId;
    private UUID userId;
    private List<WishlistProductDTO> products;
    private int totalProducts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isEmpty;
}
