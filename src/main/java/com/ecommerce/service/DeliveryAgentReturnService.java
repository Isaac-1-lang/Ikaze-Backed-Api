package com.ecommerce.service;

import com.ecommerce.dto.ReturnRequestDTO;
import com.ecommerce.dto.DeliveryAgentReturnTableDTO;
import com.ecommerce.dto.DeliveryAgentReturnDetailsDTO;
import com.ecommerce.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DeliveryAgentReturnService {

    /**
     * Get return requests assigned to a delivery agent with pagination and filtering
     */
    Page<DeliveryAgentReturnTableDTO> getAssignedReturnRequests(
            UUID deliveryAgentId,
            Pageable pageable,
            String returnStatus,
            String deliveryStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String customerName,
            String orderNumber
    );

    /**
     * Get return request by ID if assigned to the delivery agent
     */
    ReturnRequestDTO getReturnRequestById(Long returnRequestId, UUID deliveryAgentId);

    /**
     * Get comprehensive return request details for delivery agent
     */
    DeliveryAgentReturnDetailsDTO getReturnRequestDetails(Long returnRequestId, UUID deliveryAgentId);

    /**
     * Update delivery status of a return request
     */
    ReturnRequestDTO updateDeliveryStatus(
            Long returnRequestId,
            UUID deliveryAgentId,
            ReturnRequest.DeliveryStatus deliveryStatus,
            String notes
    );

    /**
     * Get delivery agent statistics
     */
    DeliveryAgentStats getDeliveryAgentStats(UUID deliveryAgentId);

    /**
     * Statistics DTO for delivery agent
     */
    record DeliveryAgentStats(
            long totalAssigned,
            long pickupScheduled,
            long pickupInProgress,
            long pickupCompleted,
            long pickupFailed,
            double successRate
    ) {}
}
