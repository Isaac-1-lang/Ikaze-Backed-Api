package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.DeliveryAssignmentDTO;
import com.ecommerce.entity.DeliveryAssignment;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.Enum.DeliveryStatus;
import com.ecommerce.repository.DeliveryAssignmentRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.DeliveryAssignmentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryAssignmentServiceImpl implements DeliveryAssignmentService {
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public DeliveryAssignmentDTO assignAgentToOrder(Long orderId, java.util.UUID agentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found"));
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrder(order);
        assignment.setAgent(agent);
        assignment.setStatus(DeliveryStatus.PENDING);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        deliveryAssignmentRepository.save(assignment);
        return toDTO(assignment);
    }

    @Override
    public DeliveryAssignmentDTO changeAgentForOrder(Long orderId, java.util.UUID newAgentId) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderOrderId(orderId)
                .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
        User newAgent = userRepository.findById(newAgentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found"));
        assignment.setAgent(newAgent);
        assignment.setUpdatedAt(LocalDateTime.now());
        deliveryAssignmentRepository.save(assignment);
        return toDTO(assignment);
    }

    @Override
    public void unassignAgentFromOrder(Long orderId) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderOrderId(orderId)
                .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
        deliveryAssignmentRepository.delete(assignment);
    }

    @Override
    public void cancelAssignment(Long assignmentId) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
        assignment.setStatus(DeliveryStatus.CANCELED);
        assignment.setUpdatedAt(LocalDateTime.now());
        deliveryAssignmentRepository.save(assignment);
    }

    @Override
    public DeliveryAssignmentDTO updateDeliveryStatus(Long assignmentId, DeliveryStatus status) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
        assignment.setStatus(status);
        assignment.setUpdatedAt(LocalDateTime.now());
        deliveryAssignmentRepository.save(assignment);
        return toDTO(assignment);
    }

    @Override
    public List<DeliveryAssignmentDTO> getAllAssignments() {
        return deliveryAssignmentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DeliveryAssignmentDTO getAssignmentByOrderId(Long orderId) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderOrderId(orderId)
                .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
        return toDTO(assignment);
    }

    @Override
    public List<DeliveryAssignmentDTO> getAssignmentsByAgentId(java.util.UUID agentId) {
        return deliveryAssignmentRepository.findByAgentId(agentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private DeliveryAssignmentDTO toDTO(DeliveryAssignment assignment) {
        DeliveryAssignmentDTO dto = new DeliveryAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setOrderId(assignment.getOrder().getOrderId()); // Ensure getOrderId() returns UUID
        dto.setAgentId(assignment.getAgent().getId()); // Ensure getId() returns UUID
        dto.setStatus(assignment.getStatus());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setUpdatedAt(assignment.getUpdatedAt());
        return dto;
    }
}
