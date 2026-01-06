package com.ecommerce.repository;

import com.ecommerce.entity.AdminInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, UUID> {

    // Find by invitation token
    Optional<AdminInvitation> findByInvitationToken(String invitationToken);

    // Find by invitation token + shop
    Optional<AdminInvitation> findByInvitationTokenAndShopShopId(String invitationToken, UUID shopId);

    // Find by email
    List<AdminInvitation> findByEmail(String email);

    // Find by status
    Page<AdminInvitation> findByStatus(AdminInvitation.InvitationStatus status, Pageable pageable);

    // Find by status + shop
    Page<AdminInvitation> findByStatusAndShopShopId(AdminInvitation.InvitationStatus status, UUID shopId, Pageable pageable);

    // Find by invited by user
    Page<AdminInvitation> findByInvitedBy_Id(UUID invitedById, Pageable pageable);

    // Find by invited by user + shop
    Page<AdminInvitation> findByInvitedBy_IdAndShopShopId(UUID invitedById, UUID shopId, Pageable pageable);

    // Find by assigned role
    Page<AdminInvitation> findByAssignedRole(String assignedRole, Pageable pageable);

    // Find by assigned role + shop
    Page<AdminInvitation> findByAssignedRoleAndShopShopId(String assignedRole, UUID shopId, Pageable pageable);

    // Find by department
    Page<AdminInvitation> findByDepartment(String department, Pageable pageable);

    // Find by department + shop
    Page<AdminInvitation> findByDepartmentAndShopShopId(String department, UUID shopId, Pageable pageable);

    // Find pending invitations that are expired
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.status = 'PENDING' AND ai.expiresAt < :now")
    List<AdminInvitation> findExpiredPendingInvitations(@Param("now") LocalDateTime now);

    // Find by email and status
    Page<AdminInvitation> findByEmailAndStatus(String email, AdminInvitation.InvitationStatus status,
            Pageable pageable);

    // Find by email and status + shop
    Page<AdminInvitation> findByEmailAndStatusAndShopShopId(String email, AdminInvitation.InvitationStatus status, UUID shopId,
            Pageable pageable);

    // Find by invited by and status
    Page<AdminInvitation> findByInvitedBy_IdAndStatus(UUID invitedById, AdminInvitation.InvitationStatus status,
            Pageable pageable);

    Page<AdminInvitation> findByInvitedBy_IdAndStatusAndShopShopId(UUID invitedById, AdminInvitation.InvitationStatus status, UUID shopId,
            Pageable pageable);

    // Find by assigned role and status
    Page<AdminInvitation> findByAssignedRoleAndStatus(String assignedRole, AdminInvitation.InvitationStatus status,
            Pageable pageable);

    Page<AdminInvitation> findByAssignedRoleAndStatusAndShopShopId(String assignedRole, AdminInvitation.InvitationStatus status, UUID shopId,
            Pageable pageable);

    // Find by department and status
    Page<AdminInvitation> findByDepartmentAndStatus(String department, AdminInvitation.InvitationStatus status,
            Pageable pageable);

    Page<AdminInvitation> findByDepartmentAndStatusAndShopShopId(String department, AdminInvitation.InvitationStatus status, UUID shopId,
            Pageable pageable);

    // Find by email containing (partial match)
    Page<AdminInvitation> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<AdminInvitation> findByEmailContainingIgnoreCaseAndShopShopId(String email, UUID shopId, Pageable pageable);

    // Find by first name or last name containing (partial match)
    @Query("SELECT ai FROM AdminInvitation ai WHERE LOWER(ai.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(ai.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<AdminInvitation> findByFirstNameOrLastNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    // Find by invitation message containing (partial match)
    Page<AdminInvitation> findByInvitationMessageContainingIgnoreCase(String message, Pageable pageable);

    Page<AdminInvitation> findByInvitationMessageContainingIgnoreCaseAndShopShopId(String message, UUID shopId, Pageable pageable);

    // Find by position containing (partial match)
    Page<AdminInvitation> findByPositionContainingIgnoreCase(String position, Pageable pageable);

    Page<AdminInvitation> findByPositionContainingIgnoreCaseAndShopShopId(String position, UUID shopId, Pageable pageable);

    // Find by phone number
    Page<AdminInvitation> findByPhoneNumber(String phoneNumber, Pageable pageable);

    Page<AdminInvitation> findByPhoneNumberAndShopShopId(String phoneNumber, UUID shopId, Pageable pageable);

    // Find by phone number containing (partial match)
    Page<AdminInvitation> findByPhoneNumberContaining(String phoneNumber, Pageable pageable);

    Page<AdminInvitation> findByPhoneNumberContainingAndShopShopId(String phoneNumber, UUID shopId, Pageable pageable);

    // Find by notes containing (partial match)
    Page<AdminInvitation> findByNotesContainingIgnoreCase(String notes, Pageable pageable);

    Page<AdminInvitation> findByNotesContainingIgnoreCaseAndShopShopId(String notes, UUID shopId, Pageable pageable);

    // Find by created date range
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.createdAt BETWEEN :startDate AND :endDate")
    Page<AdminInvitation> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // Find by expiration date range
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.expiresAt BETWEEN :startDate AND :endDate")
    Page<AdminInvitation> findByExpiresAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // Find by accepted date range
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.acceptedAt BETWEEN :startDate AND :endDate")
    Page<AdminInvitation> findByAcceptedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // Count by status
    long countByStatus(AdminInvitation.InvitationStatus status);

    // Count by invited by user
    long countByInvitedBy_Id(UUID invitedById);

    // Count by assigned role
    long countByAssignedRole(String assignedRole);

    // Count by department
    long countByDepartment(String department);

    // Count pending invitations that are expired
    @Query("SELECT COUNT(ai) FROM AdminInvitation ai WHERE ai.status = 'PENDING' AND ai.expiresAt < :now")
    long countExpiredPendingInvitations(@Param("now") LocalDateTime now);

    // Delete expired invitations
    @Modifying
    @Query("DELETE FROM AdminInvitation ai WHERE ai.status = 'EXPIRED' AND ai.expiresAt < :cutoffDate")
    void deleteExpiredInvitations(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Mark expired pending invitations as expired
    @Modifying
    @Query("UPDATE AdminInvitation ai SET ai.status = 'EXPIRED' WHERE ai.status = 'PENDING' AND ai.expiresAt < :now")
    void markExpiredPendingInvitations(@Param("now") LocalDateTime now);

    // Check if email has pending invitation
    boolean existsByEmailAndStatus(String email, AdminInvitation.InvitationStatus status);

    boolean existsByEmailAndStatusAndShopShopId(String email, AdminInvitation.InvitationStatus status, UUID shopId);

    // Check if invitation token exists
    boolean existsByInvitationToken(String invitationToken);

    // Find accepted invitations by user (acceptedBy)
    @Query("SELECT ai FROM AdminInvitation ai WHERE ai.acceptedBy.id = :userId AND ai.status = 'ACCEPTED' ORDER BY ai.acceptedAt DESC")
    List<AdminInvitation> findAcceptedInvitationsByUser(@Param("userId") UUID userId);
}
