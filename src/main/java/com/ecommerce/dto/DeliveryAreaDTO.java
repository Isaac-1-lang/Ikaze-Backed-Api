package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAreaDTO {

    private Long deliveryAreaId;
    private String deliveryAreaName;
    private Long parentId;
    private String parentName;
    private LocalDateTime createdAt;
    private List<DeliveryAreaDTO> children = new ArrayList<>();
    private int level;

    /**
     * Simplified constructor for creating a new delivery area
     * 
     * @param deliveryAreaName the name of the delivery area
     * @param parentId         the ID of the parent delivery area (null for
     *                         top-level areas)
     */
    public DeliveryAreaDTO(String deliveryAreaName, Long parentId) {
        this.deliveryAreaName = deliveryAreaName;
        this.parentId = parentId;
    }
}