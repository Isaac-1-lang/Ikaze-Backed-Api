package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantImageDTO {
    private Long imageId;
    private String url;
    private String altText;
    private Boolean isPrimary;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
