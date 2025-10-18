package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeTypeDTO {

    private Long attributeTypeId;

    @NotBlank(message = "Attribute type name is required")
    @Size(min = 2, max = 50, message = "Attribute type name must be between 2 and 50 characters")
    private String name;

    private boolean isRequired;

    @Builder.Default
    private List<ProductAttributeValueDTO> attributeValues = new ArrayList<>();
    
    private Long productCount = 0L;
}