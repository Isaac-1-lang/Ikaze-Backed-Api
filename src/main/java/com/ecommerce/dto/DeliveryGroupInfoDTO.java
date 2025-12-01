package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for delivery group information embedded in order responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryGroupInfoDTO {
    private Long deliveryGroupId;
    private String deliveryGroupName;
    private String deliveryGroupDescription;
    private String delivererId;
    private String delivererName;
    private String delivererEmail;
    private String delivererPhone;
    private Integer memberCount;
    private Boolean hasDeliveryStarted;
    private LocalDateTime deliveryStartedAt;
    private Boolean hasDeliveryFinished;
    private LocalDateTime deliveryFinishedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private String status; // READY, IN_PROGRESS, COMPLETED
}
