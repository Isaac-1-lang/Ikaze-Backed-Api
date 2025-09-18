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
public class ProductVariantAttributeDTO {
    private Long attributeValueId;
    private String attributeValue;
    private Long attributeTypeId;
    private String attributeType;
}
