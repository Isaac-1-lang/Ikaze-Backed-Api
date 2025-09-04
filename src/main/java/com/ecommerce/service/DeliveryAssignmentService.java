package com.ecommerce.service;

import com.ecommerce.dto.DeliveryAssignmentDTO;
import com.ecommerce.entity.DeliveryAssignment;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.Enum.DeliveryStatus;
import java.util.List;

public interface DeliveryAssignmentService {
    DeliveryAssignmentDTO assignAgentToOrder(Long orderId, java.util.UUID agentId);

    DeliveryAssignmentDTO changeAgentForOrder(Long orderId, java.util.UUID newAgentId);

    void unassignAgentFromOrder(Long orderId);

    void cancelAssignment(Long assignmentId);

    DeliveryAssignmentDTO updateDeliveryStatus(Long assignmentId, DeliveryStatus status);

    List<DeliveryAssignmentDTO> getAllAssignments();

    DeliveryAssignmentDTO getAssignmentByOrderId(Long orderId);

    List<DeliveryAssignmentDTO> getAssignmentsByAgentId(java.util.UUID agentId);
}
