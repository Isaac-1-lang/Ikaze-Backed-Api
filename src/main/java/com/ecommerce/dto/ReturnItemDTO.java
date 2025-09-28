package com.ecommerce.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for specifying items to be returned
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemDTO {
    
    @NotNull(message = "Order item ID is required")
    private Long orderItemId;
    
    @NotNull(message = "Return quantity is required")
    @Min(value = 1, message = "Return quantity must be at least 1")
    private Integer returnQuantity;
    
    private String itemReason;
    private UUID productId;
    private Long variantId;
    private Integer maxQuantity;
    private String productName;
    private String variantName;
}
