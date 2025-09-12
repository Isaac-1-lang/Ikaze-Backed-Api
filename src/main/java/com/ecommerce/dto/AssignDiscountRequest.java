package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignDiscountRequest {

    @NotBlank(message = "Discount ID is required")
    private String discountId;

    @NotEmpty(message = "At least one variant ID is required")
    private List<String> variantIds;
}
