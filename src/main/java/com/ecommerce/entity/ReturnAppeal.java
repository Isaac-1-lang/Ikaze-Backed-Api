package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_appeals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"returnRequest", "appealMedia"})
public class ReturnAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Return request ID is required")
    @Column(name = "return_request_id", nullable = false)
    private Long returnRequestId;

    @NotNull(message = "Appeal level is required")
    @Column(name = "level", nullable = false)
    private Integer level = 1;  

    @Column(name = "appeal_text", length = 1000)
    private String appealText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppealStatus status = AppealStatus.PENDING;

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

    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", insertable = false, updatable = false)
    private ReturnRequest returnRequest;

    @OneToMany(mappedBy = "returnAppeal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AppealMedia> appealMedia = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (level == null) {
            level = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for appeal status
     */
    public enum AppealStatus {
        PENDING,
        APPROVED,
        DENIED
    }

    /**
     * Helper method to approve the appeal
     */
    public void approve(String decisionNotes) {
        this.status = AppealStatus.APPROVED;
        this.decisionAt = LocalDateTime.now();
        this.decisionNotes = decisionNotes;
    }

    /**
     * Helper method to deny the appeal
     */
    public void deny(String decisionNotes) {
        this.status = AppealStatus.DENIED;
        this.decisionAt = LocalDateTime.now();
        this.decisionNotes = decisionNotes;
    }

    /**
     * Helper method to check if appeal is still pending
     */
    public boolean isPending() {
        return status == AppealStatus.PENDING;
    }

    /**
     * Helper method to check if appeal was approved
     */
    public boolean isApproved() {
        return status == AppealStatus.APPROVED;
    }

    /**
     * Helper method to check if appeal was denied
     */
    public boolean isDenied() {
        return status == AppealStatus.DENIED;
    }
}
