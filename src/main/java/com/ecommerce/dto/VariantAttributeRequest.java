package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttributeRequest {
    @NotBlank(message = "Attribute type name is required")
    @Size(max = 100, message = "Attribute type name must not exceed 100 characters")
    private String attributeTypeName;

    @NotBlank(message = "Attribute value is required")
    @Size(max = 100, message = "Attribute value must not exceed 100 characters")
    private String attributeValue;
}
