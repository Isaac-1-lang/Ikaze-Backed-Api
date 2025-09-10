package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentDashboardDTO {
    private DeliveryAgentStatsDTO stats;
    private List<DeliveryGroupDto> currentGroups;
    private List<DeliveryGroupDto> completedGroups;
}
