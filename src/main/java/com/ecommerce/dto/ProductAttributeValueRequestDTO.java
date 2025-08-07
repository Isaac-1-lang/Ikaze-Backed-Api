package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a product attribute value
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeValueRequestDTO {

    @NotBlank(message = "Value is required")
    private String value;

    @NotNull(message = "Attribute type ID is required")
    private Long attributeTypeId;
}