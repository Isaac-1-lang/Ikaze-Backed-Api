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
public class DashboardResponseDTO {

    private long totalProducts;
    private long totalOrders;
    private BigDecimal totalRevenue; // Only for ADMIN; null for others
    private long totalCustomers;
    private List<RecentOrderDTO> recentOrders;
    private AlertsDTO alerts;
}
