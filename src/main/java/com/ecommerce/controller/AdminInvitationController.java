package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.service.AdminInvitationService;
import com.ecommerce.ServiceImpl.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin-invitations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Invitation Management", description = "APIs for managing admin invitations")
@SecurityRequirement(name = "bearerAuth")
public class AdminInvitationController {

    private final AdminInvitationService adminInvitationService;
    private final JwtService jwtService;
    private final com.ecommerce.repository.UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new admin invitation", description = "Create an invitation for a new admin user. Only ADMIN role can create invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invitation created successfully", content = @Content(schema = @Schema(implementation = AdminInvitationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "409", description = "Conflict - User already has pending invitation"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createInvitation(
            @Valid @RequestBody CreateAdminInvitationDTO createInvitationDTO) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Creating admin invitation for email: {}", createInvitationDTO.getEmail());

            UUID adminId = getCurrentUserId();

            AdminInvitationDTO createdInvitation = adminInvitationService.createInvitation(adminId,
                    createInvitationDTO);

            response.put("success", true);
            response.put("message", "Admin invitation created successfully");
            response.put("data", createdInvitation);

            log.info("Admin invitation created successfully with ID: {}", createdInvitation.getInvitationId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for creating admin invitation: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (EntityNotFoundException e) {
            log.error("Admin not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error creating admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to create admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{invitationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an admin invitation", description = "Update an existing admin invitation. Only ADMIN role can update invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation updated successfully", content = @Content(schema = @Schema(implementation = AdminInvitationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Invitation cannot be updated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateInvitation(
            @PathVariable UUID invitationId,
            @Valid @RequestBody UpdateAdminInvitationDTO updateInvitationDTO) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Updating admin invitation with ID: {}", invitationId);

            AdminInvitationDTO updatedInvitation = adminInvitationService.updateInvitation(invitationId,
                    updateInvitationDTO);

            response.put("success", true);
            response.put("message", "Admin invitation updated successfully");
            response.put("data", updatedInvitation);

            log.info("Admin invitation updated successfully with ID: {}", invitationId);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request for updating admin invitation: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalStateException e) {
            log.error("Cannot update invitation: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error updating admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to update admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{invitationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an admin invitation", description = "Delete an admin invitation. Only ADMIN role can delete invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteInvitation(@PathVariable UUID invitationId) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Deleting admin invitation with ID: {}", invitationId);

            boolean deleted = adminInvitationService.deleteInvitation(invitationId);

            if (deleted) {
                response.put("success", true);
                response.put("message", "Admin invitation deleted successfully");

                log.info("Admin invitation deleted successfully with ID: {}", invitationId);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to delete admin invitation");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error deleting admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to delete admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{invitationId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel an admin invitation", description = "Cancel a pending admin invitation. Only ADMIN role can cancel invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Invitation cannot be cancelled"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> cancelInvitation(@PathVariable UUID invitationId) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Cancelling admin invitation with ID: {}", invitationId);

            boolean cancelled = adminInvitationService.cancelInvitation(invitationId);

            if (cancelled) {
                response.put("success", true);
                response.put("message", "Admin invitation cancelled successfully");

                log.info("Admin invitation cancelled successfully with ID: {}", invitationId);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to cancel admin invitation");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.error("Cannot cancel invitation: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error cancelling admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to cancel admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{invitationId}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resend an admin invitation", description = "Resend a pending admin invitation with a new token. Only ADMIN role can resend invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation resent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Invitation cannot be resent"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> resendInvitation(@PathVariable UUID invitationId) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Resending admin invitation with ID: {}", invitationId);

            boolean resent = adminInvitationService.resendInvitation(invitationId);

            if (resent) {
                response.put("success", true);
                response.put("message", "Admin invitation resent successfully");

                log.info("Admin invitation resent successfully with ID: {}", invitationId);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to resend admin invitation");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.error("Cannot resend invitation: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error resending admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to resend admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{invitationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get invitation by ID", description = "Get admin invitation details by ID. ADMIN and EMPLOYEE roles can view invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation retrieved successfully", content = @Content(schema = @Schema(implementation = AdminInvitationDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getInvitationById(@PathVariable UUID invitationId) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching admin invitation with ID: {}", invitationId);

            AdminInvitationDTO invitation = adminInvitationService.getInvitationById(invitationId);

            response.put("success", true);
            response.put("message", "Admin invitation retrieved successfully");
            response.put("data", invitation);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error fetching admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/token/{invitationToken}")
    @Operation(summary = "Get invitation by token", description = "Get admin invitation details by invitation token. Public endpoint for invitation validation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation retrieved successfully", content = @Content(schema = @Schema(implementation = AdminInvitationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getInvitationByToken(@PathVariable String invitationToken) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching admin invitation by token: {}", invitationToken);

            AdminInvitationDTO invitation = adminInvitationService.getInvitationByToken(invitationToken);

            response.put("success", true);
            response.put("message", "Admin invitation retrieved successfully");
            response.put("data", invitation);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Error fetching admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all invitations", description = "Get all admin invitations with pagination. ADMIN and EMPLOYEE roles can view invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAllInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching all admin invitations with pagination - page: {}, size: {}", page, size);

            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<AdminInvitationDTO> invitations = adminInvitationService.getAllInvitations(pageable);

            response.put("success", true);
            response.put("message", "Admin invitations retrieved successfully");
            response.put("data", invitations.getContent());
            response.put("pagination", Map.of(
                    "currentPage", invitations.getNumber(),
                    "totalPages", invitations.getTotalPages(),
                    "totalElements", invitations.getTotalElements(),
                    "size", invitations.getSize(),
                    "hasNext", invitations.hasNext(),
                    "hasPrevious", invitations.hasPrevious()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching admin invitations: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve admin invitations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Search invitations", description = "Search admin invitations with various criteria. ADMIN and EMPLOYEE roles can search invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> searchInvitations(
            @Valid @RequestBody AdminInvitationSearchDTO searchDTO,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Searching admin invitations with criteria");

            Pageable pageable = PageRequest.of(page, size);
            Page<AdminInvitationDTO> invitations = adminInvitationService.searchInvitations(searchDTO, pageable);

            response.put("success", true);
            response.put("message", "Search completed successfully");
            response.put("data", invitations.getContent());
            response.put("pagination", Map.of(
                    "currentPage", invitations.getNumber(),
                    "totalPages", invitations.getTotalPages(),
                    "totalElements", invitations.getTotalElements(),
                    "size", invitations.getSize(),
                    "hasNext", invitations.hasNext(),
                    "hasPrevious", invitations.hasPrevious()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search criteria: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error searching admin invitations: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to search admin invitations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get invitations by status", description = "Get admin invitations filtered by status. ADMIN and EMPLOYEE roles can view invitations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getInvitationsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching admin invitations by status: {}", status);

            Pageable pageable = PageRequest.of(page, size);
            Page<AdminInvitationDTO> invitations = adminInvitationService.getInvitationsByStatus(status, pageable);

            response.put("success", true);
            response.put("message", "Admin invitations retrieved successfully");
            response.put("data", invitations.getContent());
            response.put("pagination", Map.of(
                    "currentPage", invitations.getNumber(),
                    "totalPages", invitations.getTotalPages(),
                    "totalElements", invitations.getTotalElements(),
                    "size", invitations.getSize(),
                    "hasNext", invitations.hasNext(),
                    "hasPrevious", invitations.hasPrevious()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error fetching admin invitations by status: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve admin invitations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept invitation", description = "Accept an admin invitation and create/update user account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation accepted successfully", content = @Content(schema = @Schema(implementation = AdminInvitationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Invitation cannot be accepted"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> acceptInvitation(@Valid @RequestBody AcceptInvitationDTO acceptInvitationDTO) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Accepting admin invitation with token: {}", acceptInvitationDTO.getInvitationToken());

            AdminInvitationDTO acceptedInvitation = adminInvitationService.acceptInvitation(acceptInvitationDTO);

            response.put("success", true);
            response.put("message", "Admin invitation accepted successfully");
            response.put("data", acceptedInvitation);

            log.info("Admin invitation accepted successfully for email: {}", acceptedInvitation.getEmail());
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.error("Cannot accept invitation: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error accepting admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to accept admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/decline/{invitationToken}")
    @Operation(summary = "Decline invitation", description = "Decline an admin invitation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation declined successfully"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Invitation cannot be declined"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> declineInvitation(@PathVariable String invitationToken) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Declining admin invitation with token: {}", invitationToken);

            boolean declined = adminInvitationService.declineInvitation(invitationToken);

            if (declined) {
                response.put("success", true);
                response.put("message", "Admin invitation declined successfully");

                log.info("Admin invitation declined successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to decline admin invitation");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (EntityNotFoundException e) {
            log.error("Invitation not found: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.error("Cannot decline invitation: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Error declining admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to decline admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/validate/{invitationToken}")
    @Operation(summary = "Validate invitation", description = "Check if an invitation is valid and can be accepted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
            @ApiResponse(responseCode = "404", description = "Invitation not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> validateInvitation(@PathVariable String invitationToken) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Validating admin invitation with token: {}", invitationToken);

            boolean isValid = adminInvitationService.isInvitationValid(invitationToken);
            boolean isExpired = adminInvitationService.isInvitationExpired(invitationToken);
            boolean canBeAccepted = adminInvitationService.canInvitationBeAccepted(invitationToken);

            response.put("success", true);
            response.put("message", "Invitation validation completed");
            response.put("data", Map.of(
                    "isValid", isValid,
                    "isExpired", isExpired,
                    "canBeAccepted", canBeAccepted));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating admin invitation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to validate admin invitation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get invitation statistics", description = "Get statistics about admin invitations. ADMIN and EMPLOYEE roles can view statistics.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getInvitationStatistics() {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Fetching admin invitation statistics");

            long pendingCount = adminInvitationService.getInvitationsCountByStatus("PENDING");
            long acceptedCount = adminInvitationService.getInvitationsCountByStatus("ACCEPTED");
            long declinedCount = adminInvitationService.getInvitationsCountByStatus("DECLINED");
            long expiredCount = adminInvitationService.getInvitationsCountByStatus("EXPIRED");
            long cancelledCount = adminInvitationService.getInvitationsCountByStatus("CANCELLED");
            long expiredPendingCount = adminInvitationService.getExpiredInvitationsCount();

            response.put("success", true);
            response.put("message", "Admin invitation statistics retrieved successfully");
            response.put("data", Map.of(
                    "pending", pendingCount,
                    "accepted", acceptedCount,
                    "declined", declinedCount,
                    "expired", expiredCount,
                    "cancelled", cancelledCount,
                    "expiredPending", expiredPendingCount,
                    "total", pendingCount + acceptedCount + declinedCount + expiredCount + cancelledCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching admin invitation statistics: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve admin invitation statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/expired/mark")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark expired invitations", description = "Mark pending invitations that have expired. Only ADMIN role can perform this action.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Expired invitations marked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> markExpiredInvitations() {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Marking expired admin invitations");

            adminInvitationService.markExpiredInvitations();

            response.put("success", true);
            response.put("message", "Expired invitations marked successfully");

            log.info("Expired invitations marked successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error marking expired invitations: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to mark expired invitations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete expired invitations", description = "Delete expired invitations older than 30 days. Only ADMIN role can perform this action.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Expired invitations deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteExpiredInvitations() {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Deleting expired admin invitations");

            adminInvitationService.deleteExpiredInvitations();

            response.put("success", true);
            response.put("message", "Expired invitations deleted successfully");

            log.info("Expired invitations deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting expired invitations: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to delete expired invitations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof com.ecommerce.ServiceImpl.CustomUserDetails customUserDetails) {
                String email = customUserDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            if (principal instanceof com.ecommerce.entity.User user && user.getId() != null) {
                return user.getId();
            }

            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                return userRepository.findByUserEmail(email).map(com.ecommerce.entity.User::getId).orElse(null);
            }

            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return userRepository.findByUserEmail(name).map(com.ecommerce.entity.User::getId).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage(), e);
        }
        return null;
    }
}
