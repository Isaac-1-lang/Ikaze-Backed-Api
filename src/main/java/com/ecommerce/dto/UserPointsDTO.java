package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPointsDTO {

    private Long id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private Integer points;
    private String pointsType;
    private String description;
    private Long orderId;
    private BigDecimal pointsValue;
    private Integer balanceAfter;
    private LocalDateTime createdAt;
}
