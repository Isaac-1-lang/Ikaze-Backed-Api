package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
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

    private List<String> productIds;

    private List<String> variantIds;
}
