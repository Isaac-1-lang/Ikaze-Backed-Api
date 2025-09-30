package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAppealRequestDTO {

    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;

    private UUID customerId;

    @NotBlank(message = "Appeal reason is required")
    private String reason;

    private String description;
}
