package com.ecommerce.dto;

import com.ecommerce.Enum.DeliveryStatus;
import java.time.LocalDateTime;

public class DeliveryAssignmentDTO {
    private Long id;
    private Long orderId;
    private java.util.UUID agentId;
    private DeliveryStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime updatedAt;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public java.util.UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(java.util.UUID agentId) {
        this.agentId = agentId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
