package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReadyForDeliveryGroupDTO {
    private String deliveryGroupName;
    private String deliveryGroupDescription;
    private UUID delivererId;
    private List<Long> orderIds;
}
