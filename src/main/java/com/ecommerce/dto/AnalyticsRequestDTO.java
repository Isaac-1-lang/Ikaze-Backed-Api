package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsRequestDTO {
    private LocalDate startDate; // inclusive
    private LocalDate endDate; // inclusive
}
