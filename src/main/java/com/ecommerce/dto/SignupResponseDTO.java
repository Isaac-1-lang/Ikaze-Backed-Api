package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponseDTO {
    private String message;
    private UUID userId;
    private Integer awardedPoints;
    private String pointsDescription;
}
