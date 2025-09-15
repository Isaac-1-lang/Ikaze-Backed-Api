package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarProductsRequestDTO {
    private UUID productId;
    private int page = 0;
    private int size = 12;
    private boolean includeOutOfStock = false;
    private String algorithm = "mixed";
}
