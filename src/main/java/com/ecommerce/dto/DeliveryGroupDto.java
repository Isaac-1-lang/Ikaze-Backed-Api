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
public class DeliveryGroupDto {
    private Long deliveryGroupId;
    private String deliveryGroupName;
    private String deliveryGroupDescription;
    private java.util.UUID delivererId;
    private String delivererName;
    private List<Long> orderIds;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private Boolean hasDeliveryStarted;
    private LocalDateTime deliveryStartedAt;
    private Boolean hasDeliveryFinished;
    private LocalDateTime deliveryFinishedAt;
    private String status;
}
