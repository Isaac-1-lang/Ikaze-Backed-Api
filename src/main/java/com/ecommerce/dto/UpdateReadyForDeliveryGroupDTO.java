package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReadyForDeliveryGroupDTO {
    private String deliveryGroupName;
    private String deliveryGroupDescription;
    private java.util.UUID delivererId;
}
