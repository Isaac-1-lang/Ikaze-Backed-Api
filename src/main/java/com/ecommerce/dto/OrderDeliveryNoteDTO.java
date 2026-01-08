package com.ecommerce.dto;

import com.ecommerce.entity.OrderDeliveryNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryNoteDTO {
    
    private Long noteId;
    private String noteText;
    private String noteType;
    private String noteCategory;
    
    // Order information (if order-specific note)
    private Long orderId;
    private String orderNumber;
    
    // Delivery group information (if group-general note)
    private Long deliveryGroupId;
    private String deliveryGroupName;
    
    // Agent information
    private String agentId;
    private String agentName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert entity to DTO
     */
    public static OrderDeliveryNoteDTO fromEntity(OrderDeliveryNote note) {
        OrderDeliveryNoteDTOBuilder builder = OrderDeliveryNoteDTO.builder()
                .noteId(note.getNoteId())
                .noteText(note.getNoteText())
                .noteType(note.getNoteType().name())
                .noteCategory(note.getNoteCategory() != null ? note.getNoteCategory().name() : null)
                .agentId(note.getAgent().getId() != null ? note.getAgent().getId().toString() : null)
                .agentName(note.getAgent().getFirstName() + " " + note.getAgent().getLastName())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt());
        
        // Add order info if present (from shopOrder)
        if (note.getShopOrder() != null && note.getShopOrder().getOrder() != null) {
            builder.orderId(note.getShopOrder().getOrder().getOrderId())
                   .orderNumber(note.getShopOrder().getOrder().getOrderCode());
        }
        
        // Add delivery group info if present
        if (note.getDeliveryGroup() != null) {
            builder.deliveryGroupId(note.getDeliveryGroup().getDeliveryGroupId())
                   .deliveryGroupName(note.getDeliveryGroup().getDeliveryGroupName());
        }
        
        return builder.build();
    }
}
