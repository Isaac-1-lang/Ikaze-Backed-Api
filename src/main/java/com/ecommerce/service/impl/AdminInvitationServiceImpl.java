package com.ecommerce.service.impl;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.dto.*;
import com.ecommerce.entity.AdminInvitation;
import com.ecommerce.entity.User;
import com.ecommerce.repository.AdminInvitationRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AdminInvitationService;
import com.ecommerce.service.EmailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminInvitationServiceImpl implements AdminInvitationService {

    private final AdminInvitationRepository adminInvitationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int INVITATION_EXPIRY_HOURS = 48;
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 32;

    @Override
    public AdminInvitationDTO createInvitation(UUID adminId, CreateAdminInvitationDTO createInvitationDTO) {
        log.info("Creating admin invitation for email: {}", createInvitationDTO.getEmail());

        // Validate admin exists
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found with ID: " + adminId));

        // Check if user already has a pending invitation
        if (adminInvitationRepository.existsByEmailAndStatus(createInvitationDTO.getEmail(),
                AdminInvitation.InvitationStatus.PENDING)) {
            throw new IllegalArgumentException(
                    "User already has a pending invitation: " + createInvitationDTO.getEmail());
        }

        // Validate role
        UserRole assignedRole;
        try {
            assignedRole = UserRole.valueOf(createInvitationDTO.getAssignedRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + createInvitationDTO.getAssignedRole());
        }

        // Generate unique invitation token
        String invitationToken = generateUniqueInvitationToken();

        // Create invitation
        AdminInvitation invitation = new AdminInvitation();
        invitation.setEmail(createInvitationDTO.getEmail());
        invitation.setFirstName(createInvitationDTO.getFirstName());
        invitation.setLastName(createInvitationDTO.getLastName());
        invitation.setAssignedRole(assignedRole);
        invitation.setInvitationToken(invitationToken);
        invitation.setInvitedBy(admin);
        invitation.setInvitationMessage(createInvitationDTO.getInvitationMessage());
        invitation.setDepartment(createInvitationDTO.getDepartment());
        invitation.setPosition(createInvitationDTO.getPosition());
        invitation.setPhoneNumber(createInvitationDTO.getPhoneNumber());
        invitation.setNotes(createInvitationDTO.getNotes());

        // Set expiration (48 hours from now)
        LocalDateTime expiration = createInvitationDTO.getExpiresAt() != null ? createInvitationDTO.getExpiresAt()
                : LocalDateTime.now().plusHours(INVITATION_EXPIRY_HOURS);
        invitation.setExpiresAt(expiration);

        AdminInvitation savedInvitation = adminInvitationRepository.save(invitation);
        log.info("Admin invitation created successfully with ID: {}", savedInvitation.getInvitationId());

        // Check if user already exists to determine email type
        User existingUser = userRepository.findByUserEmail(createInvitationDTO.getEmail()).orElse(null);

        try {
            if (existingUser != null) {
                // Send role update email to existing user
                emailService.sendExistingUserRoleUpdateEmail(
                        createInvitationDTO.getEmail(),
                        createInvitationDTO.getFirstName(),
                        createInvitationDTO.getLastName(),
                        admin.getFullName(),
                        assignedRole.name(),
                        createInvitationDTO.getInvitationMessage(),
                        createInvitationDTO.getDepartment(),
                        createInvitationDTO.getPosition());
                log.info("Role update email sent to existing user: {}", createInvitationDTO.getEmail());
            } else {
                // Send invitation email to new user
                emailService.sendNewUserInvitationEmail(
                        createInvitationDTO.getEmail(),
                        createInvitationDTO.getFirstName(),
                        createInvitationDTO.getLastName(),
                        invitationToken,
                        admin.getFullName(),
                        assignedRole.name(),
                        createInvitationDTO.getInvitationMessage(),
                        createInvitationDTO.getDepartment(),
                        createInvitationDTO.getPosition(),
                        savedInvitation.getExpiresAt().toString());
                log.info("Invitation email sent to new user: {}", createInvitationDTO.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to send email for invitation: {}", e.getMessage(), e);
            // Don't throw exception here - invitation was created successfully
            // Email failure shouldn't prevent invitation creation
        }

        return mapToDTO(savedInvitation);
    }

    @Override
    public AdminInvitationDTO updateInvitation(UUID invitationId, UpdateAdminInvitationDTO updateInvitationDTO) {
        log.info("Updating admin invitation with ID: {}", invitationId);

        AdminInvitation invitation = adminInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with ID: " + invitationId));

        // Only allow updates for pending invitations
        if (!invitation.isPending()) {
            throw new IllegalStateException("Cannot update invitation with status: " + invitation.getStatus());
        }

        // Update fields if provided
        if (updateInvitationDTO.getFirstName() != null) {
            invitation.setFirstName(updateInvitationDTO.getFirstName());
        }
        if (updateInvitationDTO.getLastName() != null) {
            invitation.setLastName(updateInvitationDTO.getLastName());
        }
        if (updateInvitationDTO.getAssignedRole() != null) {
            try {
                UserRole assignedRole = UserRole.valueOf(updateInvitationDTO.getAssignedRole().toUpperCase());
                invitation.setAssignedRole(assignedRole);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + updateInvitationDTO.getAssignedRole());
            }
        }
        if (updateInvitationDTO.getInvitationMessage() != null) {
            invitation.setInvitationMessage(updateInvitationDTO.getInvitationMessage());
        }
        if (updateInvitationDTO.getDepartment() != null) {
            invitation.setDepartment(updateInvitationDTO.getDepartment());
        }
        if (updateInvitationDTO.getPosition() != null) {
            invitation.setPosition(updateInvitationDTO.getPosition());
        }
        if (updateInvitationDTO.getPhoneNumber() != null) {
            invitation.setPhoneNumber(updateInvitationDTO.getPhoneNumber());
        }
        if (updateInvitationDTO.getNotes() != null) {
            invitation.setNotes(updateInvitationDTO.getNotes());
        }
        if (updateInvitationDTO.getExpiresAt() != null) {
            invitation.setExpiresAt(updateInvitationDTO.getExpiresAt());
        }

        AdminInvitation updatedInvitation = adminInvitationRepository.save(invitation);
        log.info("Admin invitation updated successfully with ID: {}", updatedInvitation.getInvitationId());

        return mapToDTO(updatedInvitation);
    }

    @Override
    public boolean cancelInvitation(UUID invitationId) {
        log.info("Cancelling admin invitation with ID: {}", invitationId);

        AdminInvitation invitation = adminInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with ID: " + invitationId));

        if (!invitation.canBeCancelled()) {
            throw new IllegalStateException("Invitation cannot be cancelled with status: " + invitation.getStatus());
        }

        invitation.cancel();
        adminInvitationRepository.save(invitation);
        log.info("Admin invitation cancelled successfully with ID: {}", invitationId);

        return true;
    }

    @Override
    public boolean deleteInvitation(UUID invitationId) {
        log.info("Deleting admin invitation with ID: {}", invitationId);

        if (!adminInvitationRepository.existsById(invitationId)) {
            throw new EntityNotFoundException("Invitation not found with ID: " + invitationId);
        }

        adminInvitationRepository.deleteById(invitationId);
        log.info("Admin invitation deleted successfully with ID: {}", invitationId);

        return true;
    }

    @Override
    public boolean resendInvitation(UUID invitationId) {
        log.info("Resending admin invitation with ID: {}", invitationId);

        AdminInvitation invitation = adminInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with ID: " + invitationId));

        if (!invitation.isPending()) {
            throw new IllegalStateException("Cannot resend invitation with status: " + invitation.getStatus());
        }

        // Generate new token and extend expiration
        String newToken = generateUniqueInvitationToken();
        LocalDateTime newExpiration = LocalDateTime.now().plusHours(INVITATION_EXPIRY_HOURS);

        invitation.setInvitationToken(newToken);
        invitation.setExpiresAt(newExpiration);

        adminInvitationRepository.save(invitation);
        log.info("Admin invitation resent successfully with ID: {}", invitationId);

        // Send resend email
        try {
            emailService.sendInvitationResendEmail(
                    invitation.getEmail(),
                    invitation.getFirstName(),
                    invitation.getLastName(),
                    newToken,
                    invitation.getInvitedBy().getFullName(),
                    invitation.getAssignedRole().name(),
                    invitation.getInvitationMessage(),
                    invitation.getDepartment(),
                    invitation.getPosition(),
                    newExpiration.toString());
            log.info("Invitation resend email sent to: {}", invitation.getEmail());
        } catch (Exception e) {
            log.error("Failed to send resend email for invitation: {}", e.getMessage(), e);
            // Don't throw exception here - invitation was resent successfully
            // Email failure shouldn't prevent invitation resend
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminInvitationDTO getInvitationById(UUID invitationId) {
        log.info("Fetching admin invitation with ID: {}", invitationId);

        AdminInvitation invitation = adminInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with ID: " + invitationId));

        return mapToDTO(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminInvitationDTO getInvitationByToken(String invitationToken) {
        log.info("Fetching admin invitation by token: {}", invitationToken);

        AdminInvitation invitation = adminInvitationRepository.findByInvitationToken(invitationToken)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with token: " + invitationToken));

        return mapToDTO(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminInvitationDTO> getAllInvitations(Pageable pageable) {
        log.info("Fetching all admin invitations with pagination");

        Page<AdminInvitation> invitations = adminInvitationRepository.findAll(pageable);
        return invitations.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminInvitationDTO> searchInvitations(AdminInvitationSearchDTO searchDTO, Pageable pageable) {
        log.info("Searching admin invitations with criteria");

        if (!searchDTO.hasAtLeastOneFilter()) {
            throw new IllegalArgumentException("At least one search criteria must be provided");
        }

        // For now, return all invitations since we need to implement
        // JpaSpecificationExecutor
        Page<AdminInvitation> invitations = adminInvitationRepository.findAll(pageable);
        return invitations.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminInvitationDTO> getInvitationsByStatus(String status, Pageable pageable) {
        log.info("Fetching admin invitations by status: {}", status);

        AdminInvitation.InvitationStatus invitationStatus;
        try {
            invitationStatus = AdminInvitation.InvitationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        Page<AdminInvitation> invitations = adminInvitationRepository.findByStatus(invitationStatus, pageable);
        return invitations.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminInvitationDTO> getInvitationsByInviter(UUID inviterId, Pageable pageable) {
        log.info("Fetching admin invitations by inviter: {}", inviterId);

        Page<AdminInvitation> invitations = adminInvitationRepository.findByInvitedBy_Id(inviterId, pageable);
        return invitations.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminInvitationDTO> getInvitationsByRole(String role, Pageable pageable) {
        log.info("Fetching admin invitations by role: {}", role);

        Page<AdminInvitation> invitations = adminInvitationRepository.findByAssignedRole(role, pageable);
        return invitations.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminInvitationDTO> getInvitationsByDepartment(String department, Pageable pageable) {
        log.info("Fetching admin invitations by department: {}", department);

        Page<AdminInvitation> invitations = adminInvitationRepository.findByDepartment(department, pageable);
        return invitations.map(this::mapToDTO);
    }

    @Override
    public AdminInvitationDTO acceptInvitation(AcceptInvitationDTO acceptInvitationDTO) {
        log.info("Accepting admin invitation with token: {}", acceptInvitationDTO.getInvitationToken());

        AdminInvitation invitation = adminInvitationRepository
                .findByInvitationToken(acceptInvitationDTO.getInvitationToken())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Invitation not found with token: " + acceptInvitationDTO.getInvitationToken()));

        if (!invitation.canBeAccepted()) {
            throw new IllegalStateException("Invitation cannot be accepted. Status: " + invitation.getStatus()
                    + ", Expired: " + invitation.isExpired());
        }

        // Check if user already exists
        User existingUser = userRepository.findByUserEmail(invitation.getEmail()).orElse(null);

        if (existingUser != null) {
            // Update existing user's role
            log.info("Updating existing user role for email: {}", invitation.getEmail());
            existingUser.setRole(invitation.getAssignedRole());
            if (acceptInvitationDTO.getPhoneNumber() != null
                    && !acceptInvitationDTO.getPhoneNumber().trim().isEmpty()) {
                existingUser.setPhoneNumber(acceptInvitationDTO.getPhoneNumber());
            }
            userRepository.save(existingUser);
            invitation.accept(existingUser);

            log.info("Existing user role updated successfully for email: {}", invitation.getEmail());
        } else {
            // Create new user
            log.info("Creating new user for email: {}", invitation.getEmail());
            User newUser = new User();
            newUser.setFirstName(invitation.getFirstName());
            newUser.setLastName(invitation.getLastName());
            newUser.setUserEmail(invitation.getEmail());
            newUser.setPassword(passwordEncoder.encode(acceptInvitationDTO.getPassword()));
            newUser.setRole(invitation.getAssignedRole());
            newUser.setPhoneNumber(acceptInvitationDTO.getPhoneNumber());
            newUser.setEmailVerified(true);
            newUser.setEnabled(true);

            User savedUser = userRepository.save(newUser);
            invitation.accept(savedUser);

            log.info("New user created successfully for email: {}", invitation.getEmail());
        }

        AdminInvitation savedInvitation = adminInvitationRepository.save(invitation);
        log.info("Admin invitation accepted successfully for email: {}", invitation.getEmail());

        return mapToDTO(savedInvitation);
    }

    @Override
    public boolean declineInvitation(String invitationToken) {
        log.info("Declining admin invitation with token: {}", invitationToken);

        AdminInvitation invitation = adminInvitationRepository.findByInvitationToken(invitationToken)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with token: " + invitationToken));

        if (!invitation.canBeAccepted()) {
            throw new IllegalStateException("Invitation cannot be declined. Status: " + invitation.getStatus());
        }

        invitation.decline();
        adminInvitationRepository.save(invitation);
        log.info("Admin invitation declined successfully for email: {}", invitation.getEmail());

        return true;
    }

    @Override
    public void markExpiredInvitations() {
        log.info("Marking expired invitations as expired");

        LocalDateTime now = LocalDateTime.now();
        adminInvitationRepository.markExpiredPendingInvitations(now);
        log.info("Expired invitations marked successfully");
    }

    @Override
    public void deleteExpiredInvitations() {
        log.info("Deleting expired invitations");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // Delete invitations expired more than 30 days
                                                                      // ago
        adminInvitationRepository.deleteExpiredInvitations(cutoffDate);
        log.info("Expired invitations deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public long getExpiredInvitationsCount() {
        return adminInvitationRepository.countExpiredPendingInvitations(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long getInvitationsCountByStatus(String status) {
        AdminInvitation.InvitationStatus invitationStatus;
        try {
            invitationStatus = AdminInvitation.InvitationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        return adminInvitationRepository.countByStatus(invitationStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public long getInvitationsCountByInviter(UUID inviterId) {
        return adminInvitationRepository.countByInvitedBy_Id(inviterId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getInvitationsCountByRole(String role) {
        return adminInvitationRepository.countByAssignedRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public long getInvitationsCountByDepartment(String department) {
        return adminInvitationRepository.countByDepartment(department);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInvitationValid(String invitationToken) {
        return adminInvitationRepository.findByInvitationToken(invitationToken)
                .map(AdminInvitation::canBeAccepted)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInvitationExpired(String invitationToken) {
        return adminInvitationRepository.findByInvitationToken(invitationToken)
                .map(AdminInvitation::isExpired)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canInvitationBeAccepted(String invitationToken) {
        return adminInvitationRepository.findByInvitationToken(invitationToken)
                .map(AdminInvitation::canBeAccepted)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canInvitationBeCancelled(UUID invitationId) {
        return adminInvitationRepository.findById(invitationId)
                .map(AdminInvitation::canBeCancelled)
                .orElse(false);
    }

    @Override
    public void cancelAllPendingInvitationsByEmail(String email) {
        log.info("Cancelling all pending invitations for email: {}", email);

        adminInvitationRepository.findByEmail(email).stream()
                .filter(AdminInvitation::isPending)
                .forEach(invitation -> {
                    invitation.cancel();
                    adminInvitationRepository.save(invitation);
                });

        log.info("All pending invitations cancelled for email: {}", email);
    }

    @Override
    public void resendAllPendingInvitationsByEmail(String email) {
        log.info("Resending all pending invitations for email: {}", email);

        adminInvitationRepository.findByEmail(email).stream()
                .filter(AdminInvitation::isPending)
                .forEach(invitation -> {
                    invitation.setInvitationToken(generateUniqueInvitationToken());
                    invitation.setExpiresAt(LocalDateTime.now().plusHours(INVITATION_EXPIRY_HOURS));
                    adminInvitationRepository.save(invitation);
                });

        log.info("All pending invitations resent for email: {}", email);
    }

    private String generateUniqueInvitationToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);

        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
        }

        String generatedToken = token.toString();

        // Ensure uniqueness
        while (adminInvitationRepository.existsByInvitationToken(generatedToken)) {
            for (int i = 0; i < TOKEN_LENGTH; i++) {
                token.setCharAt(i, TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
            }
            generatedToken = token.toString();
        }

        return generatedToken;
    }

    private AdminInvitationDTO mapToDTO(AdminInvitation invitation) {
        return AdminInvitationDTO.builder()
                .invitationId(invitation.getInvitationId())
                .email(invitation.getEmail())
                .firstName(invitation.getFirstName())
                .lastName(invitation.getLastName())
                .fullName(invitation.getFullName())
                .assignedRole(invitation.getAssignedRole().name())
                .invitationToken(invitation.getInvitationToken())
                .status(invitation.getStatus().name())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .createdAt(invitation.getCreatedAt())
                .updatedAt(invitation.getUpdatedAt())
                .invitedById(invitation.getInvitedBy().getId())
                .invitedByName(invitation.getInvitedBy().getFullName())
                .invitedByEmail(invitation.getInvitedBy().getUserEmail())
                .acceptedById(invitation.getAcceptedBy() != null ? invitation.getAcceptedBy().getId() : null)
                .acceptedByName(invitation.getAcceptedBy() != null ? invitation.getAcceptedBy().getFullName() : null)
                .acceptedByEmail(invitation.getAcceptedBy() != null ? invitation.getAcceptedBy().getUserEmail() : null)
                .invitationMessage(invitation.getInvitationMessage())
                .department(invitation.getDepartment())
                .position(invitation.getPosition())
                .phoneNumber(invitation.getPhoneNumber())
                .notes(invitation.getNotes())
                .isExpired(invitation.isExpired())
                .canBeAccepted(invitation.canBeAccepted())
                .canBeCancelled(invitation.canBeCancelled())
                .build();
    }
}
