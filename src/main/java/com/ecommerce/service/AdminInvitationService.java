package com.ecommerce.service;

import com.ecommerce.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface AdminInvitationService {

    // Create and manage invitations
    AdminInvitationDTO createInvitation(UUID vendorId, CreateAdminInvitationDTO createInvitationDTO);

    AdminInvitationDTO updateInvitation(UUID invitationId, UpdateAdminInvitationDTO updateInvitationDTO);

    boolean cancelInvitation(UUID invitationId);

    boolean deleteInvitation(UUID invitationId);

    boolean resendInvitation(UUID invitationId);

    // View invitations
    AdminInvitationDTO getInvitationById(UUID invitationId);

    AdminInvitationDTO getInvitationByToken(String invitationToken);

    Page<AdminInvitationDTO> getAllInvitations(Pageable pageable);

    Page<AdminInvitationDTO> searchInvitations(AdminInvitationSearchDTO searchDTO, Pageable pageable);

    Page<AdminInvitationDTO> getInvitationsByStatus(String status, Pageable pageable);

    Page<AdminInvitationDTO> getInvitationsByInviter(UUID inviterId, Pageable pageable);

    Page<AdminInvitationDTO> getInvitationsByRole(String role, Pageable pageable);

    Page<AdminInvitationDTO> getInvitationsByDepartment(String department, Pageable pageable);

    // Accept/Decline invitations
    AdminInvitationDTO acceptInvitation(AcceptInvitationDTO acceptInvitationDTO);

    boolean declineInvitation(String invitationToken);

    // Expiration management
    void markExpiredInvitations();

    void deleteExpiredInvitations();

    long getExpiredInvitationsCount();

    // Statistics and counts
    long getInvitationsCountByStatus(String status);

    long getInvitationsCountByInviter(UUID inviterId);

    long getInvitationsCountByRole(String role);

    long getInvitationsCountByDepartment(String department);

    // Validation methods
    boolean isInvitationValid(String invitationToken);

    boolean isInvitationExpired(String invitationToken);

    boolean canInvitationBeAccepted(String invitationToken);

    boolean canInvitationBeCancelled(UUID invitationId);

    // Bulk operations
    void cancelAllPendingInvitationsByEmail(String email);

    void resendAllPendingInvitationsByEmail(String email);

    /**
     * Vendor-only: release a shop member (EMPLOYEE/DELIVERY_AGENT) back to CUSTOMER.
     * Authorization is enforced in the controller via ShopAuthorizationService.
     */
    void releaseShopMember(UUID shopId, UUID userId);

    /**
     * Check if a user exists for the given invitation email.
     * Used to validate if "I already have an account" toggle should be enabled.
     */
    boolean doesUserExistForInvitation(String invitationToken);
}
