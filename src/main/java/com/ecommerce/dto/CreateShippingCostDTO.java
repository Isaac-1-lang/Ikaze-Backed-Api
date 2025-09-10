package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShippingCostDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Distance cost per km must be non-negative")
    private BigDecimal distanceKmCost;

    @DecimalMin(value = "0.0", message = "Weight cost per kg must be non-negative")
    private BigDecimal weightKgCost;

    @DecimalMin(value = "0.0", message = "Base fee must be non-negative")
    private BigDecimal baseFee;

    @DecimalMin(value = "0.0", message = "International fee must be non-negative")
    private BigDecimal internationalFee;

    @DecimalMin(value = "0.0", message = "Free shipping threshold must be non-negative")
    private BigDecimal freeShippingThreshold;

    @NotNull(message = "Active status is required")
    private Boolean isActive = true;
}
