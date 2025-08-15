package com.ecommerce.entity;

import com.ecommerce.Enum.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "admin_invitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID invitationId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false)
    private String email;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole assignedRole;

    @Column(name = "invitation_token", unique = true, nullable = false)
    private String invitationToken;

    @NotNull(message = "Invitation status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private User acceptedBy;

    @Column(name = "invitation_message")
    private String invitationMessage;

    @Column(name = "department")
    private String department;

    @Column(name = "position")
    private String position;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "notes")
    private String notes;

    public enum InvitationStatus {
        PENDING, // Invitation sent, waiting for response
        ACCEPTED, // Invitation accepted and user account created
        DECLINED, // Invitation declined by invitee
        EXPIRED, // Invitation expired
        CANCELLED // Invitation cancelled by admin
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(48);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canBeAccepted() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    public boolean canBeCancelled() {
        return status == InvitationStatus.PENDING;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void accept(User acceptedByUser) {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
        this.acceptedBy = acceptedByUser;
    }

    public void decline() {
        this.status = InvitationStatus.DECLINED;
    }

    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
    }

    public void markAsExpired() {
        this.status = InvitationStatus.EXPIRED;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean isDeclined() {
        return status == InvitationStatus.DECLINED;
    }

    public boolean isCancelled() {
        return status == InvitationStatus.CANCELLED;
    }

    public boolean isExpiredStatus() {
        return status == InvitationStatus.EXPIRED;
    }
}
