package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppealStatisticsDTO {
    private long pendingCount;
    private long approvedCount;
    private long deniedCount;
    private long recentCount;
    private long urgentCount;
    private double approvalRate;
}
