package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_delivery_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "note_text", nullable = false, length = 2000, columnDefinition = "TEXT")
    private String noteText;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false)
    private NoteType noteType;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_category")
    private NoteCategory noteCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_group_id")
    private ReadyForDeliveryGroup deliveryGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateNote();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateNote();
    }

    /**
     * Validates that the note has either an order or delivery group
     */
    private void validateNote() {
        if (noteType == NoteType.ORDER_SPECIFIC && order == null) {
            throw new IllegalStateException("Order-specific note must have an associated order");
        }
        if (noteType == NoteType.GROUP_GENERAL && deliveryGroup == null) {
            throw new IllegalStateException("Group-general note must have an associated delivery group");
        }
    }

    /**
     * Enum for note type - determines if note is for order or group
     */
    public enum NoteType {
        ORDER_SPECIFIC,    // Note specific to a single order
        GROUP_GENERAL      // Note for the entire delivery group
    }

    /**
     * Enum for note category - helps categorize the type of note
     */
    public enum NoteCategory {
        TRAFFIC_DELAY,  
        CUSTOMER_UNAVAILABLE, 
        ADDRESS_ISSUE, 
        DELIVERY_INSTRUCTION,    
        WEATHER_CONDITION,       
        VEHICLE_ISSUE,           
        SUCCESSFUL_DELIVERY,     
        FAILED_DELIVERY,         
        GENERAL,                 
        OTHER                    
    }
}
