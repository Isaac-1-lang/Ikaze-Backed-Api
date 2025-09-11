package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadyForDeliveryGroupDTO {
    private Long deliveryGroupId;
    private String deliveryGroupName;
    private String deliveryGroupDescription;
    private java.util.UUID delivererId;
    private String delivererName;
    private List<String> orderIds;
    private Integer orderCount;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private Boolean hasDeliveryStarted;
    private LocalDateTime deliveryStartedAt;
}
