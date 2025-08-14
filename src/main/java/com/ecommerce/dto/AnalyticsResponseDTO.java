package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsResponseDTO {
    private BigDecimal totalRevenue; // admin only
    private Double totalRevenueVsPercent;

    private long totalOrders;
    private Double totalOrdersVsPercent;

    private long newCustomers;
    private Double newCustomersVsPercent;

    private long activeProducts;
    private Double activeProductsVsPercent;

    private List<TopProductDTO> topProducts;
    private List<CategoryPerformanceDTO> categoryPerformance;
}
