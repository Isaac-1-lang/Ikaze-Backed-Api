package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenizedAppealRequestDTO {

    @NotNull(message = "Return request ID is required")
    private Long returnRequestId;

    @NotBlank(message = "Tracking token is required")
    private String trackingToken;

    @NotBlank(message = "Appeal reason is required")
    private String reason;

    private String description;
}
