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
public class UpdateDiscountDTO {

    @Size(min = 3, max = 100, message = "Discount name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    private BigDecimal percentage;

    @Size(max = 50, message = "Discount code cannot exceed 50 characters")
    private String discountCode;

    private LocalDateTime startDate;

    @Future(message = "End date must be today or in the future")
    private LocalDateTime endDate;

    private Boolean isActive;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    private String discountType;
}
