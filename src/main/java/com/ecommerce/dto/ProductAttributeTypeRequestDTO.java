package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a product attribute type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeTypeRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private boolean isRequired;
}