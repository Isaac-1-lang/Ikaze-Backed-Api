package com.ecommerce.dto;

import com.ecommerce.entity.ReturnRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Streamlined DTO for delivery agent returns table display
 * Contains only the essential fields needed for the table view
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentReturnTableDTO {
    
    // Return request basic info
    private Long id;
    private String reason;
    private ReturnRequest.ReturnStatus status;
    private ReturnRequest.DeliveryStatus deliveryStatus;
    private LocalDateTime createdAt;
    
    // Order information
    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;
    
    // Customer information (from OrderCustomerInfo)
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    
    // Delivery agent information
    private UUID deliveryAgentId;
    private String deliveryAgentName;
}
