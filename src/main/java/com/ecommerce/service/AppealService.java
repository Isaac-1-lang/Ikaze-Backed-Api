package com.ecommerce.service;

import com.ecommerce.Exception.ReturnException;
import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppealService {

    private final ReturnAppealRepository returnAppealRepository;
    private final AppealMediaRepository appealMediaRepository;
    private final ReturnRequestRepository returnRequestRepository;
    
    // Notification and audit services
    private final NotificationService notificationService;

    /**
     * Submit an appeal for a denied return request
     */
    public ReturnAppealDTO submitAppeal(SubmitAppealDTO submitDTO) {
        log.info("Processing appeal submission for return request {} by customer {}", 
                submitDTO.getReturnRequestId(), submitDTO.getCustomerId());
        
        // Validate return request exists and can be appealed
        ReturnRequest returnRequest = validateReturnForAppeal(submitDTO.getReturnRequestId(), submitDTO.getCustomerId());
        
        // Check if appeal already exists
        if (returnAppealRepository.existsByReturnRequestId(submitDTO.getReturnRequestId())) {
            throw new ReturnException.AppealAlreadyExistsException(
                "Appeal already exists for return request " + submitDTO.getReturnRequestId());
        }
        
        // Validate media attachments (at least one required)
        if (submitDTO.getMediaFiles() == null || submitDTO.getMediaFiles().isEmpty()) {
            throw new ReturnException.AppealNotAllowedException(
                "At least one image or video is required for appeal submission");
        }
        
        // Create appeal
        ReturnAppeal appeal = new ReturnAppeal();
        appeal.setReturnRequestId(submitDTO.getReturnRequestId());
        appeal.setLevel(1); // Always 1 since only one appeal allowed
        appeal.setAppealText(submitDTO.getAppealText());
        appeal.setStatus(ReturnAppeal.AppealStatus.PENDING);
        appeal.setSubmittedAt(LocalDateTime.now());
        
        ReturnAppeal savedAppeal = returnAppealRepository.save(appeal);
        
        // Process media attachments
        processAppealMediaAttachments(savedAppeal.getId(), submitDTO.getMediaFiles());
        
        
        // Send notifications
        notificationService.notifyAppealSubmitted(savedAppeal, returnRequest);
        
        log.info("Appeal {} submitted successfully for return request {}", 
                savedAppeal.getId(), submitDTO.getReturnRequestId());
        
        return convertToDTO(savedAppeal);
    }

    /**
     * Review and approve/deny an appeal
     */
    public ReturnAppealDTO reviewAppeal(AppealDecisionDTO decisionDTO) {
        log.info("Processing appeal decision for appeal {}: {}", 
                decisionDTO.getAppealId(), decisionDTO.getDecision());
        
        ReturnAppeal appeal = returnAppealRepository.findByIdWithReturnRequest(decisionDTO.getAppealId())
                .orElseThrow(() -> new ReturnException.ReturnNotFoundException(
                        "Appeal not found: " + decisionDTO.getAppealId()));
        
        if (appeal.getStatus() != ReturnAppeal.AppealStatus.PENDING) {
            throw new ReturnException.InvalidReturnStatusException(
                    "Appeal is not in pending status");
        }
        
        if ("APPROVED".equals(decisionDTO.getDecision())) {
            approveAppeal(appeal, decisionDTO);
        } else if ("DENIED".equals(decisionDTO.getDecision())) {
            denyAppeal(appeal, decisionDTO);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decisionDTO.getDecision());
        }
        
        ReturnAppeal updatedAppeal = returnAppealRepository.save(appeal);

        log.info("Appeal {} decision completed: {}", 
                updatedAppeal.getId(), decisionDTO.getDecision());
        
        return convertToDTO(updatedAppeal);
    }

    /**
     * Escalate appeal to higher level review
     */
    public ReturnAppealDTO escalateAppeal(Long appealId, String escalationReason, String escalatedBy) {
        log.info("Escalating appeal {} for higher level review", appealId);
        
        ReturnAppeal appeal = returnAppealRepository.findById(appealId)
                .orElseThrow(() -> new ReturnException.ReturnNotFoundException(
                        "Appeal not found: " + appealId));
        
        if (appeal.getStatus() != ReturnAppeal.AppealStatus.PENDING) {
            throw new ReturnException.InvalidReturnStatusException(
                    "Only pending appeals can be escalated");
        }
        log.info("Appeal {} escalated successfully", appealId);
        
        return convertToDTO(appeal);
    }

    /**
     * Get appeals by customer with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnAppealDTO> getAppealsByCustomer(String customerId, Pageable pageable) {
        Page<ReturnAppeal> appeals = returnAppealRepository.findByCustomerId(customerId, pageable);
        return appeals.map(this::convertToDTO);
    }

    /**
     * Get appeals by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReturnAppealDTO> getAppealsByStatus(ReturnAppeal.AppealStatus status, Pageable pageable) {
        Page<ReturnAppeal> appeals = returnAppealRepository.findByStatus(status, pageable);
        return appeals.map(this::convertToDTO);
    }

    /**
     * Get appeal by ID with all related data
     */
    @Transactional(readOnly = true)
    public ReturnAppealDTO getAppealById(Long id) {
        ReturnAppeal appeal = returnAppealRepository.findByIdWithAllData(id)
                .orElseThrow(() -> new ReturnException.ReturnNotFoundException(
                        "Appeal not found: " + id));
        return convertToDTO(appeal);
    }

    /**
     * Get appeals that need decision (pending for more than specified days)
     */
    @Transactional(readOnly = true)
    public List<ReturnAppealDTO> getAppealsNeedingDecision(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<ReturnAppeal> appeals = returnAppealRepository.findAppealsNeedingDecision(cutoffDate);
        return appeals.stream().map(this::convertToDTO).toList();
    }

    /**
     * Get appeal statistics for dashboard
     */
    @Transactional(readOnly = true)
    public AppealStatisticsDTO getAppealStatistics() {
        AppealStatisticsDTO stats = new AppealStatisticsDTO();
        
        stats.setPendingCount(returnAppealRepository.countByStatus(ReturnAppeal.AppealStatus.PENDING));
        stats.setApprovedCount(returnAppealRepository.countByStatus(ReturnAppeal.AppealStatus.APPROVED));
        stats.setDeniedCount(returnAppealRepository.countByStatus(ReturnAppeal.AppealStatus.DENIED));
        
        // Recent appeals (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ReturnAppeal> recentAppeals = returnAppealRepository.findRecentAppeals(thirtyDaysAgo);
        stats.setRecentCount(recentAppeals.size());
        
        // Appeals needing urgent attention (pending for more than 7 days)
        List<ReturnAppeal> urgentAppeals = returnAppealRepository.findAppealsNeedingDecision(
                LocalDateTime.now().minusDays(7));
        stats.setUrgentCount(urgentAppeals.size());
        
        return stats;
    }

    // Private helper methods

    private ReturnRequest validateReturnForAppeal(Long returnRequestId, String customerId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ReturnException.ReturnNotFoundException(
                        "Return request not found: " + returnRequestId));
        
        if (!customerId.equals(returnRequest.getCustomerId())) {
            throw new ReturnException.AppealNotAllowedException(
                    "Return request does not belong to customer");
        }
        
        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.DENIED) {
            throw new ReturnException.AppealNotAllowedException(
                    "Only denied return requests can be appealed");
        }
        
        if (!returnRequest.canBeAppealed()) {
            throw new ReturnException.AppealNotAllowedException(
                    "Return request cannot be appealed (appeal may already exist)");
        }
        
        return returnRequest;
    }

    private void processAppealMediaAttachments(Long appealId, List<SubmitAppealDTO.MediaUploadDTO> mediaFiles) {
        for (SubmitAppealDTO.MediaUploadDTO mediaFile : mediaFiles) {
            AppealMedia media = new AppealMedia();
            media.setAppealId(appealId);
            media.setFileUrl(mediaFile.getFileUrl());
            media.setFileType(AppealMedia.FileType.valueOf(mediaFile.getFileType()));
            media.setUploadedAt(LocalDateTime.now());
            
            appealMediaRepository.save(media);
        }
        
        log.info("Processed {} media attachments for appeal {}", 
                mediaFiles.size(), appealId);
    }

    private void approveAppeal(ReturnAppeal appeal, AppealDecisionDTO decisionDTO) {
        appeal.approve(decisionDTO.getDecisionNotes());
        
        // When appeal is approved, the original return request should also be approved
        ReturnRequest returnRequest = appeal.getReturnRequest();
        if (returnRequest != null) {
            returnRequest.approve("Appeal approved: " + decisionDTO.getDecisionNotes());
            returnRequestRepository.save(returnRequest);
            
            // Process refund if specified
            if (decisionDTO.getRefundDetails() != null) {
                // Integrate with refund service
                log.info("Processing refund for approved appeal {}", appeal.getId());
            }
        }
        
        // Send notifications
        notificationService.notifyAppealApproved(appeal, returnRequest);
        
        log.info("Appeal {} approved", appeal.getId());
    }

    private void denyAppeal(ReturnAppeal appeal, AppealDecisionDTO decisionDTO) {
        appeal.deny(decisionDTO.getDecisionNotes());
        
        // Appeal is final - no further appeals allowed
        // Send final notification to customer
        notificationService.notifyAppealDenied(appeal, appeal.getReturnRequest());
        
        log.info("Appeal {} denied - final decision", appeal.getId());
    }

    private ReturnAppealDTO convertToDTO(ReturnAppeal appeal) {
        ReturnAppealDTO dto = new ReturnAppealDTO();
        dto.setId(appeal.getId());
        dto.setReturnRequestId(appeal.getReturnRequestId());
        dto.setLevel(appeal.getLevel());
        dto.setAppealText(appeal.getAppealText());
        dto.setStatus(appeal.getStatus());
        dto.setSubmittedAt(appeal.getSubmittedAt());
        dto.setDecisionAt(appeal.getDecisionAt());
        dto.setDecisionNotes(appeal.getDecisionNotes());
        
        // Add media if loaded
        if (appeal.getAppealMedia() != null && !appeal.getAppealMedia().isEmpty()) {
            dto.setAppealMedia(appeal.getAppealMedia().stream()
                    .map(media -> convertAppealMediaToDTO(media))
                    .toList());
        }
        
        return dto;
    }

    /**
     * Convert AppealMedia entity to DTO
     */
    private AppealMediaDTO convertAppealMediaToDTO(AppealMedia media) {
        AppealMediaDTO dto = new AppealMediaDTO();
        dto.setId(media.getId());
        dto.setAppealId(media.getAppealId());
        dto.setFileUrl(media.getFileUrl());
        dto.setFileType(media.getFileType());
        dto.setUploadedAt(media.getUploadedAt());
        dto.setCreatedAt(media.getCreatedAt());
        dto.setUpdatedAt(media.getUpdatedAt());
        return dto;
    }
}

