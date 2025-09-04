package com.ecommerce.repository;

import com.ecommerce.entity.DeliveryAssignment;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.Enum.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    List<DeliveryAssignment> findByAgent(User agent);

    List<DeliveryAssignment> findByStatus(DeliveryStatus status);

    Optional<DeliveryAssignment> findByOrder(Order order);

    List<DeliveryAssignment> findByOrderOrderId(Long orderId);

    List<DeliveryAssignment> findByAgentId(java.util.UUID agentId);
}
