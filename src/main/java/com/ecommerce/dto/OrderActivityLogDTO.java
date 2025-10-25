package com.ecommerce.dto;

import com.ecommerce.entity.OrderActivityLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Order Activity Log
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderActivityLogDTO {
    
    private Long id;
    private Long orderId;
    private OrderActivityLog.ActivityType activityType;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private String actorType;
    private String actorId;
    private String actorName;
    private String metadata;
    private String referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
    private OrderDeliveryNoteDTO deliveryNote;

    public static OrderActivityLogDTO fromEntity(OrderActivityLog entity) {
        if (entity == null) {
            return null;
        }

        OrderActivityLogDTO dto = new OrderActivityLogDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setActivityType(entity.getActivityType());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setTimestamp(entity.getTimestamp());
        dto.setActorType(entity.getActorType());
        dto.setActorId(entity.getActorId());
        dto.setActorName(entity.getActorName());
        dto.setMetadata(entity.getMetadata());
        dto.setReferenceId(entity.getReferenceId());
        dto.setReferenceType(entity.getReferenceType());
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}
