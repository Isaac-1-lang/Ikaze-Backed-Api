package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// Let's remove all the comments

@Entity
@Table(name = "order_activity_logs", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "actor_type", length = 50)
    private String actorType; 

    @Column(name = "actor_id", length = 100)
    private String actorId; 

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType; 

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public enum ActivityType {
        ORDER_PLACED,
        PAYMENT_PENDING,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        ORDER_CONFIRMED,
        ORDER_PROCESSING,
        READY_FOR_DELIVERY,
        
        // Delivery Group & Assignment
        ADDED_TO_DELIVERY_GROUP,
        REMOVED_FROM_DELIVERY_GROUP,
        DELIVERY_AGENT_ASSIGNED,
        DELIVERY_AGENT_CHANGED,
        
        // Delivery Process
        DELIVERY_STARTED,
        OUT_FOR_DELIVERY,
        DELIVERY_NOTE_ADDED,
        DELIVERY_ATTEMPTED,
        DELIVERY_FAILED,
        DELIVERY_COMPLETED,
        
        // Order Status Changes
        ORDER_CANCELLED,
        ORDER_ON_HOLD,
        ORDER_RESUMED,
        
        RETURN_REQUESTED,
        RETURN_APPROVED,
        RETURN_DENIED,
        RETURN_RECEIVED,
        RETURN_INSPECTED,
        REFUND_INITIATED,
        REFUND_COMPLETED,
        
        // Appeals
        APPEAL_SUBMITTED,
        APPEAL_APPROVED,
        APPEAL_DENIED,
        APPEAL_ESCALATED,
        
        ADMIN_NOTE_ADDED,
        CUSTOMER_NOTE_ADDED,
        SYSTEM_NOTE_ADDED,
        
        // Other Events
        ORDER_UPDATED,
        TRACKING_INFO_UPDATED,
        EXCEPTION_OCCURRED
    }

    /**
     * Builder pattern for easy creation
     */
    public static OrderActivityLogBuilder builder() {
        return new OrderActivityLogBuilder();
    }

    public static class OrderActivityLogBuilder {
        private Long orderId;
        private ActivityType activityType;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private String actorType;
        private String actorId;
        private String actorName;
        private String metadata;
        private String referenceId;
        private String referenceType;

        public OrderActivityLogBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderActivityLogBuilder activityType(ActivityType activityType) {
            this.activityType = activityType;
            return this;
        }

        public OrderActivityLogBuilder title(String title) {
            this.title = title;
            return this;
        }

        public OrderActivityLogBuilder description(String description) {
            this.description = description;
            return this;
        }

        public OrderActivityLogBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public OrderActivityLogBuilder actorType(String actorType) {
            this.actorType = actorType;
            return this;
        }

        public OrderActivityLogBuilder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public OrderActivityLogBuilder actorName(String actorName) {
            this.actorName = actorName;
            return this;
        }

        public OrderActivityLogBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public OrderActivityLogBuilder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public OrderActivityLogBuilder referenceType(String referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        public OrderActivityLog build() {
            OrderActivityLog log = new OrderActivityLog();
            log.orderId = this.orderId;
            log.activityType = this.activityType;
            log.title = this.title;
            log.description = this.description;
            log.timestamp = this.timestamp != null ? this.timestamp : LocalDateTime.now();
            log.actorType = this.actorType;
            log.actorId = this.actorId;
            log.actorName = this.actorName;
            log.metadata = this.metadata;
            log.referenceId = this.referenceId;
            log.referenceType = this.referenceType;
            return log;
        }
    }
}
