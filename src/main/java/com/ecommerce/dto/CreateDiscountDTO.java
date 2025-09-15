package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiscountDTO {

    @NotBlank(message = "Discount name is required")
    @Size(min = 3, max = 100, message = "Discount name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    private BigDecimal percentage;

    @Size(max = 50, message = "Discount code cannot exceed 50 characters")
    private String discountCode;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private boolean isActive = true;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    private String discountType = "PERCENTAGE";
}
