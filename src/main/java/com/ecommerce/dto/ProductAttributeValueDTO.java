package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeValueDTO {

    private Long attributeValueId;

    @NotBlank(message = "Value is required")
    private String value;

    @NotNull(message = "Attribute type is required")
    private Long attributeTypeId;

    private String attributeTypeName;
}