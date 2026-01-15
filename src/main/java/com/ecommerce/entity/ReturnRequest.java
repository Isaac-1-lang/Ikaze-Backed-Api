package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
@Data
@ToString(exclude = { "returnMedia", "returnItems", "returnAppeal", "shopOrder", "customer" })
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "returnMedia", "returnItems", "returnAppeal", "shopOrder", "customer" })
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Shop Order ID is required")
    @Column(name = "shop_order_id", nullable = false)
    private Long shopOrderId;

    @Column(name = "customer_id", nullable = true)
    private UUID customerId;

    @NotBlank(message = "Return reason is required")
    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatus status = ReturnStatus.PENDING;

    @NotNull(message = "Submitted date is required")
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "decision_notes", length = 1000)
    private String decisionNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Delivery Assignment Fields
    @Column(name = "delivery_agent_id")
    private UUID deliveryAgentId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "pickup_scheduled_at")
    private LocalDateTime pickupScheduledAt;

    @Column(name = "pickup_started_at")
    private LocalDateTime pickupStartedAt;

    @Column(name = "pickup_completed_at")
    private LocalDateTime pickupCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus deliveryStatus = DeliveryStatus.NOT_ASSIGNED;

    @Column(name = "delivery_notes", length = 1000)
    private String deliveryNotes;

    @Column(name = "estimated_pickup_time")
    private LocalDateTime estimatedPickupTime;

    @Column(name = "actual_pickup_time")
    private LocalDateTime actualPickupTime;

    // Refund tracking fields
    @Column(name = "refund_processed")
    private Boolean refundProcessed = false;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_processed_at")
    private LocalDateTime refundProcessedAt;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnMedia> returnMedia = new ArrayList<>();

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnItem> returnItems = new ArrayList<>();

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReturnAppeal returnAppeal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_order_id", insertable = false, updatable = false)
    private ShopOrder shopOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_agent_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User deliveryAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User assignedByUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for return request status
     */
    public enum ReturnStatus {
        PENDING,
        APPROVED,
        DENIED,
        COMPLETED
    }

    /**
     * Enum for delivery status
     */
    public enum DeliveryStatus {
        NOT_ASSIGNED, // No delivery agent assigned yet
        ASSIGNED, // Assigned to delivery agent but not started
        PICKUP_SCHEDULED, // Pickup time scheduled
        PICKUP_IN_PROGRESS, // Delivery agent is on the way/picking up
        PICKUP_COMPLETED, // Items picked up successfully
        PICKUP_FAILED, // Pickup attempt failed
        CANCELLED // Assignment cancelled
    }

    /**
     * Helper method to check if return request can be appealed
     */
    public boolean canBeAppealed() {
        return status == ReturnStatus.DENIED && returnAppeal == null;
    }

    /**
     * Helper method to approve the return request
     */
    public void approve(String decisionNotes) {
        this.status = ReturnStatus.APPROVED;
        this.decisionAt = LocalDateTime.now();
        this.decisionNotes = decisionNotes;
        // Initialize delivery status when approved
        if (this.deliveryStatus == null) {
            this.deliveryStatus = DeliveryStatus.NOT_ASSIGNED;
        }
    }

    /**
     * Helper method to deny the return request
     */
    public void deny(String decisionNotes) {
        this.status = ReturnStatus.DENIED;
        this.decisionAt = LocalDateTime.now();
        this.decisionNotes = decisionNotes;
    }

    // Delivery Assignment Helper Methods

    /**
     * Assigns a delivery agent to this return request
     */
    public void assignDeliveryAgent(UUID deliveryAgentId, UUID assignedBy, String notes) {
        this.deliveryAgentId = deliveryAgentId;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
        this.deliveryStatus = DeliveryStatus.ASSIGNED;
        this.deliveryNotes = notes;
    }

    /**
     * Unassigns the delivery agent from this return request
     */
    public void unassignDeliveryAgent(String reason) {
        this.deliveryAgentId = null;
        this.assignedBy = null;
        this.assignedAt = null;
        this.deliveryStatus = DeliveryStatus.NOT_ASSIGNED;
        this.deliveryNotes = reason;
        this.pickupScheduledAt = null;
        this.estimatedPickupTime = null;
    }

    /**
     * Schedules pickup for this return request
     */
    public void schedulePickup(LocalDateTime scheduledTime, LocalDateTime estimatedTime) {
        if (this.deliveryStatus != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Cannot schedule pickup for return request that is not assigned to a delivery agent");
        }
        this.pickupScheduledAt = scheduledTime;
        this.estimatedPickupTime = estimatedTime;
        this.deliveryStatus = DeliveryStatus.PICKUP_SCHEDULED;
    }

    /**
     * Starts the pickup process
     */
    public void startPickup() {
        if (this.deliveryStatus != DeliveryStatus.PICKUP_SCHEDULED && this.deliveryStatus != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot start pickup for return request in status: " + this.deliveryStatus);
        }
        this.pickupStartedAt = LocalDateTime.now();
        this.deliveryStatus = DeliveryStatus.PICKUP_IN_PROGRESS;
    }

    /**
     * Completes the pickup process successfully
     */
    public void completePickup(String notes) {
        if (this.deliveryStatus != DeliveryStatus.PICKUP_IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete pickup for return request not in progress");
        }
        this.pickupCompletedAt = LocalDateTime.now();
        this.actualPickupTime = LocalDateTime.now();
        this.deliveryStatus = DeliveryStatus.PICKUP_COMPLETED;
        this.deliveryNotes = notes;
    }

    /**
     * Marks pickup as failed
     */
    public void failPickup(String reason) {
        if (this.deliveryStatus != DeliveryStatus.PICKUP_IN_PROGRESS) {
            throw new IllegalStateException("Cannot fail pickup for return request not in progress");
        }
        this.deliveryStatus = DeliveryStatus.PICKUP_FAILED;
        this.deliveryNotes = reason;
    }

    /**
     * Cancels the delivery assignment
     */
    public void cancelDeliveryAssignment(String reason) {
        this.deliveryStatus = DeliveryStatus.CANCELLED;
        this.deliveryNotes = reason;
    }

    /**
     * Checks if return request can be assigned to a delivery agent
     */
    public boolean canBeAssignedToDeliveryAgent() {
        return this.status == ReturnStatus.APPROVED &&
                (this.deliveryStatus == null || // Handle legacy data with null delivery status
                        this.deliveryStatus == DeliveryStatus.NOT_ASSIGNED ||
                        this.deliveryStatus == DeliveryStatus.CANCELLED ||
                        this.deliveryStatus == DeliveryStatus.PICKUP_FAILED);
    }

    /**
     * Checks if return request is assigned to a delivery agent
     */
    public boolean isAssignedToDeliveryAgent() {
        return this.deliveryAgentId != null &&
                this.deliveryStatus != DeliveryStatus.NOT_ASSIGNED &&
                this.deliveryStatus != DeliveryStatus.CANCELLED;
    }

    /**
     * Checks if pickup is in progress or completed
     */
    public boolean isPickupInProgressOrCompleted() {
        return this.deliveryStatus == DeliveryStatus.PICKUP_IN_PROGRESS ||
                this.deliveryStatus == DeliveryStatus.PICKUP_COMPLETED;
    }

    /**
     * Gets the current delivery status display name
     */
    public String getDeliveryStatusDisplayName() {
        if (deliveryStatus == null)
            return "Not Assigned";

        return switch (deliveryStatus) {
            case NOT_ASSIGNED -> "Not Assigned";
            case ASSIGNED -> "Assigned";
            case PICKUP_SCHEDULED -> "Pickup Scheduled";
            case PICKUP_IN_PROGRESS -> "Pickup In Progress";
            case PICKUP_COMPLETED -> "Pickup Completed";
            case PICKUP_FAILED -> "Pickup Failed";
            case CANCELLED -> "Assignment Cancelled";
        };
    }
}
